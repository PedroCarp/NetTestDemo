package com.rt.nettestdemo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rt.nettestdemo.wifi.NetCheckUtils;
import com.rt.nettestdemo.wifi.WifiAutoConnectManager;

import java.util.ArrayList;
import java.util.List;

public class CheckNetWork extends Worker {

    private boolean isNetOnline;
    private Context mContext;
    private WorkerParameters workerParameters;
    private WifiManager wifiManager;


    public CheckNetWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
        this.workerParameters = workerParams;
        //初始化wifi工具
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        //接受传第过来的参数 这里的data与MainActivity的data 需要一致
        String data = workerParameters.getInputData().getString("CheckNet");
        Log.e("work-------------------",data);
        if (mHandler==null){
            mHandler = new Handler(Looper.getMainLooper());
            mTimeCounterRunnable.run();
        }

        //返回处理的结果
        Data outData = new Data.Builder().putBoolean("isNetOnline",isNetOnline).build();
        @SuppressLint("RestrictedApi")
        Result.Success success = new Result.Success(outData);
        return success;
    }

    private void checkNet(){
        if (NetCheckUtils.isNetOnline()){
            isNetOnline = true;
            Log.e("Work---net","网络正常");
        }else {
            isNetOnline = false;
            Log.e("Work---net","网路异常");
            wifiManager.disconnect();
        }
    }



    private Handler mHandler;// 全局变量
    private Runnable mTimeCounterRunnable = new Runnable() {
        @Override
        public void run() {
            checkNet();
            Log.e("Work---net---mTimeCounterRunnable","Start");
            mHandler.postDelayed(this, 60 * 1000);
        }
    };
}
