package com.rt.nettestdemo;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rt.nettestdemo.wifi.WifiAutoConnectManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{
    public static final int WIFI_SCAN_PERMISSION_CODE = 2;
    WorkAsyncTask mWorkAsyncTask = null;
    ConnectAsyncTask mConnectAsyncTask = null;
    List<ScanResult> mScanResultList = new ArrayList<>();
    String ssid = "";
    WifiAutoConnectManager.WifiCipherType type = WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS;
    String password = "11111111";
    boolean isLinked = false;

    String gateway = "";
    String mac = "";
    /**
     * ??????????????????????????????????????????????????????
     */
    private BroadcastReceiver mWifiSearchBroadcastReceiver;
    private IntentFilter mWifiSearchIntentFilter;
    private BroadcastReceiver mWifiConnectBroadcastReceiver;
    private IntentFilter mWifiConnectIntentFilter;
    private WifiAutoConnectManager mWifiAutoConnectManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //?????????wifi??????
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiAutoConnectManager = WifiAutoConnectManager.newInstance(wifiManager);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ??????wifi????????????????????????,??????????????????
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            }, WIFI_SCAN_PERMISSION_CODE);
            return;
        }

        //????????????wifi??????????????????
        initWifiSate();
        searchWifiList();
        startWork();

    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiSearchBroadcastReceiver, mWifiSearchIntentFilter);
        registerReceiver(mWifiConnectBroadcastReceiver, mWifiConnectIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiSearchBroadcastReceiver);
        unregisterReceiver(mWifiConnectBroadcastReceiver);
    }
    private void searchWifiList(){
        if (mWorkAsyncTask != null) {
            mWorkAsyncTask.cancel(true);
            mWorkAsyncTask = null;
        }
        mWorkAsyncTask = new WorkAsyncTask();
        mWorkAsyncTask.execute();
    }
    private void connectWifi(){
        if (ssid.equals(WifiAutoConnectManager.getSSID())) {
            return;
        }
        if (mConnectAsyncTask != null) {
            mConnectAsyncTask.cancel(true);
            mConnectAsyncTask = null;
        }
        mConnectAsyncTask = new ConnectAsyncTask(ssid, password, type);
        mConnectAsyncTask.execute();
    }

    private void startWork(){
        Data data = new Data.Builder().putString("CheckNet","????????????").build();
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(CheckNetWork.class)
                .addTag("CheckNet")
                .setInputData(data)
                .setInitialDelay(10, TimeUnit.MINUTES)//??????10????????????
                .build();

        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest);
    }




    private void initWifiSate() {
        //wifi ????????????????????????
        mWifiSearchBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {// ??????????????????
                    mScanResultList = WifiAutoConnectManager.getScanResults();
                }
            }
        };
        mWifiSearchIntentFilter = new IntentFilter();
        mWifiSearchIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mWifiSearchIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiSearchIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        //wifi ????????????????????????
        mWifiConnectBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    int wifState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    if (wifState != WifiManager.WIFI_STATE_ENABLED) {
                        Toast.makeText(MainActivity.this, "??????wifi", Toast.LENGTH_SHORT).show();
                    }
                } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                    int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
                    Log.e("wifidemo", ssid + "linkWifiResult:" + linkWifiResult);
                    if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                        Log.e("wifidemo", ssid + "onReceive:????????????");
                    }
                } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo.DetailedState state = ((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
                    setWifiState(state);
                }
            }
        };
        mWifiConnectIntentFilter = new IntentFilter();
        mWifiConnectIntentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
        mWifiConnectIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiConnectIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mWifiConnectIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    /**
     * ??????wifi??????
     *
     * @param state
     */
    public void setWifiState(final NetworkInfo.DetailedState state) {

        if (state == NetworkInfo.DetailedState.AUTHENTICATING) {

        } else if (state == NetworkInfo.DetailedState.BLOCKED) {

        } else if (state == NetworkInfo.DetailedState.CONNECTED) {
            Log.e("work--wifi:","CONNECTED");
            isLinked = true;
            ssid = WifiAutoConnectManager.getSSID();
            type = WifiAutoConnectManager.getCipherType(ssid);
            gateway = WifiAutoConnectManager.getGateway();
            mac = WifiAutoConnectManager.getMacAddress();
        } else if (state == NetworkInfo.DetailedState.CONNECTING) {
            Log.e("work--wifi:","CONNECTING");
            isLinked = false;
        } else if (state == NetworkInfo.DetailedState.DISCONNECTED) {
            Log.e("work--wifi:","DISCONNECTED");
            isLinked = false;
        } else if (state == NetworkInfo.DetailedState.DISCONNECTING) {
            Log.e("work--wifi:","DISCONNECTING");
            isLinked = false;
        } else if (state == NetworkInfo.DetailedState.FAILED) {
            Log.e("work--wifi:","FAILED");
            isLinked = false;
        } else if (state == NetworkInfo.DetailedState.IDLE) {

        } else if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {

        } else if (state == NetworkInfo.DetailedState.SCANNING) {

        } else if (state == NetworkInfo.DetailedState.SUSPENDED) {

        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WIFI_SCAN_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // ?????????
                    Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * ??????wifi??????
     */
    private class WorkAsyncTask extends AsyncTask<Void, Void, List<ScanResult>> {
        private List<ScanResult> mScanResult = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<ScanResult> doInBackground(Void... params) {
            if (WifiAutoConnectManager.startStan()) {
                mScanResult = WifiAutoConnectManager.getScanResults();
            }
            List<ScanResult> filterScanResultList = new ArrayList<>();
            if (mScanResult != null) {
                for (ScanResult wifi : mScanResult) {
                    filterScanResultList.add(wifi);
                    Log.e("wifidemo", "doInBackground: wifi:" + wifi);
                }
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return filterScanResultList;
        }

        @Override
        protected void onPostExecute(final List<ScanResult> result) {
            super.onPostExecute(result);
            mScanResultList = result;

        }
    }



    /**
     * ???????????????wifi
     */
    class ConnectAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private String ssid;
        private String password;
        private WifiAutoConnectManager.WifiCipherType type;
        WifiConfiguration tempConfig;

        public ConnectAsyncTask(String ssid, String password, WifiAutoConnectManager.WifiCipherType type) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // ??????wifi
            mWifiAutoConnectManager.openWifi();
            // ??????wifi????????????????????????(?????????????????????????????????1-3?????????)??????????????????wifi
            // ????????????WIFI_STATE_ENABLED????????????????????????????????????
            while (mWifiAutoConnectManager.wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                try {
                    // ????????????????????????while?????????????????????100??????????????????
                    Thread.sleep(100);

                } catch (InterruptedException ie) {
                    Log.e("wifidemo", ie.toString());
                }
            }

            tempConfig = mWifiAutoConnectManager.isExsits(ssid);
            //????????????wifi
            for (WifiConfiguration c : mWifiAutoConnectManager.wifiManager.getConfiguredNetworks()) {
                mWifiAutoConnectManager.wifiManager.disableNetwork(c.networkId);
            }
            if (tempConfig != null) {
                Log.d("wifidemo", ssid + "????????????");
                boolean result = mWifiAutoConnectManager.wifiManager.enableNetwork(tempConfig.networkId, true);
                if (!isLinked && type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
                    try {
                        Thread.sleep(5000);//??????5s????????????
                        if (!isLinked) {
                            Log.d("wifidemo", ssid + "???????????????");
                            mWifiAutoConnectManager.wifiManager.disableNetwork(tempConfig.networkId);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "????????????!?????????????????????wifi????????????????????????", Toast.LENGTH_SHORT).show();
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("???????????????")
                                            .setMessage("?????????????????????wifi????????????????????????")
                                            .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent();
                                                    intent.setAction("android.net.wifi.PICK_WIFI_NETWORK");
                                                    startActivity(intent);
                                                }
                                            }).show();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Log.d("wifidemo", "result=" + result);
                return result;
            } else {
                Log.d("wifidemo", ssid + "??????????????????");
                if (type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final EditText inputServer = new EditText(MainActivity.this);
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("???????????????")
                                    .setView(inputServer)
                                    .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                            password = inputServer.getText().toString();
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    WifiConfiguration wifiConfig = mWifiAutoConnectManager.createWifiInfo(ssid, password,
                                                            type);
                                                    if (wifiConfig == null) {
                                                        Log.d("wifidemo", "wifiConfig is null!");
                                                        return;
                                                    }
                                                    Log.d("wifidemo", wifiConfig.SSID);

                                                    int netID = mWifiAutoConnectManager.wifiManager.addNetwork(wifiConfig);
                                                    boolean enabled = mWifiAutoConnectManager.wifiManager.enableNetwork(netID, true);
                                                    Log.d("wifidemo", "enableNetwork status enable=" + enabled);
//                                                    Log.d("wifidemo", "enableNetwork connected=" + mWifiAutoConnectManager.wifiManager.reconnect());
//                                                    mWifiAutoConnectManager.wifiManager.reconnect();
                                                }
                                            }).start();
                                        }
                                    }).show();
                        }
                    });
                } else {
                    WifiConfiguration wifiConfig = mWifiAutoConnectManager.createWifiInfo(ssid, password, type);
                    if (wifiConfig == null) {
                        Log.d("wifidemo", "wifiConfig is null!");
                        return false;
                    }
                    Log.d("wifidemo", wifiConfig.SSID);
                    int netID = mWifiAutoConnectManager.wifiManager.addNetwork(wifiConfig);
                    boolean enabled = mWifiAutoConnectManager.wifiManager.enableNetwork(netID, true);
                    Log.d("wifidemo", "enableNetwork status enable=" + enabled);
//                    Log.d("wifidemo", "enableNetwork connected=" + mWifiAutoConnectManager.wifiManager.reconnect());
//                    return mWifiAutoConnectManager.wifiManager.reconnect();
                    return enabled;
                }
                return false;


            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mConnectAsyncTask = null;
        }
    }
}