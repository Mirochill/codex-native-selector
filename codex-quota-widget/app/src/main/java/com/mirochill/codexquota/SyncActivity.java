package com.mirochill.codexquota;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Connects through the official ChatGPT/Codex device flow, without an embedded WebView. */
public class SyncActivity extends Activity {
    private TextView status;
    private TextView code;
    private Button openAuth;
    private Button refresh;
    private ExecutorService executor;
    private volatile ChatGptAuthClient.DeviceCode pendingDeviceCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        status = findViewById(R.id.sync_status);
        code = findViewById(R.id.sync_code);
        openAuth = findViewById(R.id.sync_open_auth);
        refresh = findViewById(R.id.sync_reload);
        executor = Executors.newSingleThreadExecutor();

        refresh.setOnClickListener(v -> startSync());
        openAuth.setOnClickListener(v -> openVerificationPage());
        startSync();
    }

    private void startSync() {
        if (executor == null || executor.isShutdown()) return;
        refresh.setEnabled(false);
        openAuth.setVisibility(View.GONE);
        code.setVisibility(View.GONE);
        status.setText(ChatGptAuthStore.hasTokens(this)
                ? "Connexion ChatGPT / Codex…"
                : "Préparation de la connexion ChatGPT…");

        executor.submit(() -> {
            try {
                if (!ChatGptAuthStore.hasTokens(this)) {
                    ChatGptAuthClient.DeviceCode deviceCode = ChatGptAuthClient.requestDeviceCode();
                    pendingDeviceCode = deviceCode;
                    runOnUiThread(() -> showDeviceCode(deviceCode));
                    // Open the system browser, never a WebView, for the actual ChatGPT login.
                    runOnUiThread(this::openVerificationPage);
                    ChatGptAuthStore.Tokens tokens = ChatGptAuthClient.completeDeviceCode(deviceCode);
                    ChatGptAuthStore.save(this, tokens);
                }
                runOnUiThread(() -> status.setText("Récupération des limites Codex…"));
                QuotaSnapshot snapshot = ChatGptAuthClient.sync(this);
                QuotaStore.save(this, snapshot);
                CodexWidgetProvider.updateAll(this);
                runOnUiThread(() -> {
                    status.setText("Synchronisé à " + MainActivity.time(snapshot.updatedAt)
                            + ". Le widget est à jour.");
                    code.setVisibility(View.GONE);
                    openAuth.setVisibility(View.GONE);
                    refresh.setEnabled(true);
                });
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception error) {
                runOnUiThread(() -> {
                    status.setText(messageFor(error));
                    refresh.setEnabled(true);
                });
            }
        });
    }

    private void showDeviceCode(ChatGptAuthClient.DeviceCode deviceCode) {
        code.setText(deviceCode.userCode);
        code.setVisibility(View.VISIBLE);
        openAuth.setVisibility(View.VISIBLE);
        status.setText("Ouvre la page ChatGPT, saisis ce code, puis reviens ici.");
    }

    private void openVerificationPage() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ChatGptAuthClient.VERIFICATION_URL)));
        } catch (Exception error) {
            status.setText("Impossible d’ouvrir le navigateur. Va sur auth.openai.com/codex/device.");
        }
    }

    private static String messageFor(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return "Connexion impossible. Réessaie.";
        if (message.length() > 180) message = message.substring(0, 180) + "…";
        return message;
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }
}
