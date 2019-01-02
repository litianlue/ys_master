package com.yeespec.libuvccamera.uvccamera.serviceclient;

import android.os.Looper;
import android.util.Log;

/**
 * Created by Mr.Wen on 2016/8/5.
 *
 * @author Mr.Wen
 * @company YeeSpec
 * @time 2016/8/5 14:46
 */
////////////////////////////////////////////////////////
//此处实现aidl借口 IUVCServiceCallback.aidl
//********************************************************************************
//==========================================================================================
//内部类 , 重要的内部类 , 实现了抽象类Stub里面的方法 , 实现了Runnable接口 !
public class CameraTaskRunnable implements Runnable {

    private static final boolean DEBUG = false;
    private static final String TAG = "test_CameraTask";
    private static final String TAG_CAMERA = "CameraClientThread";

    private final Object mSync = new Object();
    public CameraClient mParent;
    private CameraHandler mHandler;
    private boolean mIsConnected;
    public int mServiceId;

    public CameraTaskRunnable(CameraClient parent) {
        mParent = parent;
    }

    public CameraHandler getHandler() {
        synchronized (mSync) {
            if (mHandler == null)
                try {
                    //object调用wait()方法 ,
            /*在调用sleep()方法的过程中，线程不会释放对象锁。
            而当调用wait()方法的时候，线程会放弃对象锁，
            进入等待此对象的等待锁定池，只有针对此对象
            调用notify()方法后本线程才进入对象锁定池准备
            */
                    mSync.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (DEBUG)
                        Log.e(TAG_CAMERA, "handleResize:", e);
                }
        }
        return mHandler;
    }

    @Override
    public void run() {
        if (DEBUG)
            Log.v(TAG_CAMERA, "run:");
        Looper.prepare();   //给线程创建消息循环
        synchronized (mSync) {
            mHandler = new CameraHandler(this);
            mSync.notifyAll();
        }
        /*写在Looper.loop()之后的代码不会被执行，这个函数内部应该是一个循环，
        当调用mHandler.getLooper().quit()后，loop才会中止，其后的代码才能得以运行。
        * */
        Looper.loop();  //让Looper开始工作，从消息队列里取消息，处理消息
        if (DEBUG)
            Log.v(TAG_CAMERA, "run:finising");
        synchronized (mSync) {
            mHandler = null;
            mParent = null;
            mSync.notifyAll();
        }
    }

}

