package com.hampoo.bleremotecontrol;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.hampoo.blelib.BleService;
import com.hampoo.bleremotecontrol.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jie.wu on 2016/11/17.
 */

public class RemoteControlServiceOld extends Service {
    private static final String TAG = RemoteControlService.class.getSimpleName();
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"; //"0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"; //"0000fff1-0000-1000-8000-00805f9b34fb";
    private static final int AUTO_CONNECT_DEVICE = 10001;
    private static final int NOT_SCAN_DEVICE = 10002;
    private static final int DISCONNECT_TIME_OUT = 10003;

    private static final long DELAY_MILLIS = 60 * 1000;
    private static final String filterName = "box";

    private List<Device> deviceList;
    private BleService mBleService;
    private boolean mIsBind;
    private String connDeviceName;
    private String connDeviceAddress;
    private int mPosition = 0;
    /**
     * 是否手动停止自动连接
     **/
    private boolean isManual = false;
    /**
     * 是否连接过设备
     **/
    private boolean isHasDevice = false;
    /**
     * 是否扫描到指定的设备
     **/
    private boolean isScanDevice = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AUTO_CONNECT_DEVICE:
                    if(null != mBleService && !mBleService.isScanning()) {
                        mBleService.scanLeDevice(true);
                    }
                    break;
                case NOT_SCAN_DEVICE:
                    showDialog("长时间没有扫描到遥控器，请查看附近有无遥控器或检查摇控器是否有电量");
                    break;
                case DISCONNECT_TIME_OUT:
                    sendMsg("尝试重连遥控器多次，仍无法连接，请拔下遥控器电池再重试");
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        init();
    }

    private void init(){
        initData();
        registerReceiver(bleReceiver, makeIntentFilter());
        registerReceiver(stateReceiver, makeStateFilter());
        bindService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(deviceList!=null && deviceList.size()>0){
            Device d = deviceList.get(0);
            if(d.isConnect()){
                sendMsg("Name：" + d.getName() + "\nAddress：" + d.getAddress());
            }
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
    }

    private void close(){
        doUnBindService();
        unregisterReceiver(bleReceiver);
        unregisterReceiver(stateReceiver);
    }


    private void initData() {
        deviceList = new ArrayList<>();
        connDeviceName = SPUtils.getString(SPUtils.DEVICE_NAME, null);
        connDeviceAddress = SPUtils.getString(SPUtils.DEVICE_ADDRESS, null);
        if (!TextUtils.isEmpty(connDeviceName) && !TextUtils.isEmpty(connDeviceAddress)) {
            Device device = new Device();
            device.setName(connDeviceName);
            device.setAddress(connDeviceAddress);
            device.setConnect(false);
            deviceList.add(device);
            isHasDevice = true;
        }
    }

    private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                Log.e(TAG, "name: " + tmpDevName + ", address: " + tmpDevAddress);

                if(TextUtils.isEmpty(tmpDevName) || !tmpDevName.toLowerCase().contains(filterName)){
                    return;
                }

                if (isHasDevice) {
                    if (!TextUtils.isEmpty(tmpDevAddress) && tmpDevAddress.equals(connDeviceAddress)) {
                        isScanDevice = true;
                        if (mBleService.isScanning()) {
                            mBleService.scanLeDevice(false);
                        }
                    }
                } else {
                    Device device = new Device();
                    device.setName(tmpDevName);
                    device.setAddress(tmpDevAddress);
                    device.setConnect(false);
                    deviceList.add(device);
                }
            } else if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
                deviceList.get(mPosition).setConnect(true);
                Log.e(TAG, "----------------------------ACTION_GATT_CONNECTED");
                mHandler.removeCallbacksAndMessages(null);
                sendMsgAndFinish("Name：" + connDeviceName + "\nAddress：" + connDeviceAddress);
            } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
                Log.e(TAG, "----------------------------断开连接...");
                mHandler.sendEmptyMessage(DISCONNECT_TIME_OUT);
                deviceList.get(mPosition).setConnect(false);

                if (null != mBleService && !mBleService.isConnect() && !isManual) {
                    mBleService.connect(connDeviceAddress);
                }else {
                    mHandler.sendEmptyMessage(AUTO_CONNECT_DEVICE);
                }
            } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
                if (isHasDevice) {
                    if (isScanDevice && deviceList.size()>0) {
                        Log.e(TAG, "----------------------------扫描到摇控器" + connDeviceName + "，准备连接");
                        sendMsg("扫描到摇控器" + connDeviceName + "，正在连接...");
                        mBleService.connect(connDeviceAddress);
                    } else {
                        Log.e(TAG, "----------------------------没有扫描到摇控器，重新扫描");
                        sendMsg("没有扫描到摇控器，重新扫描中...");
                        mHandler.sendEmptyMessage(AUTO_CONNECT_DEVICE);
                    }
                    isHasDevice = false;
                } else {
                    if (null != deviceList && deviceList.size() > 0) {
                        //删除超时消息
                        mHandler.removeMessages(NOT_SCAN_DEVICE);

                        Device device = deviceList.get(0);
                        connDeviceAddress = device.getAddress();
                        connDeviceName = device.getName();
                        SPUtils.putString(SPUtils.DEVICE_ADDRESS, connDeviceAddress);
                        SPUtils.putString(SPUtils.DEVICE_NAME, connDeviceName);

                        Device d = new Device();
                        d.setName(connDeviceName);
                        d.setAddress(connDeviceAddress);
                        d.setConnect(false);
                        deviceList.clear();
                        deviceList.add(d);

                        Log.e(TAG, "----------------------------扫描到摇控器" + connDeviceName + "，准备连接");
                        sendMsg("扫描到摇控器" + connDeviceName + "，正在连接...");
                        mBleService.connect(connDeviceAddress);
                    } else {
                        Log.e(TAG, "----------------------------没有扫描到摇控器，重新扫描");
                        sendMsg("没有扫描到摇控器，重新扫描中...");
                        mHandler.sendEmptyMessage(AUTO_CONNECT_DEVICE);
                    }
                }
            }
        }
    };

    private IntentFilter makeIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_BLUETOOTH_DEVICE);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_SCAN_FINISHED);
        return intentFilter;
    }

    private IntentFilter makeStateFilter(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);  //监听蓝牙开关状态
        return intentFilter;
    }

    private BroadcastReceiver stateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch(blueState){
                        case BluetoothAdapter.STATE_TURNING_ON:
                            // 正在打开
                            Log.e(TAG, "正在打开");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            // 已打开
                            Log.e(TAG, "已打开");
                            mHandler.sendEmptyMessage(AUTO_CONNECT_DEVICE);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            // 正在关闭
                            Log.e(TAG, "正在关闭");
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            // 已关闭
                            Log.e(TAG, "已关闭");
                            break;
                    }
                    break;
            }
        }
    };

    /**
     * 绑定服务
     */
    private void bindService() {
        Intent serviceIntent = new Intent(this, BleService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 解绑服务
     */
    private void doUnBindService() {
        if (mIsBind) {
            unbindService(serviceConnection);
            mBleService = null;
            mIsBind = false;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleService = ((BleService.LocalBinder) service).getService();
            if (mBleService != null) {
                mIsBind = true;
                // 设置监听
                setBleServiceListener();
                // 初始化
                if (mBleService.initialize()) {
                    if (mBleService.enableBluetooth(true)) {
                        mBleService.scanLeDevice(true);
                        mHandler.sendEmptyMessageDelayed(NOT_SCAN_DEVICE, DELAY_MILLIS);
                        //Toast.makeText(MyApplication.getContext(), "Bluetooth was opened", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MyApplication.getContext(), "Not support Bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Log.e(TAG, "----------------------------onServiceDisconnected");
            mBleService = null;
            mIsBind = false;
        }
    };

    private void setBleServiceListener() {
        mBleService.setOnServicesDiscoveredListener(new BleService.OnServicesDiscoveredListener() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "----------------------------GATT_SUCCESS");
                    //mBleService.setCharacteristicNotification(SERVICE_UUID, CHARACTERISTIC_UUID, true);
                    if (isHasDevice) {
                        //Log.e(TAG, "----------------------------自动连接到摇控器成功");
                        isHasDevice = false;
                        isScanDevice = false;
                    }
                }
            }
        });
    }

    private void sendMsg(String msg) {
        Intent intent = new Intent();
        intent.setAction(StartActivity.ACTION_MSG);
        intent.putExtra("msg", msg);
        sendBroadcast(intent);
    }

    private void sendMsgAndFinish(String msg) {
        Intent intent = new Intent();
        intent.setAction(StartActivity.ACTION_CONNECTED);
        intent.putExtra("msg", msg);
        sendBroadcast(intent);
    }

    private void showDialog(String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(str)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
}