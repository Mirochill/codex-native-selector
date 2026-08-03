package com.mirochill.codexquota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView status;
    private Spinner interval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.main_status);
        interval = findViewById(R.id.main_interval);

        findViewById(R.id.main_sync).setOnClickListener(v ->
                startActivity(new Intent(this, SyncActivity.class)));
        findViewById(R.id.main_manual).setOnClickListener(v -> showManualDialog());
        setupIntervalSelector();
        AutoSyncScheduler.ensureScheduled(this);
        AutoSyncScheduler.requestSyncIfDue(this);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) {
            AutoSyncScheduler.requestSyncIfDue(this);
            render();
        }
    }

    private void render() {
        QuotaSnapshot s = QuotaStore.get(this);
        String updated = s.updatedAt == 0L ? "Jamais synchronisé." : "Dernière synchronisation : " + time(s.updatedAt);
        String connection = ChatGptAuthStore.hasTokens(this)
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
                    QuotaStore.save(this, QuotaSnapshot.manual(primary.getText().toString(), secondary.getText().toString()));
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

    public static String time(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date(timestamp));
    }
}
