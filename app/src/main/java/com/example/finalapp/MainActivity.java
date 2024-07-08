package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends ComponentActivity {
    private FirebaseAuth auth;
    private FirebaseUser user;
    private WebView webView;
    private ActivityResultLauncher<Intent> loginLauncher;

    // Define your allowed URLs here
    private static final String[] ALLOWED_URLS = {
            "https://streamboxweb.netlify.app/",
             // Add ore URLs as needed
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance();

        // Check if user is already signed in
        user = auth.getCurrentUser();
        if (user == null) {
            // If not signed in, launch login activity
            startLoginActivity();
        } else {
            // User is already signed in, proceed with WebView setup
            setupWebView();
        }
    }

    private void setupWebView() {
        webView = findViewById(R.id.webview);

        // Enable JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Other WebView settings for performance and security
        webSettings.setDomStorageEnabled(true); // Enable DOM Storage

        // Set WebViewClient to handle page navigation within WebView
        webView.setWebViewClient(new WhitelistWebViewClient(ALLOWED_URLS));

        // Load the initial URL in the WebView
        webView.loadUrl(ALLOWED_URLS[0]); // Load the first allowed URL
    }

    private void startLoginActivity() {
        // Initialize ActivityResultLauncher for login activity
        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Handle successful login
                        user = auth.getCurrentUser();
                        if (user != null) {
                            // Reload WebView after successful login
                            setupWebView();
                        } else {
                            // Handle unexpected case where user is null after login
                            finish();
                        }
                    } else {
                        // Handle unsuccessful login or cancellation
                        finish();
                    }
                });

        // Launch the login activity
        Intent loginIntent = new Intent(MainActivity.this, LoginPage.class);
        loginLauncher.launch(loginIntent);
    }

    @Override
    public void onBackPressed() {
        // Check if WebView can go back
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Custom WebViewClient for whitelisting URLs
    private static class WhitelistWebViewClient extends WebViewClient {
        private String[] allowedUrls;

        public WhitelistWebViewClient(String[] allowedUrls) {
            this.allowedUrls = allowedUrls;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Check if the URL is in the allowed list
            for (String allowedUrl : allowedUrls) {
                if (url.startsWith(allowedUrl)) {
                    return false; // Allow the WebView to load the URL
                }
            }
            // Block the URL
            return true;
        }
    }
}
