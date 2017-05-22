package com.hampoo.bleremotecontrol.utils;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by Administrator on 2016/12/16.
 */

public class BluetoothUtil {
    BluetoothAdapter mBluetoothAdapter;

    private BluetoothUtil() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private static BluetoothUtil instance = null;
    public static BluetoothUtil getInstance() {
        if (instance == null) {
            synchronized (BluetoothUtil.class) {
                if (instance == null) {
                    instance = new BluetoothUtil();
                }
            }
        }
        return instance;
    }

    public boolean isEnabled(){
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     */
    public void openBluetooth(){
        if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
    }

    /**
     * 关闭蓝牙
     */
    public void closeBluetooth(){
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
    }
}
