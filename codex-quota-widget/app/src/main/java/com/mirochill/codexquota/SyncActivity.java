package com.mirochill.codexquota;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONTokener;

public class SyncActivity extends Activity {
    public static final String USAGE_URL = "https://chatgpt.com/codex/settings/usage";
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final int MAX_POLLS = 15;

    private WebView webView;
    private TextView status;
    private int polls;

    private final Runnable inspectRunnable = this::inspectPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        status = findViewById(R.id.sync_status);
        webView = findViewById(R.id.sync_webview);
        Button reload = findViewById(R.id.sync_reload);
        reload.setOnClickListener(v -> loadUsagePage());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return route(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return route(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                status.setText("Connexion ChatGPT / Codex…");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                status.setText("Récupération des limites Codex…");
                polls = 0;
                HANDLER.removeCallbacks(inspectRunnable);
                HANDLER.postDelayed(inspectRunnable, 1400L);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) status.setText("Impossible de charger Codex. Vérifie la connexion.");
            }
        });
        loadUsagePage();
    }

    private boolean route(Uri uri) {
        String host = uri == null ? "" : uri.getHost();
        boolean allowed = host != null && (host.equals("chatgpt.com") || host.endsWith(".chatgpt.com")
                || host.equals("openai.com") || host.endsWith(".openai.com")
                || host.equals("accounts.google.com") || host.equals("appleid.apple.com")
                || host.equals("login.microsoftonline.com"));
        if (!allowed && uri != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
        return !allowed;
    }

    private void loadUsagePage() {
        polls = 0;
        HANDLER.removeCallbacks(inspectRunnable);
        webView.loadUrl(USAGE_URL);
    }

    private void inspectPage() {
        if (webView == null) return;
        webView.evaluateJavascript(
                "(function(){return document.body ? document.body.innerText : '';})()",
                result -> {
                    String text = decode(result);
                    QuotaSnapshot snapshot = QuotaSnapshot.fromPageText(text);
                    if (snapshot.hasAnyValue()) {
                        QuotaStore.save(this, snapshot);
                        CodexWidgetProvider.updateAll(this);
                        status.setText("Synchronisé à " + MainActivity.time(snapshot.updatedAt)
                                + ". Tu peux fermer cet écran.");
                    } else if (++polls < MAX_POLLS) {
                        status.setText("Attente des données du tableau d’usage…");
                        HANDLER.postDelayed(inspectRunnable, 1200L);
                    } else {
                        status.setText("Aucun quota détecté. Vérifie que le tableau d’usage est ouvert, puis actualise.");
                    }
                });
    }

    private static String decode(String javascriptResult) {
        if (javascriptResult == null || "null".equals(javascriptResult)) return "";
        try {
            Object value = new JSONTokener(javascriptResult).nextValue();
            return value == null ? "" : value.toString();
        } catch (Exception ignored) {
            return javascriptResult;
        }
    }

    @Override
    protected void onDestroy() {
        HANDLER.removeCallbacks(inspectRunnable);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
