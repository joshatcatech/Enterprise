/*
 * Created by awitrisna on 2013-11-15.
 * Copyright (c) 2013 CA Technologies. All rights reserved.
 */

package com.ca.apim.mag.enterprise;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

//import com.l7tech.msso.app.App;
//import com.l7tech.msso.service.MssoIntents;

import com.ca.mas.core.app.App;
import com.ca.mas.core.service.MssoIntents;

import java.util.List;

public class EnterpriseBrowser extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enterprisebrowser);
        final GridView gridview = (GridView) findViewById(R.id.gridview);
        final ImageAdapter imageAdapter = new ImageAdapter(this);
        imageAdapter.setApps((List<App>) getIntent().getExtras().getSerializable(MssoIntents.EXTRA_APPS));
        gridview.setAdapter(imageAdapter);
        gridview.refreshDrawableState();
    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private List<App> apps;

        public void setApps(List<App> apps) {
            this.apps = apps;
        }

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            if (apps != null) {
                return apps.size();
            } else {
                return 0;
            }
        }

        public Object getItem(int position) {
            return apps.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(300, 300));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
                apps.get(position).renderIcon(EnterpriseBrowser.this, imageView, new ResultReceiver(null){
                    @Override
                    protected void onReceiveResult(final int resultCode, final Bundle resultData) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (resultData != null) {
                                    String message = resultData.getString(MssoIntents.RESULT_ERROR_MESSAGE);
                                    if (message != null) {
                                        Toast.makeText(EnterpriseBrowser.this, message, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                    }
                });
            } else {
                imageView = (ImageView) convertView;
            }
            return imageView;
        }
    }
}