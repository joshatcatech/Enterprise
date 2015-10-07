/*
 * Created by awitrisna on 2013-11-15.
 * Copyright (c) 2013 CA Technologies. All rights reserved.
 */

package com.ca.apim.mag.enterprise;

import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ca.mas.core.MobileSso;
import com.ca.mas.core.request.ClientCredentialsRequest;
import com.ca.mas.core.context.MssoContext;
import com.ca.mas.core.gui.HttpResponseFragment;
import com.ca.mas.core.request.PasswordGrantRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.net.URI;

/**
 * Retained fragment that downloads and/or caches a downloaded resource in JSON format.
 * <p/>
 * Activities that wish to make use of this fragment must implement {@link UserActivity}.
 */
public class JsonDownloaderFragment extends HttpResponseFragment {
    public static final int MAX_JSON_SIZE = 100 * 1024;

    private String lastDownloadedJson;

    private UserActivity userActivity() {
        return (UserActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (lastDownloadedJson != null)
            sendJsonToActivity(lastDownloadedJson);
    }

    void downloadJson() {
        // Can only be called while there is an activity associated
        ExampleActivity activity = (ExampleActivity) getActivity();
        activity.setProgressVisibility(ProgressBar.VISIBLE);

        final URI uri = activity.getJsonDownloadUri();
        HttpGet httpGet = new HttpGet(uri);

        processRequest(httpGet, activity.getUserLoggedIn(), activity.mobileSso());
    }

    void processRequest(HttpGet httpGet, boolean userLoggedIn, MobileSso msso) {
        if (userLoggedIn) {
            msso.processRequest(new PasswordGrantRequest(httpGet){
                @Override
                public String getScope(MssoContext context) {
                    return "openid msso phone profile address email msso_client_register member_scope products";
                }
            }, getResultReceiver());
        } else {
            msso.processRequest(new ClientCredentialsRequest(httpGet){
                @Override
                public String getScope(MssoContext context) {
                    return "openid msso phone profile address email msso_client_register products";
                }
            }, getResultReceiver());
        }
    }

    void sendJsonToActivity(String json) {
        UserActivity activity = userActivity();
        if (activity != null) {
            activity.setDownloadedJson(json);
        }
    }

    @Override
    protected void onResponse(
            long requestId, int resultCode, String errorMessage, HttpResponse httpResponse) {
        // Might be invoked while we have no associated activity
        UserActivity activity = userActivity();
        if (activity != null)
            activity.setProgressVisibility(ProgressBar.GONE);

        if (errorMessage != null) {
            if (activity != null)
                activity.showMessage(errorMessage, Toast.LENGTH_LONG);
        } else {
            lastDownloadedJson = toString(httpResponse, MAX_JSON_SIZE, false);
            sendJsonToActivity(lastDownloadedJson);
        }
    }

    /**
     * Interface that must be implemented by an Activity that wishes to use this fragment.
     */
    public interface UserActivity {
        Context getBaseContext();

        MobileSso mobileSso();

        void setProgressVisibility(int visibility);

        void setDownloadedJson(String json);

        void showMessage(String errorMessage, final int toastLength);

        URI getJsonDownloadUri();
    }
}
