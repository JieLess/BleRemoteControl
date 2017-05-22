package com.hampoo.bleremotecontrol;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.hampoo.bleremotecontrol.adapter.CommonAdapter;
import com.hampoo.bleremotecontrol.adapter.ViewHolder;
import com.hampoo.blelib.BleService;
import com.hampoo.bleremotecontrol.utils.SPUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    private static final int AUTO_CONNECT_DEVICE = 10001;

    private Button btn_scanBle, btn_state;
    private ListView lv_device;

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private List<Map<String, Object>> deviceList;
    private CommonAdapter<Map<String, Object>> deviceAdapter;
    private BleService mBleService;
    private boolean mIsBind;
    private String connDeviceName;
    private String connDeviceAddress;
    private int mPosition = 0;
    /**是否手动停止自动连接**/
    private boolean isManual = false;
    /**是否连接过设备**/
    private boolean isHasDevice = false;
    /**是否扫描到指定的设备**/
    private boolean isScanDevice = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AUTO_CONNECT_DEVICE:
                    mBleService.scanLeDevice(true);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);

        intView();
        initData();
        registerReceiver(bleReceiver, makeIntentFilter());
        bindService();
    }

    private void intView() {
        btn_scanBle = (Button)findViewById(R.id.btn_scanBle);
        btn_state = (Button)findViewById(R.id.btn_state);
        lv_device = (ListView) findViewById(R.id.lv_device);
        btn_scanBle.setOnClickListener(this);
        btn_state.setOnClickListener(this);
    }
    
    private void initData(){
        deviceList = new ArrayList<>();
        connDeviceName = SPUtils.getString(SPUtils.DEVICE_NAME, null);
        connDeviceAddress = SPUtils.getString(SPUtils.DEVICE_ADDRESS, null);
        if(!TextUtils.isEmpty(connDeviceName) && !TextUtils.isEmpty(connDeviceAddress)){
            HashMap<String, Object> connDevMap = new HashMap<String, Object>();
            connDevMap.put("name", connDeviceName);
            connDevMap.put("address", connDeviceAddress);
            connDevMap.put("isConnect", false);
            deviceList.add(connDevMap);
            isHasDevice = true;
        }

        deviceAdapter = new CommonAdapter<Map<String, Object>>(
                this, R.layout.item_device, deviceList) {
            @Override
            public void convert(ViewHolder holder, final Map<String, Object> deviceMap) {
                holder.setText(R.id.tv_name, deviceMap.get("name").toString());
                holder.setText(R.id.tv_address, deviceMap.get("address").toString());
                holder.setText(R.id.tv_connState, ((boolean) deviceMap.get("isConnect")) ?
                        getString(R.string.state_connected) :
                        getString(R.string.state_disconnect));
            }
        };
        lv_device.setAdapter(deviceAdapter);
        lv_device.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_scanBle:
                if (!mBleService.isScanning()) {
                    isHasDevice = false;
                    verifyIfRequestPermission();
                    deviceList.clear();
                    mBleService.scanLeDevice(true);
                }
                break;
            case R.id.btn_state:
                if ((boolean) deviceList.get(mPosition).get("isConnect")) {
                    mBleService.disconnect();
                    showDialog(getString(R.string.disconnecting));
                    isManual = true;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mPosition = i;
        Map<String, Object> deviceMap = deviceList.get(i);
        if ((boolean) deviceMap.get("isConnect")) {
//            mBleService.disconnect();
//            showDialog(getString(R.string.disconnecting));
//            isManual = true;
        } else {
            isManual = false;

            connDeviceAddress = (String) deviceMap.get("address");
            connDeviceName = (String) deviceMap.get("name");

            SPUtils.putString(SPUtils.DEVICE_ADDRESS, connDeviceAddress);
            SPUtils.putString(SPUtils.DEVICE_NAME, connDeviceName);

            HashMap<String, Object> connDevMap = new HashMap<String, Object>();
            connDevMap.put("name", connDeviceName);
            connDevMap.put("address", connDeviceAddress);
            connDevMap.put("isConnect", false);
            deviceList.clear();
            deviceList.add(connDevMap);
            deviceAdapter.notifyDataSetChanged();
            mPosition = 0;
            mBleService.connect(connDeviceAddress);
            showDialog(getString(R.string.connecting));
        }
    }

    @Override
    public void onBackPressed() {
        if (mBleService.isScanning()) {
            mBleService.scanLeDevice(false);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        doUnBindService();
        unregisterReceiver(bleReceiver);
        super.onDestroy();
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
                showDialog(getResources().getString(R.string.scanning));
                mBleService.scanLeDevice(true);
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(MainActivity.this, "位置访问权限被拒绝将无法搜索到ble设备", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                Log.e(TAG, "name: " + tmpDevName + ", address: " + tmpDevAddress);
                if(isHasDevice){
                    if(!TextUtils.isEmpty(tmpDevAddress) && tmpDevAddress.equals(connDeviceAddress)) {
                        Log.e(TAG,"----------------------------扫描到设备，准备连接");
                        isScanDevice = true;
                        if (mBleService.isScanning()){
                            mBleService.scanLeDevice(false);
                        }
                    }
                }else {
                    HashMap<String, Object> deviceMap = new HashMap<>();
                    deviceMap.put("name", tmpDevName);
                    deviceMap.put("address", tmpDevAddress);
                    deviceMap.put("isConnect", false);
                    deviceList.add(deviceMap);
                    deviceAdapter.notifyDataSetChanged();
                }
            } else if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
                deviceList.get(mPosition).put("isConnect", true);
                deviceAdapter.notifyDataSetChanged();
                dismissDialog();
                btn_state.setVisibility(View.VISIBLE);
                btn_scanBle.setVisibility(View.GONE);
            } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
                deviceList.get(mPosition).put("isConnect", false);
                deviceAdapter.notifyDataSetChanged();
                dismissDialog();
                btn_state.setVisibility(View.GONE);
                btn_scanBle.setVisibility(View.VISIBLE);

                if(null!=mBleService && !mBleService.isConnect() && !isManual){
                    mBleService.connect(connDeviceAddress);
                }

            } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
                if(isHasDevice){
                    if(isScanDevice){
                        Log.e(TAG,"----------------------------扫描到设备，准备连接");
                        mBleService.connect(connDeviceAddress);
                    }else {
                        Log.e(TAG,"----------------------------没有扫描到设备，重新扫描");
                        mHandler.sendEmptyMessage(AUTO_CONNECT_DEVICE);
                    }
                }else {
                    btn_scanBle.setEnabled(true);
                    dismissDialog();
                }
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_BLUETOOTH_DEVICE);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_SCAN_FINISHED);
        return intentFilter;
    }

    /**
     * 绑定服务
     */
    private void bindService(){
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
                        verifyIfRequestPermission();
                        Toast.makeText(MainActivity.this, "Bluetooth was opened", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Not support Bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleService = null;
            mIsBind = false;
        }
    };

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
                if(!isHasDevice) {
                    showDialog(getResources().getString(R.string.scanning));
                }
                mBleService.scanLeDevice(true);
            }
        } else {
            if(!isHasDevice) {
                showDialog(getResources().getString(R.string.scanning));
            }
            mBleService.scanLeDevice(true);
        }
    }

    private void setBleServiceListener() {
        mBleService.setOnServicesDiscoveredListener(new BleService.OnServicesDiscoveredListener() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mBleService.setCharacteristicNotification(SERVICE_UUID, CHARACTERISTIC_UUID, true);
                    if(isHasDevice){
                        Log.e(TAG,"----------------------------自动连接已连接的设备成功");
                        isHasDevice = false;
                        isScanDevice = false;
                    }
                }
            }
        });
//        //Ble扫描回调
//        mBleService.setOnLeScanListener(new BleService.OnLeScanListener() {
//            @Override
//            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//                //每当扫描到一个Ble设备时就会返回，（扫描结果重复的库中已处理）
//
//            }
//        });
//        //Ble连接回调
//        mBleService.setOnConnectListener(new BleListener.OnConnectionStateChangeListener() {
//            @Override
//            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                    //Ble连接已断开
//                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
//                    //Ble正在连接
//                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    //Ble已连接
//
//                } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
//                    //Ble正在断开连接
//                }
//            }
//        });
//        //Ble服务发现回调
//        mBleService.setOnServicesDiscoveredListener(new BleService.OnServicesDiscoveredListener() {
//            @Override
//            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//
//            }
//        });
//        //Ble数据回调
//        mBleService.setOnDataAvailableListener(new BleService.OnDataAvailableListener() {
//            @Override
//            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                //处理特性读取返回的数据
//            }
//
//            @Override
//            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//                //处理通知返回的数据
//            }
//        @Override
//        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//
//        }
//        });

//        mBleService.setOnReadRemoteRssiListener(new BleService.OnReadRemoteRssiListener() {
//            @Override
//            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//                Log.i(TAG, "onReadRemoteRssi: rssi = " + rssi);
//            }
//        });
    }


    /**
     * Show dialog
     */
    private ProgressDialog progressDialog;

    private void showDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissDialog() {
        if (progressDialog == null) return;
        progressDialog.dismiss();
        progressDialog = null;
    }
}
