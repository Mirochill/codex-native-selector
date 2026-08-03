package com.mirochill.codexquota;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted, local-only storage for the short-lived ChatGPT/Codex OAuth tokens. */
public final class ChatGptAuthStore {
    private static final String PREFS = "codex_chatgpt_auth";
    private static final String KEY_ALIAS = "codex_quota_chatgpt_auth_v1";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private static final String ID = "id";

    private ChatGptAuthStore() {}

    public static final class Tokens {
        public final String accessToken;
        public final String refreshToken;
        public final String idToken;
        public final String accountId;

        public Tokens(String accessToken, String refreshToken, String idToken, String accountId) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.idToken = idToken;
            this.accountId = accountId;
        }
    }

    public static boolean hasTokens(Context context) {
        return load(context) != null;
    }

    public static void save(Context context, Tokens tokens) throws Exception {
        if (tokens == null || isBlank(tokens.accessToken) || isBlank(tokens.refreshToken)) {
            throw new IllegalArgumentException("OAuth tokens are incomplete");
        }
        SharedPreferences.Editor editor = prefs(context).edit();
        putEncrypted(editor, ACCESS, tokens.accessToken);
        putEncrypted(editor, REFRESH, tokens.refreshToken);
        putEncrypted(editor, ID, tokens.idToken == null ? "" : tokens.idToken);
        editor.apply();
    }

    public static Tokens load(Context context) {
        try {
            SharedPreferences prefs = prefs(context);
            String access = getEncrypted(prefs, ACCESS);
            String refresh = getEncrypted(prefs, REFRESH);
            if (isBlank(access) || isBlank(refresh)) return null;
            String id = getEncrypted(prefs, ID);
            return new Tokens(access, refresh, id, accountIdFromIdToken(id));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS);
        } catch (Exception ignored) {
            // Clearing the preferences is sufficient if the keystore is unavailable.
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void putEncrypted(SharedPreferences.Editor editor, String name, String value)
            throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        editor.putString(name, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(encrypted, Base64.NO_WRAP));
    }

    private static String getEncrypted(SharedPreferences prefs, String name) throws Exception {
        String stored = prefs.getString(name, null);
        if (isBlank(stored)) return "";
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return "";
        byte[] iv = Base64.decode(parts[0], Base64.DEFAULT);
        byte[] encrypted = Base64.decode(parts[1], Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static SecretKey key() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private static String accountIdFromIdToken(String idToken) {
        if (isBlank(idToken)) return "";
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) return "";
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP), StandardCharsets.UTF_8);
            org.json.JSONObject claims = new org.json.JSONObject(payload);
            org.json.JSONObject auth = claims.optJSONObject("https://api.openai.com/auth");
            return auth == null ? "" : auth.optString("chatgpt_account_id", "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
