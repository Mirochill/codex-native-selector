package com.mirochill.codexquota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    public static final String ACTION_WIDGET_SYNC =
            "com.mirochill.codexquota.action.WIDGET_SYNC";

    private TextView status;
    private Spinner interval;
    private Button syncButton;
    private ExecutorService executor;
    private boolean syncing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.main_status);
        interval = findViewById(R.id.main_interval);
        syncButton = findViewById(R.id.main_sync);
        executor = Executors.newSingleThreadExecutor();

        syncButton.setOnClickListener(v -> handleSyncRequest());
        findViewById(R.id.main_manual).setOnClickListener(v -> showManualDialog());
        setupIntervalSelector();
        AutoSyncScheduler.ensureScheduled(this);
        render();

        if (ACTION_WIDGET_SYNC.equals(getIntent().getAction())) {
            syncNow();
        } else {
            AutoSyncScheduler.requestSyncIfDue(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && ACTION_WIDGET_SYNC.equals(intent.getAction())) syncNow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null && !syncing) {
            AutoSyncScheduler.requestSyncIfDue(this);
            render();
        }
    }

    private void handleSyncRequest() {
        if (ChatGptAuthStore.hasTokens(this)) {
            syncNow();
        } else {
            startActivity(new Intent(this, SyncActivity.class));
        }
    }

    private void syncNow() {
        if (syncing) return;
        if (!ChatGptAuthStore.hasTokens(this)) {
            startActivity(new Intent(this, SyncActivity.class));
            return;
        }

        syncing = true;
        syncButton.setEnabled(false);
        syncButton.setText("SYNCHRONISATION…");
        status.setText("Connexion à Codex et récupération des quotas en cours…");
        status.setTextColor(Color.WHITE);
        CodexWidgetProvider.showSyncing(this);

        executor.submit(() -> {
            try {
                QuotaSnapshot snapshot = ChatGptAuthClient.sync(this);
                QuotaStore.save(this, snapshot);
                AutoSyncScheduler.reschedule(this);
                CodexWidgetProvider.updateAll(this);
                runOnUiThread(() -> {
                    syncing = false;
                    render();
                    Toast.makeText(this, "Quotas synchronisés à " + time(snapshot.updatedAt),
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    syncing = false;
                    render();
                    Toast.makeText(this, safeMessage(error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void render() {
        QuotaSnapshot s = QuotaStore.get(this);
        boolean connected = ChatGptAuthStore.hasTokens(this);
        String updated = s.updatedAt == 0L ? "Jamais synchronisé."
                : "Dernière synchronisation : " + time(s.updatedAt);
        String connection = connected
                ? "ChatGPT / Codex connecté"
                : "ChatGPT / Codex non connecté";
        String resetBank = s.resetCredits < 0 ? "—" : Integer.toString(s.resetCredits);
        String quotas = CodexWidgetProvider.displayWindow(s.primaryLabel) + " : " + s.primaryValue;
        if (s.hasSecondaryWindow()) {
            quotas += "   •   " + CodexWidgetProvider.displayWindow(s.secondaryLabel)
                    + " : " + s.secondaryValue;
        }
        status.setText(connection
                + "\n\n" + quotas
                + "\nTokens aujourd’hui : " + QuotaSnapshot.compactTokens(s.dailyTokens)
                + "\nTokens total : " + QuotaSnapshot.compactTokens(s.lifetimeTokens)
                + "\nAbonnement : " + s.planType + "   •   Resets : " + resetBank
                + "\n" + updated
                + "\nActualisation : " + SyncPreferences.currentLabel(this));
        status.setTextColor(Color.WHITE);
        syncButton.setEnabled(!syncing);
        syncButton.setText(syncing ? "SYNCHRONISATION…"
                : connected ? "SYNCHRONISER MAINTENANT" : "CONNECTER CHATGPT / CODEX");
    }

    private void setupIntervalSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SyncPreferences.LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        interval.setAdapter(adapter);
        interval.setSelection(SyncPreferences.selectedIndex(this), false);
        interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == SyncPreferences.selectedIndex(MainActivity.this)) return;
                SyncPreferences.select(MainActivity.this, position);
                AutoSyncScheduler.reschedule(MainActivity.this);
                AutoSyncScheduler.requestSyncIfDue(MainActivity.this);
                render();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showManualDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 8, 36, 0);

        EditText primary = field("Quota 5 h restant", QuotaStore.get(this).primaryValue);
        EditText secondary = field("Quota semaine restant", QuotaStore.get(this).secondaryValue);
        layout.addView(primary);
        layout.addView(secondary);

        new AlertDialog.Builder(this)
                .setTitle("Entrer les quotas")
                .setView(layout)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    QuotaStore.save(this, QuotaSnapshot.manual(
                            primary.getText().toString(), secondary.getText().toString()));
                    CodexWidgetProvider.updateAll(this);
                    render();
                })
                .show();
    }

    private EditText field(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText("—".equals(value) ? "" : value);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return "Synchronisation impossible.";
        return message.length() > 160 ? message.substring(0, 160) + "…" : message;
    }

    public static String time(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date(timestamp));
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }
}
