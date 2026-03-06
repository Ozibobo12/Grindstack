package com.example.myapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.*;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private int mPageCount = 0;

    public static final String START_URL = "https://grindstak.free.nf";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar  = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        webView      = findViewById(R.id.webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setAllowFileAccess(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        AdManager.init(this);
        setupAds();
        
        // Rate app prompt after 3 days
        android.content.SharedPreferences prefs = getSharedPreferences("wab_prefs", MODE_PRIVATE);
        long firstLaunch = prefs.getLong("first_launch", 0);
        if (firstLaunch == 0) {
            prefs.edit().putLong("first_launch", System.currentTimeMillis()).apply();
        } else if (System.currentTimeMillis() - firstLaunch > 259200000L && !prefs.getBoolean("rated", false)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Enjoying the app?")
                .setMessage("Would you like to rate us on the Play Store?")
                .setPositiveButton("Rate Now", (d, w) -> {
                    prefs.edit().putBoolean("rated", true).apply();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                })
                .setNegativeButton("Not Now", null)
                .show();
        }

        webView.setWebViewClient(new AppWebViewClient(this));
        webView.setWebChromeClient(new AppWebChromeClient());

        swipeRefresh.setColorSchemeColors(Color.parseColor(getString(R.string.primary_color)));
        swipeRefresh.setOnRefreshListener(() -> webView.reload());

        webView.loadUrl(START_URL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
            startActivity(Intent.createChooser(intent, "Share via"));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    
    private void setupAds() {
        // Banner Ad
        mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        RelativeLayout layout = findViewById(R.id.adContainer);
        layout.addView(mAdView);
        mAdView.loadAd(new AdRequest.Builder().build());

        // Interstitial
        new InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) { mInterstitialAd = ad; }
                @Override
                public void onAdFailedToLoad(LoadAdError e) { mInterstitialAd = null; }
            });
    }

    private void maybeShowInterstitial() {
        mPageCount++;
        if (mPageCount % 3 == 0 && mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd = null;
        }
    }

    // ── WebViewClient ──────────────────────────────
    private class AppWebViewClient extends WebViewClient {
        private final Activity activity;
        AppWebViewClient(Activity a) { activity = a; }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            String url = req.getUrl().toString();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                view.loadUrl(url);
            } else {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {}
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame()) {
                view.loadUrl("about:blank");
                view.evaluateJavascript(getOfflinePage(), null);
            }
        }
    }

    // ── WebChromeClient ────────────────────────────
    private class AppWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int progress) {
            progressBar.setProgress(progress);
            progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
        }
    }

    private String getOfflinePage() {
        return "document.body.innerHTML = '<div style=\"font-family:sans-serif;text-align:center;padding:60px 24px;\">" +
               "<h2 style=\"color:#6366f1\">No Connection</h2>" +
               "<p style=\"color:#64748b\">Check your internet and try again.</p>" +
               "<button onclick=\"location.reload()\" style=\"margin-top:20px;padding:12px 28px;background:#6366f1;color:white;border:none;border-radius:8px;font-size:15px;cursor:pointer\">Retry</button>" +
               "</div>';";
    }
}