/*
 * Created by awitrisna on 2013-11-15.
 * Copyright (c) 2013 CA Technologies. All rights reserved.
 */

package com.ca.apim.mag.enterprise;

import android.app.Activity;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.view.GestureDetectorCompat;
import android.view.*;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ca.mas.core.app.App;
import com.ca.mas.core.service.MssoIntents;

public class EnterpriseBrowserApp extends Activity implements GestureDetector.OnGestureListener {

    private GestureDetectorCompat mDetector;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enterprisebrowserapp);
        mDetector = new GestureDetectorCompat(this, this);
        App app = (App) getIntent().getExtras().getSerializable(MssoIntents.EXTRA_APP);
        if (app != null) {
            WebView webView = (WebView) findViewById(R.id.webView);
            webView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mDetector.onTouchEvent(event);
                    return false;
                }
            });

            //Show progress Bar
            final ProgressBar progressBar;
            progressBar = (ProgressBar) findViewById(R.id.progressBar);

            webView.setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int progress) {
                    if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                    }

                    progressBar.setProgress(progress);
                    if (progress == 100) {
                        progressBar.setVisibility(ProgressBar.GONE);
                    }
                }
            });
            app.renderWebView(this, webView, new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(final int resultCode, final Bundle resultData) {
                    EnterpriseBrowserApp.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (resultData != null) {
                                String message = resultData.getString(MssoIntents.RESULT_ERROR_MESSAGE);
                                Toast.makeText(EnterpriseBrowserApp.this, message, Toast.LENGTH_LONG).show();
                                EnterpriseBrowserApp.this.onBackPressed();
                            }
                        }
                    });
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browser, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        WebView webView = ((WebView) findViewById(R.id.webView));
        switch (item.getItemId()) {
            case R.id.back:
                webView.goBack();
                return true;
            case R.id.forward:
                webView.goForward();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        getActionBar().show();
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        getActionBar().hide();
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        getActionBar().hide();
        return false;
    }
}