/*
 * Created by awitrisna on 2013-11-15.
 * Copyright (c) 2013 CA Technologies. All rights reserved.
 */

package com.ca.apim.mag.enterprise;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.l7tech.msso.EnterpriseApp;
import com.l7tech.msso.MobileSso;
import com.l7tech.msso.MobileSsoFactory;
import com.l7tech.msso.service.MssoIntents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExampleActivity extends FragmentActivity implements JsonDownloaderFragment.UserActivity {
    private static final String TAG = "ExampleA";

    private static final String STATE_PROGRESS_VISIBILITY = "exampleActivity.progressVisibility";

    private static final int MENU_GROUP_LOGOUT = 66;
    private static final int MENU_ITEM_LOG_OUT = 3;
    private static final int MENU_ITEM_LOG_OUT_CLIENT_ONLY = 1;
    private static final int MENU_ITEM_REMOVE_DEVICE_REGISTRATION = 4;
    private static final int MENU_ITEM_DESTROY_TOKEN_STORE = 2;
    private static final int ACTION_SETTINGS_CONFIG_MENU = 1;

    //Application endpoint
    private URI productListDownloadUri = null;

    private  URI userInfoUri = null;

    ListView itemList;
    ProgressBar progressBar;
    static boolean usedMobileSso = false;


    @Override
    public MobileSso mobileSso() {
        //Initialize the MobileSso with the configuration defined under /assets/msso_config.json
        MobileSso mobileSso = MobileSsoFactory.getInstance(this);
        usedMobileSso = true;
        return mobileSso;
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
            progressBar.setVisibility(savedInstanceState.getInt(STATE_PROGRESS_VISIBILITY));
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        JsonDownloaderFragment httpFragment = (JsonDownloaderFragment) fragmentManager.findFragmentByTag("httpResponseFragment");
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
                ArrayAdapter a = ((ArrayAdapter) itemList.getAdapter());
                if (a != null) {
                    a.clear();
                    a.notifyDataSetChanged();
                }
                finalHttpFragment.downloadJson();
            }
        });

        final Button logOutButton = (Button) findViewById(R.id.logOutButton);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doServerLogout();
            }
        });
        registerForContextMenu(logOutButton);

        initAppEndpoint();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(MENU_GROUP_LOGOUT, MENU_ITEM_LOG_OUT, Menu.NONE, "Log Out");
        menu.add(MENU_GROUP_LOGOUT, MENU_ITEM_LOG_OUT_CLIENT_ONLY, Menu.NONE, "Log Out (client only)");
        menu.add(MENU_GROUP_LOGOUT, MENU_ITEM_REMOVE_DEVICE_REGISTRATION, Menu.NONE, "Unregister Device");
        menu.add(MENU_GROUP_LOGOUT, MENU_ITEM_DESTROY_TOKEN_STORE, Menu.NONE, "Destroy Token Store");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (66 != item.getGroupId())
            return false;
        switch (item.getItemId()) {
            case 1:
                mobileSso().logout(false);
                showMessage("Logged Out (client only)", Toast.LENGTH_SHORT);
                return true;
            case 2:
                mobileSso().destroyAllPersistentTokens();
                showMessage("Device Registration Destroyed (client only)", Toast.LENGTH_SHORT);
                return true;
            case 3:
                doServerLogout();
                return true;
            case 4:
                doServerUnregisterDevice();
                return true;
        }
        return false;
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

    // Tell the token server to un-register this device, without affecting the client-side token caches in any way.
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
        setupForegroundDispatch();
    }

    @Override
    public void showMessage(final String message, final int toastLength) {
        if (message.toLowerCase().contains("jwt")) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("JWT Error");
            alertDialog.setMessage(message);
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
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
            List<Object> objects;
            if (json == null || json.trim().length() < 1) {
                objects = Collections.emptyList();
            } else {
                objects = parseProductListJson(json);
            }
            itemList.setAdapter(new ArrayAdapter<Object>(this, R.layout.listitem, objects));
            GetUserNameToast();
        } catch (JSONException e) {
            showMessage("Error: " + e.getMessage(), Toast.LENGTH_LONG);
        }

    }
    public void GetUserNameToast() {
        SharedPreferences sharedUsername = getApplicationContext().getSharedPreferences("Login", Context.MODE_WORLD_READABLE);
        String user1 = sharedUsername.getString("", "");
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
        toast.makeText(ExampleActivity.this, "Showing coupons for User: " + user1, toast.LENGTH_LONG).show();
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

    private static List<Object> parseProductListJson(String json) throws JSONException {
        try {
            List<Object> objects = new ArrayList<Object>();
            JSONObject parsed = (JSONObject) new JSONTokener(json).nextValue();
            JSONArray items = parsed.getJSONArray("products");
            for (int i = 0; i < items.length(); ++i) {
                JSONObject item = (JSONObject) items.get(i);
                Integer id = (Integer) item.get("id");
                String name = (String) item.get("name");
                objects.add(new Pair<Integer, String>(id, name) {
                    @Override
                    public String toString() {
                        return first + "  " + second;
                    }
                });
            }
            return objects;
        } catch (ClassCastException e) {
            throw (JSONException) new JSONException("Response JSON was not in the expected format").initCause(e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.entBrowser:
                mobileSso();
                startEnterpriseBrowser();
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, CustomConfigurationActivity.class);
                startActivityForResult(i, ACTION_SETTINGS_CONFIG_MENU);
                break;
            case R.id.scanQRCode:
                IntentIntegrator intentIntegrator = new IntentIntegrator(this);
                intentIntegrator.initiateScan();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            //Receive NFC message for remote login
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
                String type = intent.getType();
                if ("text/plain".equals(type)) {
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    Ndef ndef = Ndef.get(tag);
                    NdefMessage ndefMessage = ndef.getCachedNdefMessage();

                    NdefRecord[] records = ndefMessage.getRecords();
                    if (records.length > 0) {
                        String url = new String(records[0].getPayload());
                        if (url != null) {
                            mobileSso().authorize(url, new ResultReceiver(null) {
                                @Override
                                protected void onReceiveResult(int resultCode, Bundle resultData) {
                                    if (resultCode != MssoIntents.RESULT_CODE_SUCCESS) {
                                        showMessage(resultData.getString(MssoIntents.RESULT_ERROR_MESSAGE), Toast.LENGTH_LONG);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    /**
     * Listen NFC for remote login
     */
    public void setupForegroundDispatch() {
        final Intent intent = new Intent(this.getApplicationContext(), ExampleActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        //adapter.enableForegroundDispatch(this, pendingIntent, filters, techList);
        if (adapter != null ) {
            adapter.enableForegroundDispatch(this, pendingIntent, filters, techList);
        }
    }


    private void startEnterpriseBrowser() {

        EnterpriseApp.getInstance().processEnterpriseApp(ExampleActivity.this, new ResultReceiver(null) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode != MssoIntents.RESULT_CODE_SUCCESS) {
                    String message = resultData.getString(MssoIntents.RESULT_ERROR_MESSAGE);
                    if (message == null) {
                        message = "<Unknown error>";
                    }
                    showMessage(message, Toast.LENGTH_LONG);
                }
            }
        }, ExampleApp.class);
    }
    private void initAppEndpoint() {
        if (productListDownloadUri == null || userInfoUri == null) {
            MobileSso mobileSso = mobileSso();
            userInfoUri = mobileSso.getURI(mobileSso.getPrefix()+"/openid/connect/v1/userinfo");
            productListDownloadUri = mobileSso.getURI(mobileSso.getPrefix()+"/protected/resource/products?operation=listProducts");
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        //Got the QR Code, perform the remote login request.
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            String r = scanResult.getContents();
            if (r != null) {
                mobileSso().authorize(r, new ResultReceiver(null) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (resultCode != MssoIntents.RESULT_CODE_SUCCESS) {
                            showMessage(resultData.getString(MssoIntents.RESULT_ERROR_MESSAGE), Toast.LENGTH_LONG);
                        }
                    }
                });
            }
        }
    }

}

