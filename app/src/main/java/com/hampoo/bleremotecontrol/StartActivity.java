package com.hampoo.bleremotecontrol;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by jie.wu on 2016/11/18.
 */

public class StartActivity extends AppCompatActivity {
    private static final String TAG = StartActivity.class.getSimpleName();
    public static final String ACTION_MSG = "com.hampoo.action.MSG";
    public static final String ACTION_MSG_LOG = "com.hampoo.action.MSG_LOG";
    public static final String ACTION_CONNECTED = "com.hampoo.action.CONNECTED";
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    private final boolean isLog = true;
    private TextView tv_msg, tv_log;
    private ScrollView scrollView;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_start);
        tv_msg = (TextView)findViewById(R.id.tv_msg);
        tv_log = (TextView)findViewById(R.id.tv_log);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        mBroadcastReceiver = new MsgBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MSG);
        intentFilter.addAction(ACTION_MSG_LOG);
        intentFilter.addAction(ACTION_CONNECTED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyIfRequestPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void startService(){
        tv_msg.setText("正在扫描遥控器，请稍候...");
        startService(new Intent(this, RemoteControlService.class));
    }

    private void stopService(){
        stopService(new Intent(this, RemoteControlService.class));
    }

    private class MsgBroadcastReceiver extends BootBroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(ACTION_MSG)){
                showMsg(intent);
            }else if(action.equals(ACTION_CONNECTED)){
                showMsg(intent);

                /*new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StartActivity.this.finish();
                    }
                }, 1000);*/
            }else if(action.equals(ACTION_MSG_LOG)){
                if(isLog) {
                    if(tv_log.getLineCount()>100){
                        tv_log.setText("");
                    }
                    String msg = intent.getStringExtra("msg");
                    tv_log.append(msg.concat("\n"));
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    }

    private void showMsg(Intent intent){
        try {
            String msg = intent.getStringExtra("msg");
            tv_msg.setText(msg);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void verifyIfRequestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.i(TAG, "onCreate: checkSelfPermission");
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onCreate: Android 6.0 动态申请权限");

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS)) {
                    Log.i(TAG, "*********onCreate: shouldShowRequestPermissionRationale**********");
                    Toast.makeText(this, "只有允许访问位置才能搜索到蓝牙设备", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_ACCESS_COARSE_LOCATION);
                }
            } else {
                startService();
            }
        } else {
            startService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            Log.i(TAG, "onRequestPermissionsResult: permissions.length = " + permissions.length +
                    ", grantResults.length = " + grantResults.length);
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                startService();
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, "位置访问权限被拒绝将无法搜索到ble设备", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
