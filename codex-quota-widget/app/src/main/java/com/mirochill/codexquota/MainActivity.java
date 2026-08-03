package com.mirochill.codexquota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.main_status);

        findViewById(R.id.main_sync).setOnClickListener(v ->
                startActivity(new Intent(this, SyncActivity.class)));
        findViewById(R.id.main_dashboard).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SyncActivity.USAGE_URL))));
        findViewById(R.id.main_manual).setOnClickListener(v -> showManualDialog());
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) render();
    }

    private void render() {
        QuotaSnapshot s = QuotaStore.get(this);
        String updated = s.updatedAt == 0L ? "Jamais synchronisé." : "Dernière synchronisation : " + time(s.updatedAt);
        status.setText("Quota 5 h : " + s.primaryValue + "\nQuota semaine : " + s.secondaryValue + "\n" + updated);
        status.setTextColor(Color.WHITE);
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
