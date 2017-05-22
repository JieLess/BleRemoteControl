package com.hampoo.bleremotecontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.hampoo.bleremotecontrol.MainActivity;
import com.hampoo.bleremotecontrol.MyApplication;

public class SPUtils
{
    /**
     * 保存在手机里面的文件名
     */
    public static final String SP_NAME = "share_data";

    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_NAME = "DEVICE_NAME";

    private volatile static SharedPreferences mSP;

    private SPUtils() { }

    /**
     *
     * @param key
     * @param value
     *            a string value to share preference file
     */
    public static void putString(String key, String value) {
        getSharePreferences().edit().putString(key, value).commit();
    }

    /**
     *
     * @param key
     * @param defaultValue
     *            a string value from share preference file, ask a default value
     */
    public static String getString(String key, String defaultValue) {
        return getSharePreferences().getString(key, defaultValue);
    }

    /**
     * @param key
     * @return string value that store in shared preferences ,default is ""
     */
    public static String getString(String key) {
        return getSharePreferences().getString(key, "");
    }

    /**
     *
     * @param key
     * @param value
     *            a boolean value to share preference file
     */
    public static void putBoolean(String key, boolean value) {
        getSharePreferences().edit().putBoolean(key, value).commit();
    }

    /**
     *
     * @param key
     * @param defaultValue
     *            a boolean value from share preference file, ask a default
     *            value
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return getSharePreferences().getBoolean(key, defaultValue);
    }

    /**
     *
     * @param key
     * @param value
     *            a long value to share preference file
     */
    public static void putLong(String key, long value) {
        getSharePreferences().edit().putLong(key, value).commit();
    }

    /**
     *
     * @param key
     * @param defaultValue
     *            a long value from share preference file, ask a default value
     */
    public static long getLong(String key, long defaultValue) {
        return getSharePreferences().getLong(key, defaultValue);
    }

    /**
     *
     * @param key
     * @return boolean value that store in shared preferences ,default is false
     */
    public static boolean getBoolean(String key) {
        return getSharePreferences().getBoolean(key, false);
    }

    /**
     *
     * @param key
     * @param value
     *            a int value to share preference file
     */
    public static void putInt(String key, int value) {
        getSharePreferences().edit().putInt(key, value).commit();
    }

    /**
     *
     * @param key
     * @param defaultValue
     *            a int value from share preference file, ask a default value
     */
    public static int getInt(String key, int defaultValue) {
        return getSharePreferences().getInt(key, defaultValue);
    }

    private static SharedPreferences getSharePreferences() {
        if (mSP == null) {
            synchronized (SP_NAME) {
                if (mSP == null) {
                    mSP = MyApplication.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                }
            }
        }
        return mSP;
    }

    // =============================================================
    // ------------------------保存数据--------------------------------
    // ------------------ key===>键名 value===>保存的数据值 ----------------
    // =============================================================

    /**
     * 保存数据 根据数据类型自动拆箱
     *
     * @param key
     *            键名
     * @param obj
     *            Object类型数据 但仅限于(String/int/float/boolean/long)
     */
    public static void save(String key, Object obj) {
        SharedPreferences.Editor editor = getSharePreferences().edit();
        if (obj instanceof String)
            editor.putString(key, (String) obj);

        else if (obj instanceof Integer)
            editor.putInt(key, (Integer) obj);

        else if (obj instanceof Long)
            editor.putLong(key, (Long) obj);

        else if (obj instanceof Boolean)
            editor.putBoolean(key, (Boolean) obj);

        else if (obj instanceof Float)
            editor.putFloat(key, (Float) obj);

        editor.commit();
    }

    // =============================================================
    // ------------------------获取数据--------------------------------
    // ------ key===>键名 defaultValue===>根据key查找不到的默认数据的数据值 -------
    // =============================================================

    /**
     * 获取Object类型数据 根据接收类型自动拆箱
     *
     * @param key
     *            键名
     * @param defaultValue
     *            根据key获取不到是默认值仅限于(String/int/float/boolean/long)
     */
    public static Object get(String key, Object defaultValue) {
        SharedPreferences sp = getSharePreferences();
        if (defaultValue instanceof String)
            return sp.getString(key, (String) defaultValue);

        else if (defaultValue instanceof Integer)
            return sp.getInt(key, (Integer) defaultValue);

        else if (defaultValue instanceof Long)
            return sp.getLong(key, (Long) defaultValue);

        else if (defaultValue instanceof Boolean)
            return sp.getBoolean(key, (Boolean) defaultValue);

        else if (defaultValue instanceof Float)
            return sp.getFloat(key, (Float) defaultValue);

        return null;
    }
}

