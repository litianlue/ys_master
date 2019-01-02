package com.yeespec.microscope.master.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Mr.Wen on 2016/9/21.
 */
public class AdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "test_AdminReceiver";

    @Override
    public DevicePolicyManager getManager(Context context) {

        return super.getManager(context);
    }

    @Override
    public ComponentName getWho(Context context) {
       // Log.w(TAG, "------" + "getWho" + "------");
        return super.getWho(context);
    }

    /**
     * 禁用
     */
    @Override
    public void onDisabled(Context context, Intent intent) {

//        Toast.makeText(context, "禁用设备管理", Toast.LENGTH_SHORT).show();

        super.onDisabled(context, intent);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        Log.w(TAG, "------" + "onDisableRequested" + "------");
        return super.onDisableRequested(context, intent);
    }

    /**
     * 激活
     */
    @Override
    public void onEnabled(Context context, Intent intent) {

//        Toast.makeText(context, "启动设备管理", Toast.LENGTH_SHORT).show();

        super.onEnabled(context, intent);
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        Log.w(TAG, "------" + "onPasswordChanged" + "------");
        super.onPasswordChanged(context, intent);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        Log.w(TAG, "------" + "onPasswordFailed" + "------");
        super.onPasswordFailed(context, intent);
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        Log.w(TAG, "------" + "onPasswordSucceeded" + "------");
        super.onPasswordSucceeded(context, intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "------" + "onReceive" + "------");

        super.onReceive(context, intent);
    }

    @Override
    public IBinder peekService(Context myContext, Intent service) {
        Log.w(TAG, "------" + "peekService" + "------");
        return super.peekService(myContext, service);
    }

}
