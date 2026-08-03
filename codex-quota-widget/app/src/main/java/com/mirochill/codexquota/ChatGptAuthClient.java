package com.mirochill.codexquota;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Small client for the same ChatGPT auth/rate-limit flow used by Codex. */
public final class ChatGptAuthClient {
    public static final String VERIFICATION_URL = "https://auth.openai.com/codex/device";
    private static final String AUTH_BASE = "https://auth.openai.com";
    private static final String API_ACCOUNTS = AUTH_BASE + "/api/accounts";
    private static final String CHATGPT_USAGE = "https://chatgpt.com/backend-api/wham/usage";
    private static final String CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 25_000;

    private ChatGptAuthClient() {}

    public static final class DeviceCode {
        public final String userCode;
        public final String deviceAuthId;
        public final int intervalSeconds;

        private DeviceCode(String userCode, String deviceAuthId, int intervalSeconds) {
            this.userCode = userCode;
            this.deviceAuthId = deviceAuthId;
            this.intervalSeconds = intervalSeconds;
        }
    }

    public static final class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }

    private static final class HttpFailure extends Exception {
        final int status;

        HttpFailure(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    public static DeviceCode requestDeviceCode() throws Exception {
        JSONObject body = new JSONObject().put("client_id", CLIENT_ID);
        JSONObject response = requestJson("POST", API_ACCOUNTS + "/deviceauth/usercode",
                body.toString(), "application/json", null);
        String userCode = response.optString("user_code", response.optString("usercode", ""));
        String deviceAuthId = response.optString("device_auth_id", "");
        int interval = response.optInt("interval", 5);
        if (interval < 2) interval = 5;
        if (userCode.isEmpty() || deviceAuthId.isEmpty()) {
            throw new AuthException("Le serveur d’auth n’a pas renvoyé de code appareil.");
        }
        return new DeviceCode(userCode, deviceAuthId, interval);
    }

    public static ChatGptAuthStore.Tokens completeDeviceCode(DeviceCode deviceCode) throws Exception {
        long deadline = System.currentTimeMillis() + 15L * 60L * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("auth cancelled");
            try {
                JSONObject response = requestJson("POST", API_ACCOUNTS + "/deviceauth/token",
                        new JSONObject()
                                .put("device_auth_id", deviceCode.deviceAuthId)
                                .put("user_code", deviceCode.userCode)
                                .toString(), "application/json", null);
                String authorizationCode = response.optString("authorization_code", "");
                if (!authorizationCode.isEmpty()) {
                    String idToken = exchangeCode(authorizationCode,
                            response.optString("code_challenge", ""),
                            response.optString("code_verifier", ""));
                    JSONObject tokens = new JSONObject(idToken);
                    return new ChatGptAuthStore.Tokens(
                            tokens.getString("access_token"),
                            tokens.getString("refresh_token"),
                            tokens.optString("id_token", ""), "");
                }
            } catch (HttpFailure pending) {
                if (pending.status != 403 && pending.status != 404) throw pending;
            }
            Thread.sleep(deviceCode.intervalSeconds * 1000L);
        }
        throw new AuthException("Le code appareil a expiré. Relance la connexion.");
    }

    /** Returns the latest quota, refreshing the OAuth access token once after a 401. */
    public static QuotaSnapshot sync(Context context) throws Exception {
        ChatGptAuthStore.Tokens tokens = ChatGptAuthStore.load(context);
        if (tokens == null) throw new AuthException("Connexion ChatGPT requise.");
        try {
            return fetchQuota(tokens);
        } catch (HttpFailure failure) {
            if (failure.status != 401 || tokens.refreshToken == null || tokens.refreshToken.isEmpty()) {
                throw failure;
            }
            ChatGptAuthStore.Tokens refreshed = refresh(tokens);
            ChatGptAuthStore.save(context, refreshed);
            return fetchQuota(refreshed);
        }
    }

    private static QuotaSnapshot fetchQuota(ChatGptAuthStore.Tokens tokens) throws Exception {
        JSONObject response = requestJson("GET", CHATGPT_USAGE, null, null, tokens);
        QuotaSnapshot snapshot = QuotaSnapshot.fromUsageJson(response.toString());
        if (!snapshot.hasAnyValue()) throw new AuthException("Quota Codex introuvable dans la réponse.");
        return snapshot;
    }

    private static ChatGptAuthStore.Tokens refresh(ChatGptAuthStore.Tokens old) throws Exception {
        String form = "grant_type=refresh_token"
                + "&client_id=" + Uri.encode(CLIENT_ID)
                + "&refresh_token=" + Uri.encode(old.refreshToken);
        JSONObject response = requestJson("POST", AUTH_BASE + "/oauth/token", form,
                "application/x-www-form-urlencoded", null);
        return new ChatGptAuthStore.Tokens(
                response.optString("access_token", old.accessToken),
                response.optString("refresh_token", old.refreshToken),
                response.optString("id_token", old.idToken), "");
    }

    private static String exchangeCode(String authorizationCode, String challenge, String verifier)
            throws Exception {
        if (challenge.isEmpty() || verifier.isEmpty()) {
            throw new AuthException("Réponse d’authentification incomplète.");
        }
        String form = "grant_type=authorization_code"
                + "&code=" + Uri.encode(authorizationCode)
                + "&redirect_uri=" + Uri.encode(AUTH_BASE + "/deviceauth/callback")
                + "&client_id=" + Uri.encode(CLIENT_ID)
                + "&code_verifier=" + Uri.encode(verifier);
        JSONObject response = requestJson("POST", AUTH_BASE + "/oauth/token", form,
                "application/x-www-form-urlencoded", null);
        return response.toString();
    }

    private static JSONObject requestJson(String method, String endpoint, String body,
                                          String contentType, ChatGptAuthStore.Tokens tokens)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "codex-quota-widget/1.2");
        if (tokens != null) {
            connection.setRequestProperty("Authorization", "Bearer " + tokens.accessToken);
            if (tokens.accountId != null && !tokens.accountId.isEmpty()) {
                connection.setRequestProperty("ChatGPT-Account-Id", tokens.accountId);
            }
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", contentType);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String responseBody = read(stream);
        connection.disconnect();
        if (status < 200 || status >= 300) throw new HttpFailure(status,
                "Réponse serveur " + status + (responseBody.isEmpty() ? "" : " : " + responseBody));
        try {
            return new JSONObject(responseBody);
        } catch (Exception parseError) {
            throw new AuthException("Réponse JSON inattendue du serveur Codex.");
        }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }
}
