package com.example.finalapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends ComponentActivity {
    private FirebaseAuth auth;
    private FirebaseUser user;
    private WebView webView;
    private ActivityResultLauncher<Intent> loginLauncher;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout fullscreenContainer;
    private int originalOrientation;

    private static final String[] ALLOWED_URLS = {
            "https://streamboxweb.netlify.app/",
            // Add more URLs as needed
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        fullscreenContainer = findViewById(R.id.fullscreen_container);

        setupLoginLauncher();
        setupBackPressHandler();

        if (savedInstanceState == null) {
            checkInternetAndProceed();
        } else {
            restoreWebViewState(savedInstanceState);
        }
    }

    private void setupLoginLauncher() {
        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        user = auth.getCurrentUser();
                        if (user != null) {
                            setupWebView();
                        } else {
                            finish();
                        }
                    } else {
                        finish();
                    }
                });
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customView != null) {
                    hideCustomView();
                } else if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    MainActivity.super.getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void checkInternetAndProceed() {
        if (!isInternetConnected()) {
            startNoInternetActivity();
        } else if (user == null) {
            startLoginActivity();
        } else {
            setupWebView();
        }
    }

    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            @SuppressWarnings("deprecation")
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    private void startNoInternetActivity() {
        Intent intent = new Intent(this, NoInternetActivity.class);
        startActivity(intent);
        finish();
    }

    private void startLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginPage.class);
        loginLauncher.launch(loginIntent);
    }

    private void setupWebView() {
        webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WhitelistWebViewClient(ALLOWED_URLS));
        webView.setWebChromeClient(new CustomWebChromeClient());

        if (webView.getUrl() == null) {
            webView.loadUrl(ALLOWED_URLS[0]);
        }
    }

    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            originalOrientation = getRequestedOrientation();
            customViewCallback = callback;
            fullscreenContainer.addView(customView);
            fullscreenContainer.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            setFullscreen(true);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }

    private void hideCustomView() {
        if (customView == null) return;

        setFullscreen(false);
        fullscreenContainer.removeView(customView);
        fullscreenContainer.setVisibility(View.GONE);
        customView = null;
        customViewCallback.onCustomViewHidden();
        webView.setVisibility(View.VISIBLE);
        setRequestedOrientation(originalOrientation);
    }

    private void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void restoreWebViewState(Bundle savedInstanceState) {
        webView = findViewById(R.id.webview);
        webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Do nothing here to prevent activity restart
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInternetConnected()) {
            startNoInternetActivity();
        }
    }

    private static class WhitelistWebViewClient extends WebViewClient {
        private final String[] allowedUrls;

        public WhitelistWebViewClient(String[] allowedUrls) {
            this.allowedUrls = allowedUrls;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            for (String allowedUrl : allowedUrls) {
                if (url.startsWith(allowedUrl)) {
                    return false; // Allow the WebView to load the URL
                }
            }
            return true; // Block the URL
        }
    }
}