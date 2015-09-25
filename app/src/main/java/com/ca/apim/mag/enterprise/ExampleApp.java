/*
 * Created by awitrisna on 2013-11-15.
 * Copyright (c) 2013 CA Technologies. All rights reserved.
 */

package com.ca.apim.mag.enterprise;

import android.content.Context;
import android.net.http.SslError;
import android.os.ResultReceiver;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ca.mas.core.app.App;

import org.json.JSONObject;

/**
 * Example to show how to override the default implementation of the WebViewClient.
 * The default WebViewClient implement 3 methods: {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)},
 * {@link android.webkit.WebViewClient#onReceivedError(android.webkit.WebView, int, String, String)}, and
 * {@link android.webkit.WebViewClient#onReceivedError(android.webkit.WebView, int, String, String)},
 */
public class ExampleApp extends App {

    @Override
    protected WebViewClient getWebViewClient(Context context, ResultReceiver errorHandler) {
        final WebViewClient webViewClient = super.getWebViewClient(context, errorHandler);
        return new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return webViewClient.shouldInterceptRequest(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                webViewClient.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                webViewClient.onReceivedSslError(view, handler, error);
            }
        };
    }

    public ExampleApp(JSONObject app) {
        super(app);
    }
}
