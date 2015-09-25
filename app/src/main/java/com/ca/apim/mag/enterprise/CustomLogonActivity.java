/*
 * Created by awitrisna on 2013-11-15.
 * Copyright (c) 2013 CA Technologies. All rights reserved.
 */

package com.ca.apim.mag.enterprise;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

//import com.l7tech.msso.gui.AbstractLogonActivity;
import com.ca.mas.core.gui.AbstractLogonActivity;

public class CustomLogonActivity extends AbstractLogonActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customlogon);
        final Button logonButton = (Button) findViewById(R.id.btnlogin);
        if (isEnterpriseLoginEnabled()) {
            logonButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String username = ((EditText) findViewById(R.id.etxtname)).getText().toString();
                    final String password = ((EditText) findViewById(R.id.etxtpass)).getText().toString();

                    sendCredentialsIntent(username, password);
                    savePreferences(username);


                    setResult(RESULT_OK);
                    finish();
                }
            });
        }
        else {
            logonButton.setEnabled(false);
        }
    }

    private void savePreferences(String username) {
        SharedPreferences sharedUsername = getApplicationContext().getSharedPreferences("Login", Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor toEdit = sharedUsername.edit();
        toEdit.putString("",username);
        toEdit.commit();
    }

}
