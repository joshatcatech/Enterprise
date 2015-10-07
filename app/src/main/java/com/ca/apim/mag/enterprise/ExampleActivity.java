/*
 * Created by awitrisna on 2013-11-15.
 * Copyright (c) 2013 CA Technologies. All rights reserved.
 */

package com.ca.apim.mag.enterprise;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.ca.mas.core.MobileSso;
import com.ca.mas.core.MobileSsoFactory;

public class ExampleActivity extends FragmentActivity
        implements JsonDownloaderFragment.UserActivity {

    private static final String TAG = "EnterpriseApp";
    private static final String STATE_PROGRESS_VISIBILITY = "exampleActivity.progressVisibility";

    private static final int MENU_GROUP_LOGOUT = 66;
    private static final int MENU_ITEM_LOG_OUT = 3;
    private static final int MENU_ITEM_LOG_OUT_CLIENT_ONLY = 1;
    private static final int MENU_ITEM_REMOVE_DEVICE_REGISTRATION = 4;
    private static final int MENU_ITEM_DESTROY_TOKEN_STORE = 2;

    private static final String TEXT1 = "text1";
    private static final String TEXT2 = "text2";

    ListView itemList;
    ProgressBar progressBar;
    static boolean usedMobileSso = false;
    static boolean isUserLoggedIn = false;

    //Application endpoint
    private URI productListDownloadUri = null;

    @Override
    public MobileSso mobileSso() {
        //Initialize the MobileSso with the configuration defined under /assets/msso_config.json
        MobileSso mobileSso = MobileSsoFactory.getInstance(this);
        usedMobileSso = true;
        return mobileSso;
    }

    public boolean getUserLoggedIn() {
        return isUserLoggedIn;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        itemList = (ListView) findViewById(R.id.itemList);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (savedInstanceState != null) {
            //noinspection ResourceType
            progressBar.setVisibility(savedInstanceState.getInt(STATE_PROGRESS_VISIBILITY));
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        JsonDownloaderFragment httpFragment =
                (JsonDownloaderFragment) fragmentManager.findFragmentByTag("httpResponseFragment");
        if (httpFragment == null) {
            httpFragment = new JsonDownloaderFragment();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.add(httpFragment, "httpResponseFragment");
            ft.commit();
        }

        final Button listButton = (Button) findViewById(R.id.listItemsButton);
        final JsonDownloaderFragment finalHttpFragment = httpFragment;

        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initAppEndpoint();
                finalHttpFragment.downloadJson();
            }
        });

        // Initialize with login button and conditionally show login or logout button thereafter
        // based on login status.
        final Button logOutButton = (Button) findViewById(R.id.logOutButton);
        final Button loginButton = (Button) findViewById(R.id.loginButton);
        final ListView productList = (ListView) findViewById(R.id.itemList);

        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOutButton.setVisibility(View.GONE);
                loginButton.setVisibility(View.VISIBLE);
                isUserLoggedIn = false;
                doServerLogout();

                productList.setAdapter(null);
            }
        });
        registerForContextMenu(logOutButton);

        loginButton.setVisibility(View.VISIBLE);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // login
                loginButton.setVisibility(View.GONE);
                logOutButton.setVisibility(View.VISIBLE);
                isUserLoggedIn = true;
                initAppEndpoint();

                finalHttpFragment.downloadJson();
            }
        });
    }

    @Override
    public void onCreateContextMenu(
            ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(MENU_GROUP_LOGOUT,
                MENU_ITEM_LOG_OUT, Menu.NONE, "Log Out");

        menu.add(MENU_GROUP_LOGOUT,
                MENU_ITEM_LOG_OUT_CLIENT_ONLY, Menu.NONE, "Log Out (client only)");

        menu.add(MENU_GROUP_LOGOUT,
                MENU_ITEM_REMOVE_DEVICE_REGISTRATION, Menu.NONE, "Unregister Device");

        menu.add(MENU_GROUP_LOGOUT,
                MENU_ITEM_DESTROY_TOKEN_STORE, Menu.NONE, "Destroy Token Store");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (66 != item.getGroupId())
            return false;
        switch (item.getItemId()) {
            case 1:
                mobileSso().logout(false);
                showMessage("Logged Out (client only)", Toast.LENGTH_SHORT);
                isUserLoggedIn = false;
                return true;
            case 2:
                mobileSso().destroyAllPersistentTokens();
                showMessage("Device Registration Destroyed (client only)", Toast.LENGTH_SHORT);
                isUserLoggedIn = false;
                return true;
            case 3:
                doServerLogout();
                isUserLoggedIn = false;
                return true;
            case 4:
                doServerUnregisterDevice();
                isUserLoggedIn = false;
                return true;
        }
        return false;
    }

    @Override
    public void showMessage(final String message, final int toastLength) {
        if (message.toLowerCase().contains("jwt")) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("JWT Error");
            alertDialog.setMessage(message);

            alertDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        // TODO Add your code for the button here.
                        }
            });

            alertDialog.show();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ExampleActivity.this, message, toastLength).show();
                }
            });
        }
    }

    @Override
    public URI getJsonDownloadUri() {
        return productListDownloadUri;
    }

    @Override
    public void setProgressVisibility(int visibility) {
        progressBar.setVisibility(visibility);
    }
    @Override
    public void setDownloadedJson(String json) {
        try {
            List<Map<String, String>> list;

            List<Object> objects;
            if (json == null || json.trim().length() < 1) {
                list = Collections.emptyList();
            } else {
                list = parseProductListJson(json);
            }
            //itemList.setAdapter(new ArrayAdapter<Object>(this, R.layout.listitem, objects));

            String[] mapKeys = new String[] {TEXT1, TEXT2};
            int[] layoutId = new int[] {android.R.id.text1, android.R.id.text2};

            itemList.setAdapter(new SimpleAdapter(
                    this, list, android.R.layout.simple_list_item_2, mapKeys, layoutId));

            if (isUserLoggedIn)
                GetUserNameToast();

        } catch (JSONException e) {
            showMessage("Error: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    public void GetUserNameToast() {
        SharedPreferences sharedUsername =
                getApplicationContext().getSharedPreferences("Login", Context.MODE_WORLD_READABLE);

        String user1 = sharedUsername.getString("", "");
        Toast toast = new Toast(getApplicationContext());

        toast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
        toast.makeText(
                ExampleActivity.this, "Showing results for Enterprise Plus member: " + user1,
                toast.LENGTH_LONG).show();
    }

    /**
     * Utility method that parses a URI without throwing a checked exception if parsing fails.
     *
     * @param uri a URI to parse, or null.
     * @return a parsed URI, or null.  Never null if uri is a valid URI.
     */
    protected final URI uri(String uri) {
        try {
            if (uri != null)
                return new URI(uri);
        } catch (URISyntaxException e) {
            Log.d(TAG, "invalid URI: " + uri, e);
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PROGRESS_VISIBILITY, progressBar.getVisibility());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (usedMobileSso)
            mobileSso().processPendingRequests();
    }

    private void initAppEndpoint() {

        MobileSso mobileSso = mobileSso();

        if (isUserLoggedIn == true) {
            productListDownloadUri = mobileSso.getURI(
                    mobileSso.getPrefix() + "/demo/member/products?operation=listProducts");
        } else {
            productListDownloadUri = mobileSso.getURI(
                    mobileSso.getPrefix() + "/demo/products?operation=listProducts");
        }
    }

    // Log the user out of all client apps and notify the server to revoke tokens.
    private void doServerLogout() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mobileSso().logout(true);
                    showMessage("Logged Out Successfully", Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    String msg = "Server Logout Failed: " + e.getMessage();
                    Log.e(TAG, msg, e);
                    showMessage(msg, Toast.LENGTH_LONG);
                }
                return null;
            }
        }.execute((Void) null);
    }

    // Tell the token server to un-register this device, without affecting the client-side token
    // caches in any way.
    private void doServerUnregisterDevice() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mobileSso().removeDeviceRegistration();
                    showMessage("Server Registration Removed for This Device", Toast.LENGTH_LONG);
                } catch (Exception e) {
                    String msg = "Server Device Removal Failed: " + e.getMessage();
                    Log.e(TAG, msg, e);
                    showMessage(msg, Toast.LENGTH_LONG);
                }
                return null;
            }
        }.execute((Void) null);
    }

    private static List<Map<String, String>> parseProductListJson(String json)
            throws JSONException {

        JSONObject parsed = (JSONObject) new JSONTokener(json).nextValue();
        JSONArray items = parsed.getJSONArray("products");

        if (isUserLoggedIn == true) {
            return getMemberProducts(items);
        } else {
            return getProducts(items);
        }
    }

    private static List<Map<String, String>> getMemberProducts(JSONArray items)
            throws JSONException {

        try {
            List<Map<String, String>> listObjects =
                    new ArrayList<Map<String, String>>(items.length());

            for (int i = 0; i < items.length(); ++i) {

                Map<String, String> objects = new HashMap<String, String>();

                JSONObject item = (JSONObject) items.get(i);
                Integer id      = (Integer) item.get("id");
                String size     = (String) item.get("size");
                String type     = (String) item.get("type");
                String location = (String) item.get("location");
                String price    = (String) item.get("price");

                objects.put(TEXT1, id + ". " + size + " " + type);
                objects.put(TEXT2, location + " - " + price);
                listObjects.add(objects);
            }

            return listObjects;
        } catch (ClassCastException e) {
            throw (JSONException) new JSONException(
                    "Response JSON was not in the expected format").initCause(e);
        }
    }

    private static List<Map<String, String>> getProducts(JSONArray items) throws JSONException {
        try {
            List<Map<String, String>> listObjects =
                    new ArrayList<Map<String, String>>(items.length());

            for (int i = 0; i < items.length(); ++i) {

                Map<String, String> objects = new HashMap<String, String>();

                JSONObject item = (JSONObject) items.get(i);
                Integer id      = (Integer) item.get("id");
                String size     = (String) item.get("size");
                String location = (String) item.get("location");

                objects.put(TEXT1, id + ". " + size);
                objects.put(TEXT2, location);
                listObjects.add(objects);
            }

            return listObjects;
        } catch (ClassCastException e) {
            throw (JSONException) new JSONException(
                    "Response JSON was not in the expected format").initCause(e);
        }
    }
}

