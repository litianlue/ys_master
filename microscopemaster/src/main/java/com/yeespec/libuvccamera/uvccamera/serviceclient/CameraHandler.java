package com.yeespec.libuvccamera.uvccamera.serviceclient;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by Mr.Wen on 2016/8/5.
 *
 * @author Mr.Wen
 * @company YeeSpec
 * @time 2016/8/5 14:36
 */
//================================================================================
//内部类 , CameraHandler , 用于处理Handler信息 :
public class CameraHandler extends Handler {

    private static final boolean DEBUG = false;
    private static final String TAG = "test_CameraHandler";

    public static final int MSG_CONNECT = 1;
    public static final int MSG_DISCONNECT = 2;

    public static final int MSG_SATURATION_CONTROL = 11;
    public static final int MSG_CONTRAST_CONTROL = 12;
    public static final int MSG_GAMMA = 18;

    public static final int MSG_RELEASE = 99;

    public static CameraClient mParent;

    public static CameraHandler createHandler(CameraClient parent) {
        mParent = parent;
        //        mUsbManager = mUsbManager;
        final CameraTaskRunnable runnable = new CameraTaskRunnable(parent);
        new Thread(runnable).start();
        return runnable.getHandler();
    }

    private CameraTaskRunnable mCameraTaskRunnable;

    public CameraHandler(final CameraTaskRunnable cameraTaskRunnable) {
        mCameraTaskRunnable = cameraTaskRunnable;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {

            case MSG_RELEASE:
                //                mCameraTaskRunnable.handleRelease();
                mCameraTaskRunnable = null;
                Looper.myLooper().quit();
                break;
            default:
                throw new RuntimeException("unknown message:what=" + msg.what);
        }
    }



   /* public int getGamma() {
        return mGamma;
    }

    public int getContrast() {
        return mContrast;
    }

    public int getSaturation() {
        return mSaturation;
    }*/


}

