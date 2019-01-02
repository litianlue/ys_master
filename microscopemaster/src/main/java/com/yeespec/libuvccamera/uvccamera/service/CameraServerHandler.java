package com.yeespec.libuvccamera.uvccamera.service;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: CameraServerHandler.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.orhanobut.hawk.Hawk;
import com.yeespec.R;
import com.yeespec.libuvccamera.bluetooth.BluetoothActivity;
import com.yeespec.libuvccamera.usart.usbserial.driver.UsbSerialDriver;
import com.yeespec.libuvccamera.usart.usbserial.driver.UsbSerialPort;
import com.yeespec.libuvccamera.usart.usbserial.driver.UsbSerialProber;
import com.yeespec.libuvccamera.usart.usbserial.util.HexDump;
import com.yeespec.libuvccamera.usart.usbserial.util.SerialInputOutputManager;
import com.yeespec.libuvccamera.usb.IButtonCallback;
import com.yeespec.libuvccamera.usb.IFrameCallback;
import com.yeespec.libuvccamera.usb.IStatusCallback;
import com.yeespec.libuvccamera.usb.Size;
import com.yeespec.libuvccamera.usb.USBMonitor.UsbControlBlock;
import com.yeespec.libuvccamera.usb.UVCCamera;
import com.yeespec.libuvccamera.uvccamera.encoder.MediaAudioEncoder;
import com.yeespec.libuvccamera.uvccamera.encoder.MediaEncoderRunnable;
import com.yeespec.libuvccamera.uvccamera.encoder.MediaMuxerWrapper;
import com.yeespec.libuvccamera.uvccamera.encoder.MediaSurfaceEncoder;
import com.yeespec.libuvccamera.uvccamera.glutils.RenderHolderRunnable;

import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.MDateUtils;

import com.yeespec.microscope.utils.SPHelper;
import com.yeespec.microscope.utils.SPUtils;

import java.io.DataOutputStream;
import java.io.File;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static com.yeespec.libuvccamera.uvccamera.service.UVCService.EXECUTOR_SERVICE_SCHEDULED;
import static com.yeespec.libuvccamera.uvccamera.service.UVCService.restartAppState;


/**
 * 实现com.serenegiant.service的IUVCServiceOnFrameAvailable.aidl远程接口;
 */
public final class CameraServerHandler extends Handler {

    private static final boolean DEBUG = true;
    private static final String TAG = "test_CameraServer";

    public final static String ACTION_STATUS_CHANGED = "com.yeespec.action.STATUS_CHANGED";
    // public static final ScheduledExecutorService Executor_SERVICE_DELAY = Executors.newScheduledThreadPool(2);
    private static Context mContext;

    public static final boolean USE_USART = true;      //true为使用串口USART ; false为使用libusb
    public static boolean REMOTE_LOGIN = false;      //true为使用远程登录，false为局域网登录
    public static String userName;
    public static String password;
    public static int cgain = 0; //记录ISO值
    public static int cbrightness = 0;//记录亮度值
    public static int csaturation = 0;//记录激发块
    public static int ccontrast = 0;//记录物镜倍数值
    public static int crecolor = 0;//记录染色
    public static int cgamma = 0;//焦点值

    public static int phone_number = 0;//连接数量

    public static String recolorString;//染色名称
    public static String devicename = null;//
    public static int mProductId = 248;//
    private int mFrameWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH, mFrameHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;

    public void restartApp() {
        Hawk.put("port_exception", true);
       // FileUtils.writeFileToLogFolder("串口异常，restartApp");
        RestartUtil  restartUtil = new RestartUtil();
        restartUtil.killProcess();

    }
    private static class CallbackCookie {
        boolean isConnected;
    }

    private final RemoteCallbackList<IUVCServiceCallback> mCallbacks = new RemoteCallbackList<IUVCServiceCallback>();
    private int mRegisteredCallbackCount;
    private RenderHolderRunnable mRenderHolderRunnable;
    public final SoftReference<CameraThread> mWeakThread;

    private static IFrameCallback mIFrameCallback;

    public static int[] mobjects;

    public static CameraServerHandler createServerHandler(final Context context, final UsbControlBlock ctrlBlock, final int vid, final int pid, IFrameCallback iFrameCallback) {


        if (DEBUG)
            Log.d(TAG, "createServerHandler:");
        mIFrameCallback = iFrameCallback;
        //2016.09.18 : 新增 :
        mContext = context;

        mCtrlBlock = ctrlBlock;
        initObjects();
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        final CameraThread thread = new CameraThread(context, null);
        thread.start();
        return thread.getHandler();
    }

    public static int LightCount = 4;
    public static String LightType = "1";

    private static void initObjects() {
        File filedir = Environment.getExternalStorageDirectory().getAbsoluteFile();
        String s = FileUtils.getFileString(filedir + "", "developerset.txt");
        if (s != null && !s.equals("")) {
            String[] split = s.split("\n");
            if (split.length > 1) {
                String[] strings = split[0].split(",");
                LightCount = strings.length;
                LightType = split[0].toString();
                String[] mtimes = split[1].trim().split(",");
                if (mtimes.length > 1)
                    mobjects = new int[mtimes.length];
                for (int i = 0; i < mtimes.length; i++) {
                    int times = Integer.valueOf(mtimes[i]);
                    switch (times) {
                        case 5:
                            mobjects[i] = 5;

                            break;
                        case 10:
                            mobjects[i] = 10;
                            break;
                        case 20:
                            mobjects[i] = 20;
                            break;
                        case 50:
                            mobjects[i] = 50;
                            break;
                    }

                }
                Arrays.sort(mobjects);//排序

            } else {

                mobjects = new int[2];
                mobjects[0] = 5;
                mobjects[1] = 20;
            }
            if (split.length > 4) {
                userName = split[4].split("&")[0];
                password = split[4].split("&")[1];
            }
        } else {

            mobjects = new int[2];
            mobjects[0] = 5;
            mobjects[1] = 20;
        }
    }

    private CameraServerHandler(final CameraThread thread) {

        mWeakThread = new SoftReference<CameraThread>(thread);
        mRegisteredCallbackCount = 0;
        mRenderHolderRunnable = new RenderHolderRunnable(mFrameWidth, mFrameHeight, null);
    }

    @Override
    protected void finalize() throws Throwable {
        if (DEBUG)
            Log.i(TAG, "finalize:");
        release();
        super.finalize();
    }

    public void registerCallback(final IUVCServiceCallback callback) {
        if (DEBUG)
            Log.d(TAG, "registerCallback:");
        mCallbacks.register(callback, new CallbackCookie());
        mRegisteredCallbackCount++;
    }

    public boolean unregisterCallback(final IUVCServiceCallback callback) {

        mCallbacks.unregister(callback);
        mRegisteredCallbackCount--;
        if (mRegisteredCallbackCount < 0)
            mRegisteredCallbackCount = 0;
        return mRegisteredCallbackCount == 0;
    }

    public void release() {

        handleRelease();
        mCallbacks.kill();
        if (mRenderHolderRunnable != null) {
            mRenderHolderRunnable.release();
            mRenderHolderRunnable = null;
        }
    }

    //********************************************************************************
    //********************************************************************************
    public void resize(final int width, final int height) {
        if (DEBUG)
            Log.d(TAG, String.format("resize(%d,%d)", width, height));
        if (!isRecording()) {
            mFrameWidth = width;
            mFrameHeight = height;
            if (mRenderHolderRunnable != null) {
                mRenderHolderRunnable.resize(width, height);
            }
        }
    }

    public void connect() {

        if (!isCameraOpened()) {
            sendMessage(obtainMessage(MSG_OPEN));
            sendMessage(obtainMessage(MSG_PREVIEW_START, mFrameWidth, mFrameHeight, mRenderHolderRunnable.getSurface()));
        } else {
            if (DEBUG)
                Log.d(TAG, "already connected, just call callback");
            processOnCameraStart();
        }
    }

    //2016.09.14 : 新增 : 用于待机唤醒后重新设置并开始预览图像 :
    public void restartPreview(long delayMillis) {
        if (DEBUG)
            Log.d(TAG, "1:" + delayMillis);

        sendMessageDelayed(obtainMessage(MSG_RESTART_PREVIEW, mFrameWidth, mFrameHeight, mRenderHolderRunnable.getSurface()), delayMillis);


    }

    public void mrestartPreview(long delayMillis) {

        sendMessageDelayed(obtainMessage(MY_MSG_RESTART_PREVIEW, mFrameWidth, mFrameHeight, mRenderHolderRunnable.getSurface()), delayMillis);


    }


    public void disconnect() {
        if (DEBUG)
            Log.d(TAG, "disconnect:");
        processOnCameraStop();
        stopRecording();
        final CameraThread thread = mWeakThread.get();
        if (thread == null)
            return;
        synchronized (thread.mSync) {
            sendEmptyMessage(MSG_PREVIEW_STOP);
            sendEmptyMessage(MSG_CLOSE);
            // wait for actually preview stopped to avoid releasing Surface/SurfaceTexture  //等待其实预览停止以避免释放表面/表面
            // while preview is still running.  //当预览仍然运行。
            // therefore this method will take a time to execute    //因此，这种方法将需要一段时间来执行
            try {
                thread.mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return isCameraOpened();
    }

    public boolean isRecording() {

        return (mUVCCamera != null) && (mMuxer != null);
        //        return (thread != null) && thread.isRecording();
    }

    public void addSurface(final int id, final Surface surface, final boolean isRecordable, final IUVCServiceOnFrameAvailable onFrameAvailableListener) {
        if (DEBUG)
            Log.d(TAG, "addSurface:id=" + id + ",surface=" + surface);
        if (mRenderHolderRunnable != null)
            mRenderHolderRunnable.addSurface(id, surface, isRecordable, onFrameAvailableListener);
    }

    public void removeSurface(final int id) {
        if (DEBUG)
            Log.d(TAG, "removeSurface:id=" + id);
        if (mRenderHolderRunnable != null)
            mRenderHolderRunnable.removeSurface(id);
    }

    public void startRecording(int number) {
        if (!isRecording())
            sendMessage(obtainMessage(MSG_CAPTURE_START, number));
        //  sendEmptyMessage(MSG_CAPTURE_START);
    }

    public void stopRecording() {
        if (isRecording())
            sendEmptyMessage(MSG_CAPTURE_STOP);
    }

    public void captureStill(final String path) {
        if (mRenderHolderRunnable != null) {
            mRenderHolderRunnable.captureStill(path);
            sendMessage(obtainMessage(MSG_CAPTURE_STILL, path));
        }
    }

    //********************************************************************************
    private void processOnCameraStart() {
        if (DEBUG)
            Log.d(TAG, "processOnCameraStart:");
        try {
            final int n = mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                if (!((CallbackCookie) mCallbacks.getBroadcastCookie(i)).isConnected)
                    try {
                        mCallbacks.getBroadcastItem(i).onConnected();    //反射调用前台进程的连接方法 , 进行相应操作 ;
                        ((CallbackCookie) mCallbacks.getBroadcastCookie(i)).isConnected = true;
                    } catch (final Exception e) {
                        e.printStackTrace();

                    }
            }
            mCallbacks.finishBroadcast();
        } catch (final Exception e) {
            e.printStackTrace();
            // Log.w(TAG, e);
        }
    }

    private void processOnCameraStop() {
        if (DEBUG)
            Log.d(TAG, "processOnCameraStop:");
        final int n = mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            if (((CallbackCookie) mCallbacks.getBroadcastCookie(i)).isConnected)
                try {
                    mCallbacks.getBroadcastItem(i).onDisConnected();    //反射调用前台进程的断开连接方法 , 进行相应操作 ;
                    ((CallbackCookie) mCallbacks.getBroadcastCookie(i)).isConnected = false;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
        }
        mCallbacks.finishBroadcast();

        /* if (DEBUG)
            Log.d(TAG, "processOnCameraStop:");
        if(mCallbacks==null)
            return;
        try{
            final int n = mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++)
                if (((CallbackCookie) mCallbacks.getBroadcastCookie(i)).isConnected) {

                    mCallbacks.getBroadcastItem(i).onDisConnected();    //反射调用前台进程的断开连接方法 , 进行相应操作 ;

                    ((CallbackCookie) mCallbacks.getBroadcastCookie(i)).isConnected = false;

                }
        }catch (IllegalArgumentException e) {

        } catch (RemoteException e) {
            e.printStackTrace();
        }finally {
            mCallbacks.finishBroadcast();
        }
        * */
    }

    //**********************************************************************
    private static final int MSG_OPEN = 0;
    private static final int MSG_CLOSE = 1;
    private static final int MSG_PREVIEW_START = 2;
    private static final int MSG_PREVIEW_STOP = 3;
    private static final int MSG_CAPTURE_STILL = 4;
    private static final int MSG_CAPTURE_START = 5;
    private static final int MSG_CAPTURE_STOP = 6;
    private static final int MSG_MEDIA_UPDATE = 7;
    private static final int MSG_RELEASE = 9;
    private static final int MSG_RELEASE_SWITH = 29;
    private static final int MSG_RETIMERFALG = 59;
    private static final int MESSAGE_STOP_ELECTRIC = 100;
    // 亮度
    public static final int MSG_BRIGHTNESS_CONTROL = 10;
    // 灯
    public static final int MSG_SATURATION_CONTROL = 11;
    // 物焦
    public static final int MSG_CONTRAST_CONTROL = 12;
    // ISO
    public static final int MSG_GAIN_CONTROL = 13;
    // 是否自动对焦
    public static final int MSG_IS_AUTO_FOCUS = 14;
    // 设置焦点
    public static final int MSG_FOCUS_CONTROL = 15;
    // 手动对焦
    public static final int MSG_GAMMA_CONTROL = 16;

    public static final int MSG_SET_COLOR = 17;

    //2016.09.14 : 新增 : 用于待机唤醒后重新设置并开始预览图像 :
    public static final int MSG_RESTART_PREVIEW = 18;
    public static final int MY_MSG_RESTART_PREVIEW = 28;
    //2016.09.19 : 新增 : 用于唤醒手机屏幕 :
    public static final int MSG_SCREEN_WALKUP = 19;
    private boolean isconnect = false;
    private int  USB_IS_REMMOVE_COUNT = 0;

    @Override
    public void handleMessage(final Message msg) {

        switch (msg.what) {
            case MSG_OPEN:
//                EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
//                    @Override
//                    public void run() {
//                        restartApp();
//                    }
//                }, 150 * 1000, TimeUnit.MILLISECONDS);
                handleOpenUVCCamera();
                handleConnectUSART();
                break;
            case MSG_CLOSE:
                handleCloseUVCCamera();
                handleDisconnectUSART();
                break;
            case MSG_PREVIEW_START:
                handleStartPreview(msg.arg1, msg.arg2, (Surface) msg.obj);
                break;
            case MSG_PREVIEW_STOP:
                handleStopPreview();
                break;
            case MSG_CAPTURE_STILL:
                handleCaptureStill((String) msg.obj);
                break;
            case MSG_CAPTURE_START:
                handleStartRecording((int) msg.obj);
                break;
            case MSG_CAPTURE_STOP:
                handleStopRecording();
                break;
            case MSG_MEDIA_UPDATE:
                handleUpdateMedia((String) msg.obj);
                break;
            case MSG_RELEASE:
                handleRelease();
                break;
            case MSG_BRIGHTNESS_CONTROL:
                handleBrightness((Integer) msg.obj);
                break;
            case MSG_CONTRAST_CONTROL:
                handleContrast((Integer) msg.obj);
                break;
            case MSG_SATURATION_CONTROL:
                handleSaturation((Integer) msg.obj);
                break;
            case MSG_GAIN_CONTROL:
                handleGain((Integer) msg.obj);
                break;
            case MSG_FOCUS_CONTROL:
                handleFocus((Integer) msg.obj);
                break;
            case MSG_IS_AUTO_FOCUS:
                handleAutoFocus((Boolean) msg.obj);
                break;
            case MSG_GAMMA_CONTROL:
                handleGamma((Integer) msg.obj);
                break;
            case MSG_SET_COLOR:
                handleColor((Integer) msg.obj);
                break;

            //2016.09.14 : 新增 : 用于待机唤醒后重新设置并开始预览图像 :
            case MSG_RESTART_PREVIEW:
                handleRestartPreview(msg.arg1, msg.arg2, (Surface) msg.obj);
                break;
            case MY_MSG_RESTART_PREVIEW:
                mhandleRestartPreview(msg.arg1, msg.arg2, (Surface) msg.obj);
                break;
            //2016.09.19 : 新增 : 用于唤醒手机屏幕 :
            case MSG_SCREEN_WALKUP:


                break;
            case MESSAGE_IS_CONNECT:
                isconnect = true;
                // Log.e("CameraClient","isconnect="+isconnect);

                break;

            //2016.12.16 : 新增 : 用于刷新检测串口设备 :
            case MESSAGE_REFRESH:
                if (!IS_USART_CONNECT) {
                    refreshDeviceList();
                   // UART_CONNECT_REFRESH --;
                    Log.w("devicelist", "IS_USART_CONNECT=" + IS_USART_CONNECT);
                }

                if(IS_USART_CONNECT){
                    USB_IS_REMMOVE_COUNT++;
                    //定时检测串口掉线
                    if(USB_IS_REMMOVE_COUNT>5){
                        restartAppState=2;
                        FileUtils.writeFileToLogFolder("检测到串口掉线(超时没有信号返回)");
                        restartApp();
                    }
                }
                //2017.01.11 : 添加自动刷新控制板状态的操作 :
                requestCallBackStatus();

                sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                break;
            case MSG_RELEASE_SWITH:

                objectiveIsSwith = false;//释放串口发送限制开关
                break;
            case MSG_RETIMERFALG:
                isReturnTime = false;
                isdebug = true;
                break;
            case MESSAGE_STOP_ELECTRIC:
                stopElectricFocus();
                break;
            default:
                throw new RuntimeException("unsupported message:what=" + msg.what);
        }
    }


    public int getBrightness() {
        if (mUVCCamera != null)
            return mUVCCamera.getBrightness();
        return 0;
    }

    public float getVoltageLevel() {
        return mVoltageLevel;
    }

    public int getMaxBrightness() {

        if (mUVCCamera != null)
            return mUVCCamera.getMaxBrightness();
        return 0;
    }

    public int getMaxNuberType(int type) {

        if (mUVCCamera != null) {
            int iso = mUVCCamera.getMaxNuberType(type);
            if (iso % 2 != 0)
                iso += 1;
            return iso;
        }
        return 0;
    }

    public int getGain() {
        if (mUVCCamera != null)
            return mUVCCamera.getGain();
        return 0;
    }

    public int getFocus() {
        if (mUVCCamera != null)
            return mUVCCamera.getFocus();
        return 0;
    }

    public int getSaturation() {    //激发块电机
        if (USE_USART) {
            return mSaturation;
        } else {
            if (mUVCCamera != null)
                return mUVCCamera.getSaturation();
            return 0;
        }
    }

    public int getSaturationState() {

        int result = 0;
        if (objectiveIsSwith) {

        } else {
            requestCallBackStatus();
        }
     /*   Log.w(TAG,"LED1_ON_FLAG="+LED1_ON_FLAG);
        Log.w(TAG,"LED2_ON_FLAG="+LED2_ON_FLAG);
        Log.w(TAG,"LED3_ON_FLAG="+LED3_ON_FLAG);
        Log.w(TAG,"LED4_ON_FLAG="+LED4_ON_FLAG);*/
       /* if(ConstantUtil.LONZA){
            return csaturation;
        }*/
        if (LED1_ON_FLAG) {
            result = 1;
        } else if (LED2_ON_FLAG) {
            result = 2;
        } else if (LED3_ON_FLAG) {
            result = 3;
        } else if (LED4_ON_FLAG) {
            result = 4;
        }

        return result;
    }

    public int getContrast() {      //物镜电机
        if (USE_USART) {
            return mContrast;
        } else {
            if (mUVCCamera != null)
                return mUVCCamera.getContrast();
            return 0;
        }
    }

    public int getGamma() {     //对焦电机
        if (USE_USART) {
            requestCallBackStatus();
            return mGamma;
        } else {
            if (mUVCCamera != null)
                return mUVCCamera.getGamma();
            return 0;
        }
    }

    public void msetGamma(int gray) {     //设置过滤灰度值
        if (mUVCCamera != null) {
            mUVCCamera.setGamma(gray);
        }
    }
    //=================================== 华丽丽的分割线 ===========================================
    /**
     * for accessing UVC
     * camera
     */
    private volatile UVCCamera mUVCCamera;
    private final Object mSync = new Object();
    private static UsbControlBlock mCtrlBlock;
    private MediaMuxerWrapper mMuxer;
    private boolean mIsRecording;
    /**
     * shutter sound
     */
    private SoundPool mSoundPool;
    private int mSoundId;

    private long mCount = 0;   //2016.07.07 : 用于onFrameAvailable调用次数的 计数 :

    public MediaSurfaceEncoder mVideoEncoder;

    private int mEncoderSurfaceId;

    public void handleOpenUVCCamera() {
        if (DEBUG)
            Log.d(TAG, "handleOpenUVCCamera:");
        handleCloseUVCCamera();
        if (mCtrlBlock != null) {
            synchronized (mSync) {
                mUVCCamera = new UVCCamera();
                mUVCCamera.open(mCtrlBlock);
                mUVCCamera.updateCameraParams();

                //----------------重要分析步骤 1 :
                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX/*UVCCamera.PIXEL_FORMAT_NV21*/);
                //                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
                //                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565);

                mUVCCamera.setStatusCallback(new IStatusCallback() {
                    @Override
                    public void onStatus(final int statusClass, final int event, final int selector, final int statusAttribute, final ByteBuffer data) {


                    }
                });

                mUVCCamera.setButtonCallback(new IButtonCallback() {
                    @Override
                    public void onButton(final int button, final int state) {
                        // Log.e(TAG, "onButton(button=" + button + "; " + "state=" + state + ")");
                    }
                });

            }
            processOnCameraStart();
        }
    }

    public void rhandleCloseUVCCamera() {

        // handleStopRecording();

        synchronized (mSync) {
            if (mUVCCamera != null) {

                // mUVCCamera.stopPreview();

                mUVCCamera.reLeaseCamera();
                mUVCCamera.destroy();
                mUVCCamera = null;

            }
            mSync.notifyAll();
        }

    }

    public void rhandleOpenUVCCamera() {

        if (mCtrlBlock != null) {
            synchronized (mSync) {

                mUVCCamera = new UVCCamera();

                mUVCCamera.open(mCtrlBlock);
                mUVCCamera.updateCameraParams();

                //----------------重要分析步骤 1 :
                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX/*UVCCamera.PIXEL_FORMAT_NV21*/);

                mUVCCamera.setStatusCallback(new IStatusCallback() {
                    @Override
                    public void onStatus(final int statusClass, final int event, final int selector, final int statusAttribute, final ByteBuffer data) {

                    }
                });

                mUVCCamera.setButtonCallback(new IButtonCallback() {
                    @Override
                    public void onButton(final int button, final int state) {

                    }
                });

                mSync.notifyAll();
                // Log.e("CameraServiceHandlser","openUVC++++++++");
            }
            handleStartPreview(mFrameWidth, mFrameHeight, mRenderHolderRunnable.getSurface());

        }
    }

    public void handleCloseUVCCamera() {

        handleStopRecording();
        boolean closed = false;
        synchronized (mSync) {
            if (mUVCCamera != null) {

                mUVCCamera.stopPreview();
                mUVCCamera.reLeaseCamera();
                mUVCCamera.destroy();
                mUVCCamera = null;
                closed = true;
            }
            mSync.notifyAll();
        }
        if (IS_USART_CONNECT) {
            rhandleCloseUVCCamera();
/*
            Log.w("test_CameraServer","数据线异常");
            Log.w("test_CameraServer","usbMannager="+mUsbManager);
            Log.w("test_CameraServer","usbMannager="+mSerialIoManager);
            Log.w("test_CameraServer","usbMannager="+mEntries.size());*/
            if (mUsbManager == null) {
                mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
            }
            sendEmptyMessage(MESSAGE_REFRESH);
            rhandleOpenUVCCamera();
        }
        if (closed)
            processOnCameraStop();
        if (DEBUG)
            Log.d(TAG, "handleCloseUVCCamera:finished");
    }

    public void handleStopRecording() {
        if (DEBUG)
            Log.d(TAG, "handleStopRecording:mMuxer=" + mMuxer);
        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            // you should not wait here
        }
    }

    public void handleRelease() {
        if (DEBUG)
            Log.d(TAG, "handleRelease:");
        handleCloseUVCCamera();
        if (mCtrlBlock != null) {
            mCtrlBlock.close();
            mCtrlBlock = null;
        }
        if (!mIsRecording) {
            //    Looper.myLooper().quit();
        }
    }

    public void handleUpdateMedia(final String path) {
        if (DEBUG)
            Log.d(TAG, "handleUpdateMedia:path=" + path);
        final Context context = mContext;
        if (context != null) {
            try {

                MediaScannerConnection.scanFile(context, new String[]{path}, null, null);
            } catch (final Exception e) {
                e.printStackTrace();

            }
        } else {
            // Log.w(TAG, "TestMainActivity already destroyed");

            handleRelease();
        }
    }

    public boolean isCameraOpened() {
        return mUVCCamera != null;
    }

    public void handleBrightness(int brightness_abs) {
        if (DEBUG)
            Log.d(TAG, "TCPServer " + "handleBrightness(" + String.valueOf(mUVCCamera != null) + "):" + brightness_abs);
        if (mUVCCamera != null) {
            // SPUtils.put(BaseApplication.getContext(), "brightness", brightness_abs);
            if (csaturation == 0) {
                mUVCCamera.setBrightness(0);
            }
            else
                mUVCCamera.setBrightness(brightness_abs);
            // BaseApplication.getInstance().setBrightness(brightness_abs);
            CameraServerHandler.cbrightness = brightness_abs;
            cbrightness = brightness_abs;
        }
    }

    public void handleColor(int color) {
        if (mUVCCamera != null) {
            mUVCCamera.setColor(color);
        }
        //BaseApplication.getInstance().setRecolor(color);
        CameraServerHandler.crecolor = color;
        crecolor = color;
    }

    public void handleSaturation(int saturation_abs) {

        if (DEBUG)
            Log.d(TAG, "TCPServer- " + "handleSaturation(" + String.valueOf(mUVCCamera != null) + "):" + saturation_abs);
        if (USE_USART) {

            handleSaturationUSART(saturation_abs);
        } else {
            if (mUVCCamera != null) {
                //                SPUtils.put(BaseApplication.getContext(), "saturation", saturation_abs);
                mUVCCamera.setSaturation(saturation_abs);
            }
        }
        // SPUtils.put(BaseApplication.getContext(), "saturation", saturation_abs);
        // BaseApplication.getInstance().setSaturation(saturation_abs);
        CameraServerHandler.csaturation = saturation_abs;
        csaturation = saturation_abs;
      /*  if (csaturation == 0) {
            if (mUVCCamera != null) {
                //  20180320 如果是关灯，把亮度调节到最高降低帧率
                mUVCCamera.setBrightness(getMaxBrightness());
            }
        }*/
    }

    public void handleContrast(int contrast_abs) {
        if (DEBUG)
            Log.d(TAG, "TCPServer " + "handleContrast(" + String.valueOf(mUVCCamera != null) + "):" + contrast_abs);
        if (USE_USART) {
            handleContrastUSART(contrast_abs);
        } else {
            if (mUVCCamera != null) {
                //                SPUtils.put(BaseApplication.getContext(), "contrast", contrast_abs);
                mUVCCamera.setContrast(contrast_abs);
            }
        }

        // BaseApplication.getInstance().setContrast(contrast_abs);
        CameraServerHandler.ccontrast = contrast_abs;
        if (contrast_abs == 5 || contrast_abs == 10 || contrast_abs == 20 || contrast_abs == 50)
            ccontrast = contrast_abs;
    }

    public void handleGamma(int gamma_abs) {
        if (DEBUG)
            Log.d(TAG, "TCPServer- " + "handleFocus(" + String.valueOf(mUVCCamera != null) + "):" + gamma_abs);
        if (USE_USART) {
            // Log.w(TAG, "CameraServerHandler#handleGamma() === " + gamma_abs);
            handleGammaUSART(gamma_abs);
        } else {
            if (mUVCCamera != null) {
                //                SPUtils.put(BaseApplication.getContext(), "focus", gamma_abs);
                mUVCCamera.setGamma(gamma_abs);
            }
        }
        // SPUtils.put(BaseApplication.getContext(), "focus", gamma_abs);

    }

    public void handleGain(int gain_abs) {
        if (DEBUG)
            Log.d(TAG, "TCPServer- " + "handleGain(" + String.valueOf(mUVCCamera != null) + "):" + gain_abs);
        if (mUVCCamera != null) {

            int maxiso = getMaxNuberType(2);
            if ((gain_abs - 8) == maxiso) {
                if (gain_abs == 0)
                    mUVCCamera.setGain(8);
                else
                    mUVCCamera.setGain(gain_abs - 8);
            } else {
                if (gain_abs == 0)
                    mUVCCamera.setGain(8);
                else
                    mUVCCamera.setGain(gain_abs);
            }

            CameraServerHandler.cgain = gain_abs;
            cgain = gain_abs;
        }
    }

    public void handleFocus(int focus_abs) {
        if (DEBUG)
            Log.d(TAG, "TCPServer- " + "handleFocus(" + String.valueOf(mUVCCamera != null) + "):" + focus_abs);
        if (mUVCCamera != null) {
            SPUtils.put(BaseApplication.getContext(), "focus", focus_abs);
            mUVCCamera.setFocus(focus_abs);
        }
    }

    public void handleAutoFocus(boolean is_auto_focus) {
        if (DEBUG)
            Log.d(TAG, "TCPServer- " + "handleAutoFocus(" + String.valueOf(mUVCCamera != null) + "):" + is_auto_focus);
        if (mUVCCamera != null) {
            SPUtils.put(BaseApplication.getContext(), "is_auto_focus", is_auto_focus);
            mUVCCamera.setAutoFocus(is_auto_focus);
        }
    }


    private void handleResize(final int width, final int height, final Surface surface) {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                final Size sz = mUVCCamera.getPreviewSize();
                if ((sz != null) && ((width != sz.width) || (height != sz.height))) {
                    mUVCCamera.stopPreview();   //为被调用执行 ;
                    try {
                        mUVCCamera.setPreviewSize(width, height);
                    } catch (final IllegalArgumentException e) {
                        e.printStackTrace();
                        try {
                            mUVCCamera.setPreviewSize(sz.width, sz.height);
                        } catch (final IllegalArgumentException e1) {
                            e1.printStackTrace();
                            // unexpectly #setPreviewSize failed
                            mUVCCamera.destroy();
                            mUVCCamera = null;
                        }
                    }
                    if (mUVCCamera == null)
                        return;
                    mFrameWidth = width;
                    mFrameHeight = height;
                    mUVCCamera.setPreviewDisplay(surface);
                    mUVCCamera.startPreview();      //为被调用执行 ;
                }
            }
        }
    }

    public void handleCaptureStill(final String path) {
        if (DEBUG)
            Log.d(TAG, "handleCaptureStill:");

        mSoundPool.play(mSoundId, 0.2f, 0.2f, 0, 0, 1.0f);    // play shutter sound
    }

    //20180130
    public void handleStartPreview(final int width, final int height, final Surface surface) {
        if (DEBUG)
            Log.d(TAG, "handleStartPreview:");
        synchronized (mSync) {
            if (mUVCCamera == null)
                return;
            try {
                //                mUVCCamera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_MJPEG);
                mUVCCamera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_YUYV);
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
                try {
                    // fallback to YUV mode
                    mUVCCamera.setPreviewSize(width, height, UVCCamera.DEFAULT_PREVIEW_MODE);
                } catch (final IllegalArgumentException e1) {
                    //20180130
                    rhandleCloseUVCCamera();
                    //    rhandleOpenUVCCamera();
                    e1.printStackTrace();
                    //20180130
                    //mUVCCamera.destroy();
                    //mUVCCamera = null;
                }
            }
            if (mUVCCamera == null)
                return;
            mFrameWidth = width;
            mFrameHeight = height;

            //2016.09.14 : 添加设置图像帧回调 , 因为调用stopPreview()后把帧回调对象清空了 ;重新预览时需要重装该对象 ;
            mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX/*UVCCamera.PIXEL_FORMAT_NV21*/);

            //2016.08.24 : surface从这里传入底层
            mUVCCamera.setPreviewDisplay(surface);

            mUVCCamera.startPreview();      //开始界面图像预览; 预览主要调用处 !
        }
    }

    public void handleStopPreview() {
        if (DEBUG)
            Log.d(TAG, "handleStopPreview:");
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();   //停止当前的预览 ;
            }
        }
    }

    //2016.09.14 : 新增 : 用于待机唤醒后重新设置并开始预览图像 :
    private void handleRestartPreview(final int width, final int height, final Surface surface) {

        synchronized (mSync) {


            if (mUVCCamera != null) {

                if (width > 0 & height > 0 & mIFrameCallback != null) {
                    //20170515注释
                    // mUVCCamera.stopPreview();   //被调用执行 ;
                    try {
                        mUVCCamera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_MJPEG);
                        //  mUVCCamera.setPreviewSize(width, height);
                    } catch (final IllegalArgumentException e) {

                        e.printStackTrace();
                        try {

                            mUVCCamera.setPreviewSize(width, height, UVCCamera.DEFAULT_PREVIEW_MODE);
                            //                            mUVCCamera.setPreviewSize(width, height);
                        } catch (final IllegalArgumentException e1) {
                            e1.printStackTrace();

                            mUVCCamera.destroy();
                            mUVCCamera = null;
                        }
                    }

                    if (mUVCCamera == null)
                        return;
                    mFrameWidth = width;
                    mFrameHeight = height;

                    //2016.09.14 : 添加设置图像帧回调 , 因为调用stopPreview()后把帧回调对象清空了 ;重新预览时需要重装该对象 ;
                    mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX/*UVCCamera.PIXEL_FORMAT_NV21*/);

                    mUVCCamera.setPreviewDisplay(surface);
                    mUVCCamera.startPreview();      //被调用执行 ;

                    // Log.i("test_CamSerHandle", " handleRestartPreview() width = " + width + " , height = " + height + " === ");
                } else {
                    Log.i("test_CamSerHandle", " handleRestartPreview() ERROR ERROR width = " + width + " , height = " + height + " , mIFrameCallback = " + mIFrameCallback + "=== ");
                }
            }
        }
    }

    private void mhandleRestartPreview(final int width, final int height, final Surface surface) {

        synchronized (mSync) {

            if (mUVCCamera != null) {

                if (width > 0 & height > 0 & mIFrameCallback != null) {
                    //20170515注释
                    mUVCCamera.stopPreview();   //被调用执行 ;

                   /* if (mUVCCamera == null)
                        return;
                    mFrameWidth = width;
                    mFrameHeight = height;

                    //2016.09.14 : 添加设置图像帧回调 , 因为调用stopPreview()后把帧回调对象清空了 ;重新预览时需要重装该对象 ;
                    mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX*//*UVCCamera.PIXEL_FORMAT_NV21*//*);

                    mUVCCamera.setPreviewDisplay(surface);
                    mUVCCamera.startPreview();   //被调用执行 ;*/

                    // Log.i("test_CamSerHandle", " handleRestartPreview() width = " + width + " , height = " + height + " === ");
                } else {
                    Log.i("test_CamSerHandle", " handleRestartPreview() ERROR ERROR width = " + width + " , height = " + height + " , mIFrameCallback = " + mIFrameCallback + "=== ");
                }
            }
        }
    }

    private String numberString(int number) {
        int filenumber = number + 1;
        if (filenumber < 10) {
            return "00000" + filenumber;
        } else if (filenumber < 100) {
            return "0000" + filenumber;
        } else if (filenumber < 1000) {
            return "000" + filenumber;
        } else {
            return "00" + filenumber;
        }
    }

    //视频录制的 StartRecording开始代码 : 最终调用下面的方法 :
    public void handleStartRecording(int filenumber) {
        if (DEBUG)
            Log.d(TAG, "handleStartRecording:");
        mCount = 0;

        try {
            if ((mUVCCamera == null) || (mMuxer != null))
                return;
            String timerstr = MDateUtils.getDateTimeString();

            timerstr = numberString(filenumber) + "-" + timerstr;
            mMuxer = new MediaMuxerWrapper(timerstr + ".mp4");    // if you record audio only, ".m4a" is also OK.
            new MediaSurfaceEncoder(mFrameWidth, mFrameHeight, mMuxer, mMediaEncoderListener);
            if (true) {
                // for audio capturing
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
            }
            mMuxer.prepare();

            mMuxer.startRecording();
        } catch (final IOException e) {
            e.printStackTrace();

        }
    }

    //此处实现aidl借口 IUVCServiceOnFrameAvailable.aidl
    //********************************************************************************
    private final IUVCServiceOnFrameAvailable mOnFrameAvailable = new IUVCServiceOnFrameAvailable() {
        @Override
        public IBinder asBinder() {
            if (DEBUG)
                Log.d(TAG, "asBinder:");
            return null;
        }

        @Override
        public void onFrameAvailable() throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "onFrameAvailable: " + (mCount++) + " === ");
            if (mVideoEncoder != null)
                mVideoEncoder.frameAvailableSoon();
        }
    };

    private final MediaEncoderRunnable.MediaEncoderListener mMediaEncoderListener = new MediaEncoderRunnable.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoderRunnable encoder) {
            if (DEBUG)
                Log.d(TAG, "onPrepared:encoder=" + encoder);
            mIsRecording = true;
            if (encoder instanceof MediaSurfaceEncoder)
                try {
                    mVideoEncoder = (MediaSurfaceEncoder) encoder;
                    final Surface encoderSurface = mVideoEncoder.getInputSurface();
                    if (encoderSurface != null) {
                        mEncoderSurfaceId = encoderSurface.hashCode();
                        mRenderHolderRunnable.addSurface(mEncoderSurfaceId, encoderSurface, true, mOnFrameAvailable);

                    }
                } catch (final Exception e) {
                    e.printStackTrace();

                }
        }

        @Override
        public void onStopped(final MediaEncoderRunnable encoder) {
            if (DEBUG)
                Log.v(TAG, "onStopped:encoder=" + encoder);
            if ((encoder instanceof MediaSurfaceEncoder))
                try {
                    mIsRecording = false;
                    final Context parent = mContext;
                    if (mEncoderSurfaceId > 0)
                        mRenderHolderRunnable.removeSurface(mEncoderSurfaceId);
                    mEncoderSurfaceId = -1;
                    //2016.08.24 : 这里停止向底层传递surface ;
                    mUVCCamera.stopCapture();

                    mVideoEncoder = null;
                    final String path = encoder.getOutputPath();
                    if (!TextUtils.isEmpty(path)) {
                        sendMessage(obtainMessage(MSG_MEDIA_UPDATE, path));
                    }
                } catch (final Exception e) {
                    e.printStackTrace();

                }
        }
    };

    //=================================== 华丽丽的分割线 ===========================================
    public static final class CameraThread extends Thread {
        private static final String TAG_THREAD = "testCameraThread";

        // shutter sound
        private SoundPool mSoundPool;
        private int mSoundId;
        private CameraServerHandler mHandler;

        private final Object mSync = new Object();

        private CameraThread(final Context context, final UsbControlBlock ctrlBlock) {
            super("CameraThread");
            if (DEBUG)
                Log.d(TAG_THREAD, "Constructor:");
            //            this.mWeakContext = new WeakReference<Context>(context);
            //            this.mCtrlBlock = ctrlBlock;
            loadSutterSound(context);
        }

        @Override
        protected void finalize() {
            Log.i(TAG_THREAD, "CameraThread#finalize");
            try {
                super.finalize();   //告诉垃圾回收器应该执行的操作;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        public CameraServerHandler getHandler() {
            if (DEBUG)
                Log.d(TAG_THREAD, "getHandler:");
            synchronized (mSync) {
                if (mHandler == null)
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
            }
            return mHandler;
        }

        /**
         * prepare and load shutter sound for still image capturing
         */
        @SuppressWarnings("deprecation")
        private void loadSutterSound(final Context context) {
            if (DEBUG)
                Log.d(TAG_THREAD, "loadSutterSound:");
            // get system stream type using refrection
            int streamType;
            try {
                final Class<?> audioSystemClass = Class.forName("android.media.AudioSystem");
                final Field sseField = audioSystemClass.getDeclaredField("STREAM_SYSTEM_ENFORCED");
                streamType = sseField.getInt(null);
            } catch (final Exception e) {
                e.printStackTrace();
                streamType = AudioManager.STREAM_SYSTEM;    // set appropriate according to your app policy
            }
            if (mSoundPool != null) {
                try {
                    mSoundPool.release();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                mSoundPool = null;
            }
            // load sutter sound from resource
            mSoundPool = new SoundPool(2, streamType, 0);
            mSoundId = mSoundPool.load(context, R.raw.camera_click, 1);
        }

        @Override
        public void run() {
            if (DEBUG)
                Log.d(TAG_THREAD, "run:");

            Looper.prepare();   //给线程创建消息循环

            synchronized (mSync) {

                mHandler = new CameraServerHandler(this);
                mHandler.sendEmptyMessage(MESSAGE_REFRESH);

                mSync.notifyAll();
            }
             /*写在Looper.loop()之后的代码不会被执行，这个函数内部应该是一个循环，
            当调用mHandler.getLooper().quit()后，loop才会中止，其后的代码才能得以运行。
            * */
            Looper.loop();  //让Looper开始工作，从消息队列里取消息，处理消息
            synchronized (mSync) {
                mHandler = null;
                mSoundPool.release();
                mSoundPool = null;
                mSync.notifyAll();
            }

        }
    }
    //=================================== 华丽丽的分割线 ===========================================

    //============================================================

    public static final int MESSAGE_IS_CONNECT = 102;//
    public static final int MESSAGE_REFRESH = 101;
    public static final long REFRESH_TIMEOUT_MILLIS = 1000*60;

    private List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();
    private static UsbManager mUsbManager;

    private static UsbSerialPort sPort = null;
    private SerialInputOutputManager mSerialIoManager;

    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    //创建一个可重用固定线程数的线程池
    //   public static final ExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    // public static final ScheduledExecutorService DELAY_START = Executors.newScheduledThreadPool(1);
    public static int mGamma = 50;      //对焦电机
    public static int mSaturation = 50; //激发块电机
    public static int mContrast = 50;   //物镜电机
    private boolean isPostExecute = false;//USB串口是否加载完成

    private void refreshDeviceList() {

        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            @Override
            protected List<UsbSerialPort> doInBackground(Void... params) {

                SystemClock.sleep(2000);
                Log.w("mytest........", " SystemClock.sleep(2000)");
                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();

                if (mUsbManager != null) {

                    final List<UsbSerialDriver> drivers =
                            UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

                    for (final UsbSerialDriver driver : drivers) {

                        final List<UsbSerialPort> ports = driver.getPorts();

                        for (int i = 0; i < ports.size(); i++) {
//                            Log.d(TAG, String.format("+ %s: %s port%s",
//                                    driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
                            result.add(ports.get(i));
                          //  Log.w("mytest........", "ports" + ports);
                        }


                    }
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<UsbSerialPort> result) {


                mEntries.clear();
                mEntries.addAll(result);

                for (int i = 0; i < mEntries.size(); i++) {
                    FileUtils.writeFileToLogFolder("找到的USB设备:" + mEntries.get(i));
//                    Log.w("mytest........", "mEntries.size=" + mEntries.size());
//                    Log.w("mytest........", "mEntries(" + i + ")" + mEntries.get(i));
                }
                isPostExecute = true;
                for (int i = 0; i < mEntries.size(); i++) {
                    if (!IS_USART_CONNECT) {

                        if (mEntries != null & mEntries.size() > 0) {

                            sPort = mEntries.get(i);

                            initializeDriver(sPort);

                        } else {

                        }
                    }
                }


            }
        }.execute((Void) null);
    }

    private boolean callBackCount = false;
    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {

            // TODO: 2016/12/9 : 添加数据处理代码 :
            USB_IS_REMMOVE_COUNT = 0;//清除串口是否掉线超时检测
            if (callBackCount)
                return;
            Log.w(TAG, "onNewData isOnLight=" + isOnLight);
            if (isOnLight) {//电机到位后打开灯
                isOnLight = false;
                callBackCount = true;
                handleSaturationLightUSART(CameraServerHandler.csaturation);
                //如果是激发块延时50毫秒再允许操作
                EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
                    @Override
                    public void run() {
                        callBackCount = false;
                        synchronized (swithObject) {
                            FileUtils.writeFileToLogFolder("电机到位后打开灯：Swith+" + objectiveIsSwith);
                            objectiveIsSwith = false;
                        }
                    }
                }, 500, TimeUnit.MILLISECONDS);

            } else {
                synchronized (swithObject) {
                    objectiveIsSwith = false;
                }
            }
            if (ConstantUtil.LONZA) {

                updateReceivedDataSmart(data);

            } else {

                updateReceivedData(data);

            }

        }
    };
    private ArrayList<Byte> strArray = new ArrayList<Byte>();
    private boolean dflags = false;

    private void updateReceivedDataSmart(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            strArray.add(data[i]);
            if (dflags && data[i] == 0x0a) {
                byte[] bytes = new byte[strArray.size()];
                for (int j = 0; j < strArray.size(); j++) {
                    bytes[j] = strArray.get(j);
                }
                strArray.clear();
                updateChange(bytes);
                dflags = false;

            }
            if (data[i] == 0x0d) {
                dflags = true;
            }
        }

    }

    private boolean switchStataI = false;
    private boolean switchStataII = false;
    private float temperature = 0;//温度数值
    private boolean tpFlags = true;//温度正、负
    private int codingLocation = 0;//编码位置

    private void updateChange(byte[] array) {

        //4a 50 4c 59
        if (array[0] != 0x4a || array[1] != 0x50 || array[2] != 0x4c || array[3] != 0x59)
            return;
        switch (array[17]) {
            case 0x53:
                for (int i = 0; i < array.length; i++) {
                    Log.w(TAG, "arrays S[" + i + "]=" + array[i]);
                    switch (i) {      //检测是否重复接收到数据帧帧头 :
                        case 4:     //光源开关状态 :
                            if (array[i] != -1) {        //判断是否为有效数据

                                if (array[i] == 1) {   //LED1:bit0  1：打开; 0：关闭

                                    LED1_ON_FLAG_T = true;
                                    Log.w(TAG, "arrays LED1_ON_FLAG_T" + LED1_ON_FLAG_T);
                                } else {
                                    LED1_ON_FLAG_T = false;
                                }

                            } else {
                                LED1_ON_FLAG_T = false;
                                LED2_ON_FLAG_T = false;
                                LED3_ON_FLAG_T = false;
                                LED4_ON_FLAG_T = false;
                            }
                            // Log.e("CameraServerHandler","e"+callBackCount);

                            mChecksum = 0;
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 5: // 限位开关状态
                            if (array[i] != -1) {       //判断是否为有效数据
                                if ((array[i] & 0x01) == 0x01) {
                                    switchStataI = true;
                                } else {
                                    switchStataI = false;
                                }
                                if ((array[i] & 0x02) == 0x02) {
                                    switchStataII = true;
                                } else {
                                    switchStataII = false;
                                }
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 6:     //对焦 电机2位置 bit[31:24]
                            mTransientFocusMotorStatus = 0;
                            if (array[i] != -1) {        //判断是否为有效数据
                                mTransientFocusMotorStatus += ((array[i] << 24) & 0x00000000FF000000);
                            } else {
                                mTransientFocusMotorStatus &= 0x00FFFFFF;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 7:     //对焦 电机2位置 bit[23:16]
                            if (array[i] != -1) {        //判断是否为有效数据
                                mTransientFocusMotorStatus += ((array[i] << 16) & 0x0000000000FF0000);

                            } else {
                                mTransientFocusMotorStatus &= 0xFF00FFFF;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 8:     //对焦 电机2位置 bit[15:8]
                            if (array[i] != -1) {        //判断是否为有效数据
                                mTransientFocusMotorStatus += ((array[i] << 8) & 0x000000000000FF00);
                            } else {
                                mTransientFocusMotorStatus &= 0xFFFF00FF;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 9:     //对焦 电机2位置 bit[7:0]
                            if (array[i] != -1) {        //判断是否为有效数据
                                mTransientFocusMotorStatus += ((array[i] << 0) & 0x00000000000000FF);
                            } else {
                                mTransientFocusMotorStatus &= 0xFFFFFF00;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 10:     //编码器位置 bit[31:24]
                            codingLocation = 0;
                            if (array[i] != -1) {        //判断是否为有效数据
                                codingLocation += ((array[i] << 24) & 0x00000000FF000000);
                            } else {
                                codingLocation &= 0x00FFFFFF;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 11:     //编码器位置 bit[23:16]
                            if (array[i] != -1) {        //判断是否为有效数据
                                codingLocation += ((array[i] << 16) & 0x0000000000FF0000);

                            } else {
                                codingLocation &= 0xFF00FFFF;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 12:     //编码器位置 bit[15:8]
                            if (array[i] != -1) {        //判断是否为有效数据
                                codingLocation += ((array[i] << 8) & 0x000000000000FF00);
                            } else {
                                codingLocation &= 0xFFFF00FF;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 13:     //编码器位置 bit[7:0]
                            if (array[i] != -1) {        //判断是否为有效数据
                                codingLocation += ((array[i] << 0) & 0x00000000000000FF);
                            } else {
                                codingLocation &= 0xFFFFFF00;
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 14: //温度正 负
                            if (array[i] != -1) {
                                //正
                                if (array[i] == 0x00) {
                                    tpFlags = true;
                                } else {
                                    tpFlags = false;
                                }
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);
                            break;
                        case 15://温度整数数值
                            if (array[i] != -1) {
                                //正
                                if (tpFlags) {
                                    temperature = array[i];
                                } else {
                                    temperature = -array[i];
                                }
                            }

                            mChecksum += (array[i] & 0x00000000000000FF);

                            break;
                        case 16:   //温度小数
                            if (array[i] != -1) {
                                //正
                                if (tpFlags) {

                                    temperature += (array[i] / 10.0f);
                                } else {

                                    temperature -= (array[i] / 10.0f);
                                }
                            }
                            mChecksum += (array[i] & 0x00000000000000FF);

                            break;
                        case 17:  //数据类型

                            mChecksum += (array[i] & 0x00000000000000FF);

                            break;

                        case 18:        //校验和bit[15:8] ;
                            mTransientChecksum = 0;
                            if (array[i] != -1) {        //判断是否为有效数据
                                mTransientChecksum += ((array[i] << 8) & 0x000000000000FF00);
                            } else {
                                mTransientChecksum &= 0xFFFF00FF;
                            }
                            break;
                        case 19:        //校验和bit[7:0] ;

                            if (array[i] != -1) {        //判断是否为有效数据
                                mTransientChecksum += ((array[i] << 0) & 0x00000000000000FF);
                            } else {
                                mTransientChecksum &= 0xFFFFFF00;
                            }

                            if (mChecksum == mTransientChecksum)    //判断数据校验和 ;
                                mCheckSumFlag = true;
                            else
                                mCheckSumFlag = false;

                            if (!mCheckSumFlag) {
                                // Log.e(TAG, "mChecksum = " + mChecksum + " , mTransientChecksum = " + mTransientChecksum);
                            }

                            mChecksum = 0;

                            break;
                        case 20:        //回车\r 0x0d
                            if (array[i] == 0x0d)      //回车\r 0x0d
                                mDataFramesFinishFlag = true;
                            else
                                mDataFramesFinishFlag = false;

                            if (!mDataFramesFinishFlag) {
                                // Log.e(TAG, "array[i] != 0x0d = " + array[i]);
                            }

                            mChecksum = 0;
                            //                                    Log.w(TAG, "  case 20 ! === ");
                            break;
                        case 21:        //换行\n  0x0a
                            if (mDataFramesFinishFlag) {
                                if (array[i] == 0x0a)      //换行\n  0x0a
                                    mDataFramesFinishFlag = true;
                                else
                                    mDataFramesFinishFlag = false;
                            }
                            if (!mDataFramesFinishFlag) {
                                // Log.e(TAG, "array[i] != 0x0a = " + array[i]);
                            }
                            mChecksum = 0;
                            break;

                    }

                }
                break;
            case 0x54://时间
                Log.w(TAG, "arrays T");
                year = String.valueOf(Integer.toHexString(array[9]));
                if (year.length() < 2)
                    year = "0" + year;
                month = String.valueOf(Integer.toHexString(array[8]));
                if (month.length() < 2)
                    month = "0" + month;
                day = String.valueOf(Integer.toHexString(array[7]));
                if (day.length() < 2)
                    day = "0" + day;
                hours = String.valueOf(Integer.toHexString(array[6]));
                if (hours.length() < 2)
                    hours = "0" + hours;
                minuter = String.valueOf(Integer.toHexString(array[5]));
                if (minuter.length() < 2)
                    minuter = "0" + minuter;
                secode = String.valueOf(Integer.toHexString(array[4]));
                if (secode.length() < 2)
                    secode = "0" + secode;
                String timestr = "20" + year + month + day + "." + hours + minuter + secode;
                Log.w(TAG, "arrays T timestr" + timestr);
                FileUtils.writeFileToLogFolder("-----------------------串口接收到的时间：" + timestr);
                setTimerZ();
                setTime(timestr);
                useTheDate("20" + year + month + day);
                break;
            case 0x45://设备号

                break;
            default:

                break;
        }
        Log.w(TAG, "arraysT  LED1_ON_FLAG_T=" + LED1_ON_FLAG_T);
        LED1_ON_FLAG = LED1_ON_FLAG_T;  //LED1 开关标识;  (白光 1)
        LED2_ON_FLAG = LED2_ON_FLAG_T;  //LED2 开关标识;  (蓝光 2)
        LED3_ON_FLAG = LED3_ON_FLAG_T;  //LED3 开关标识;  (绿光 3)
        LED4_ON_FLAG = LED4_ON_FLAG_T;  //LED4 开关标识;  (紫光 4)

        LIGHTSWITCH1_ON_FLAG = LIGHTSWITCH1_ON_FLAG_T;  //光电开关1 开关标识;  (激发块复位位置开关状态)
        LIGHTSWITCH2_ON_FLAG = LIGHTSWITCH2_ON_FLAG_T;  //光电开关2 开关标识; (激发块开关状态)
        LIGHTSWITCH3_ON_FLAG = LIGHTSWITCH3_ON_FLAG_T;  //光电开关3 开关标识; (对焦电机0位置)
        LIGHTSWITCH4_ON_FLAG = LIGHTSWITCH4_ON_FLAG_T;  //光电开关4 开关标识; (对焦电机Max位置)
        LIGHTSWITCH5_ON_FLAG = LIGHTSWITCH5_ON_FLAG_T;  //光电开关5 开关标识; (物镜电机右端位置)
        LIGHTSWITCH6_ON_FLAG = LIGHTSWITCH6_ON_FLAG_T;  //光电开关6 开关标识; (物镜电机左端位置)
        mVoltageValue = mTransientVoltageValue;             //AD电压值
        mLightMotorStatus = mTransientLightMotorStatus;     //灯光电机1 位置状态
        mLensMotorStatus = mTransientLensMotorStatus;       //物镜电机2 位置状态
        mFocusMotorStatus = mTransientFocusMotorStatus;     //对焦电机3 位置状态
       /* SPUtils.put(mContext, "LED1(W)", LED1_ON_FLAG);
        SPUtils.put(mContext, "LED2(B)", LED2_ON_FLAG);
        SPUtils.put(mContext, "LED3(G)", LED3_ON_FLAG);
        SPUtils.put(mContext, "LED4(P)", LED4_ON_FLAG);
        SPUtils.put(mContext, "LIGHTSWITCH1(reset)", LIGHTSWITCH1_ON_FLAG);
        SPUtils.put(mContext, "LIGHTSWITCH2(light)", LIGHTSWITCH2_ON_FLAG);
        SPUtils.put(mContext, "LIGHTSWITCH3(focus0)", LIGHTSWITCH3_ON_FLAG);
        SPUtils.put(mContext, "LIGHTSWITCH4(focus+)", LIGHTSWITCH4_ON_FLAG);
        SPUtils.put(mContext, "LIGHTSWITCH5(lens_r)", LIGHTSWITCH5_ON_FLAG);
        SPUtils.put(mContext, "LIGHTSWITCH6(lens_l)", LIGHTSWITCH6_ON_FLAG);
        SPUtils.put(mContext, "VoltageValue", mVoltageValue);
        SPUtils.put(mContext, "LightMotorStatus", mLightMotorStatus);
        SPUtils.put(mContext, "LensMotorStatus", mLensMotorStatus);
        SPUtils.put(mContext, "FocusMotorStatus", mFocusMotorStatus, true);
        SPUtils.put(mContext, "MaxFocus", mMaxFocusMotorStatus);
        Log.w(TAG, "arraysT  LED1_ON_FLAG=" +LED1_ON_FLAG);
        Log.w(TAG, "arraysT  switchStataI=" +switchStataI);
        Log.w(TAG, "arraysT switchStataII=" +switchStataII);
        Log.w(TAG, "arraysT temperature=" +temperature);
        Log.w(TAG, "arraysT codingLocation=" +codingLocation);
        Log.w(TAG, "arraysT mTransientFocusMotorStatus=" +mTransientFocusMotorStatus);*/
    }

    private void onDeviceStateChange() {
        FileUtils.writeFileToLogFolder("onDeviceStateChange -> stopIoManager ->startIoManager");
        stopIoManager();
        startIoManager();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {

            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {

        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void initializeDriver(UsbSerialPort mPort) {

        // FileUtils.writeFileToLogFolder("===========sport="+sPort+"  usbmanager="+mUsbManager+" ");

        if (mPort == null) {

        } else {

            if (mUsbManager != null) {
                Log.w("mytest", "initializeDriver  sPort" + mPort);
                // List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
                UsbDevice usbDevice = null;
                if (!deviceList.isEmpty()) { // deviceList不为空
                    // 枚举到设备
                    for (UsbDevice device : deviceList.values()) {
                        // 输出设备信息
                        Log.d(TAG, "DeviceInfo: " + device.getVendorId() + " , "
                                + device.getProductId() + "deviceinfaceCount=" + device.getInterfaceCount());
                        if (device.getInterfaceCount() > 0) {

                            for (int i = 0; i < device.getInterfaceCount(); i++) {
                                UsbInterface intf = device.getInterface(i);
                                Log.d(TAG, "intf.getInterfaceClass()=" + intf.getInterfaceClass());
                                Log.d(TAG, "intf.getInterfaceSubclass() =" + intf.getInterfaceSubclass());
                                Log.d(TAG, "intf.getInterfaceProtocol()=" + intf.getInterfaceProtocol());
                            }
                            if (device.getVendorId() == 1204
                                    && device.getProductId() == 248) {
                                Log.d(TAG, "枚举设备成功");
                            }
                        }

                    }
                }
                UsbDeviceConnection connection = mUsbManager.openDevice(mPort.getDriver().getDevice());
                Log.w("mytest", "devicename=  " + mPort.getDriver().getDevice().getDeviceName());
                Log.w("mytest", "VendorId=  " + mPort.getDriver().getDevice().getVendorId());
                Log.w("mytest", "getInterfaceCount=  " + mPort.getDriver().getDevice().getInterfaceCount());

                int interfaceCount = mPort.getDriver().getDevice().getInterfaceCount();
                if (connection == null || interfaceCount < 1) {
                    return;
                }
                try {
                    mPort.open(connection);
                    FileUtils.writeFileToLogFolder("已经打开USB串口");
                    //sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    mPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE, UsbSerialPort.FLOWCONTROL_NONE);
                    IS_USART_CONNECT = true;
                } catch (IOException e) {
                    FileUtils.writeFileToLogFolder("===============打开USB串口IO异常 e=" + e + "=======================");
                    e.printStackTrace();
                    try {
                        mPort.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    mPort = null;

                    return;
                }
            } else {
            }
        }
        onDeviceStateChange();
    }

    private void updateReceivedData(byte[] data) {
        // final String message;
        final String midMessage;

        midMessage = dataFramesToValue(data);

    }

    public void requestCallBackStatus() {

        if (objectiveIsSwith || UVCService.isAutoFocus())
            return;
        String commandString = "4A504C59" + "10" + "00000000000000" + "0D0A";
        sendControlCommand(commandString);  //查询一次 ;
        removeMessages(MSG_RELEASE_SWITH);
        sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
        objectiveIsSwith = true;

    }

    //
    private void stopElectricFocus() {
        final String str = "4A504C5908020000000000000D0A";
        removeMessages(MSG_RELEASE_SWITH);
        sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
        objectiveIsSwith = true;
        PORT_SERVIEC.execute(new Runnable() {
            @Override
            public void run() {
                //Hex发送 :
                byte[] writeBuffer = HexDump.hexStringToByteArray(str);

                if (IS_USART_CONNECT) {
                    if (mSerialIoManager != null) {
                        mSerialIoManager.writeAsync(writeBuffer);

                    }
                }
            }
        });
    }
    public static final  ExecutorService PORT_SERVIEC = Executors.newSingleThreadExecutor();
    public void sendControlCommand(final String commandString) {

        if (!isdebug) {
            if (!commandString.equals("4A504C5912000000000000000D0A"))
                return;
        }
        // Log.w(TAG,"sendControlCommand1");
        PORT_SERVIEC.execute(new Runnable() {
            @Override
            public void run() {
                //Hex发送 :
                byte[] writeBuffer = HexDump.hexStringToByteArray(commandString);

                if (IS_USART_CONNECT) {
                    if (mSerialIoManager != null) {
                        mSerialIoManager.writeAsync(writeBuffer);

                    }
                }
            }
        });
    }

    public String integerTo2ByteHexString(int value) {
        String hexString = Integer.toHexString(value & 0xffff);
        switch (hexString.length()) {
            case 0:
                hexString = "0000";
                break;
            case 1:
                hexString = "000" + hexString;
                break;
            case 2:
                hexString = "00" + hexString;
                break;
            case 3:
                hexString = "0" + hexString;
                break;
            default:
                //                        step = step.substring(step.length() - 4, step.length());
                break;
        }
        return hexString;
    }

    public String integerToHexString(int value) {
        String hexString = Integer.toHexString(value & 0xff);
        switch (hexString.length()) {
            case 0:
                hexString = "00";
                break;
            case 1:
                hexString = "0" + hexString;
                break;
            default:
                //                        step = step.substring(step.length() - 4, step.length());
                break;
        }
        return hexString;
    }

    private boolean IS_USART_CONNECT = false;
    private int UART_CONNECT_REFRESH = 5;
    private boolean mIsStop = false;

    public void stopCamera() {

        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
                mUVCCamera.reLeaseCamera();
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
            mSync.notifyAll();
        }
        //  handleCloseUVCCamera();
    }

    public void reStartCamera() {
        if (mCtrlBlock != null) {
            synchronized (mSync) {
                mUVCCamera = new UVCCamera();
                mUVCCamera.open(mCtrlBlock);
                mUVCCamera.updateCameraParams();

                //----------------重要分析步骤 1 :
                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX/*UVCCamera.PIXEL_FORMAT_NV21*/);
                //                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
                //                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565);

                mUVCCamera.setStatusCallback(new IStatusCallback() {
                    @Override
                    public void onStatus(final int statusClass, final int event, final int selector, final int statusAttribute, final ByteBuffer data) {

                    }
                });

                mUVCCamera.setButtonCallback(new IButtonCallback() {
                    @Override
                    public void onButton(final int button, final int state) {
                    }
                });

            }
        }
        handleStartPreview(mFrameWidth, mFrameHeight, mRenderHolderRunnable.getSurface());

    }

    public boolean isUartConnect(int constant) {
        boolean connect;
        connect = isconnect;
        CameraServerHandler.ccontrast = constant;

        return connect;

    }

    public void handleConnectUSART() {
        if (DEBUG)
            Log.v(TAG, "handleConnectUSART:");
        sendEmptyMessage(MESSAGE_REFRESH);

        if (!IS_USART_CONNECT) {
            refreshDeviceList();
            /*if (mEntries != null && mEntries.size() > 0 && isPostExecute) {
                sPort = mEntries.get(0);
                initializeDriver();

            } else {

            }*/
        } else {
            // Log.e(TAG, " === USB device is connect === ");
        }

    }

    public void handleDisconnectUSART() {
        if (DEBUG)
            Log.v(TAG, "handleDisconnectUSART:");
        removeMessages(MESSAGE_REFRESH);
        //20180425 注释
        //IS_USART_CONNECT = false;
    }

    public static int THRESHOLD_VALUE = 320;      //对焦电机速度选择的判断分界值 ;

    public void mHandleGammaUart(int focus, boolean isForWard, int currentstep, String speedSelect) {

        String selection = "02";    //02电机
        String direction;    //电机运作方向; 默认正在0x55 , 反转0xAA
        if (isForWard) {
            direction = "aa";
        } else {
            direction = "55";
        }
        String step = "5555";    //电机运转的步数 : 小端方式 , 高位存在地址高位
        String stepModle = "aa";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA
        //String speedSelect = "04";    //电机速度选择 ; 平滑慢速0x04 , 高速0x09 ;


        int stepNum = focus;

       /* if (stepNum == 32) {//如果是手动最小档 32/16=2步
            speedSelect = "00";
        } else {
            speedSelect = "08";
        }*/

        // int gamma = BaseApplication.getInstance().getGamma();
        int gamma = CameraServerHandler.cgamma;
        if (stepNum == 32) {
            if (isForWard)
                gamma += 2;
            else
                gamma -= 2;

        } else {
            if (isForWard)
                gamma += stepNum;
            else
                gamma -= stepNum;
        }
        if (gamma >= 8500) {
            gamma = 8500;
        } else if (gamma <= 0) {
            gamma = 0;
        }

        if (currentstep != -1) {
           /* if (gamma != (currentstep + stepNum)) {
                BaseApplication.getInstance().setGamma(currentstep);
            }*/
            // BaseApplication.getInstance().setGamma(gamma);
            CameraServerHandler.cgamma = gamma;
        } else {
            // BaseApplication.getInstance().setGamma(gamma);
            CameraServerHandler.cgamma = gamma;
            // Log.e("SettingActivity", "BaseApplication.getInstance().getGamma()1=" + BaseApplication.getInstance().getGamma());
        }

        //Log.e("SettingActivity", "BaseApplication.getInstance().getGamma()1=" + BaseApplication.getInstance().getGamma());
        step = integerTo2ByteHexString(stepNum & 0xffff);

        //        mGamma = focus_abs;
        String commandString = "4A504C59" + selection + direction + step + stepModle + speedSelect + "0000" + "0D0A";
        sendControlCommand(commandString);
        if (isOpentOjectiveSwith) {

            synchronized (swithObject) {
                removeMessages(MSG_RELEASE_SWITH);
                sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
                objectiveIsSwith = true;
            }
            isOpentOjectiveSwith = false;

        }
        removeMessages(MESSAGE_STOP_ELECTRIC);
        sendMessageDelayed(obtainMessage(CameraServerHandler.MESSAGE_STOP_ELECTRIC), 8000);


    }

    public void releadFocus() {
        String commandString;
        if (ConstantUtil.LONZA)
            commandString = "4A504C59" + "02" + "55" + "09" + "005509040000";
        else
            commandString = "4A504C59" + "06" + "02" + "02" + "000000000000";
        // BaseApplication.getInstance().setGamma(0);
        CameraServerHandler.cgamma = 0;
        sendControlCommand(commandString);
    }

    private boolean isdebug = true;

    public void sendStringToUart(String str) {
        Log.w(TAG, "sendStringToUart");
        str += "0D0A";
        if (str.equals("4A504C5912000000000000000D0A")) {

            isdebug = false;
            isReturnTime = true;
            //3秒后释放串口锁定标记
            removeMessages(MSG_RETIMERFALG);
            sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RETIMERFALG), 5000);
            synchronized (swithObject) {
                removeMessages(MSG_RELEASE_SWITH);
                sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
                objectiveIsSwith = true;
            }
        } else if (str.equals("4A504C5906010200000000000D0A")) {//激发块位置复位
            synchronized (swithObject) {
                removeMessages(MSG_RELEASE_SWITH);
                sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
                objectiveIsSwith = true;
            }
        }

        sendControlCommand(str);
    }

    private int mnum = 0;

    public void handleGammaUSART(int focus_abs) {      //设置物镜对焦位置控制 :

        boolean f = false;
        String selection = "02";    //02电机
        if (ConstantUtil.LONZA) {
            selection = "03";
        } else {
            selection = "02";
        }
        String direction = "55";    //电机运作方向; 默认正在0x55 , 反转0xAA
        String step = "5555";    //电机运转的步数 : 小端方式 , 高位存在地址高位
        String stepModle = "AA";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA
        String speedSelect = "04";    //电机速度选择 ; 平滑慢速0x04 , 高速0x09 ;

        int stepNum = 0;

        if (focus_abs > mGamma) {
            f = true;
            direction = "AA";

            stepNum = focus_abs - mGamma;
        } else if (focus_abs < mGamma) {
            f = false;
            direction = "55";
            stepNum = mGamma - focus_abs;
        }

        if (stepNum > THRESHOLD_VALUE) {

            stepNum = stepNum / 32;
        }
        speedSelect = "09";


        if (f) {
            mnum = mnum + stepNum;
        } else {
            mnum = mnum - stepNum;
        }
        step = integerTo2ByteHexString(stepNum & 0xffff);

        String commandString = null;
        if (ConstantUtil.ModorType == 1) {
            commandString = "4A504C59" + selection + direction + step + stepModle + speedSelect + "0000" + "0D0A";
        } else
            commandString = "4A504C59" + selection + direction + step + stepModle + speedSelect + "0400" + "0D0A";
        sendControlCommand(commandString);


    }

    public void handleSaturationUSART(final int saturation_abs) {      //设置激发灯光状态控制 :

        String selection = "01";    //01电机
        String direction = "55";    //电机运作方向; 默认正在0x55 , 反转0xAA
        String step = "5555";    //电机运转的步数 : 小端方式 , 高位存在地址高位
        String stepModle = "11";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA ,位置模式0x11

        if (saturation_abs > mSaturation) {
            direction = "55";

        } else if (saturation_abs < mSaturation) {
            direction = "AA";
            //  step = Integer.toHexString((mSaturation - progress) & 0xffff);
        }
        step = integerTo2ByteHexString(saturation_abs & 0xffff);

        mSaturation = saturation_abs;
        //加回车换行符 : 0x0D0A
        String commandString = "4A504C59" + selection + direction + step + stepModle + "080500" + "0D0A";


        //handleSaturationLightUSART(saturation_abs);


        if (saturation_abs == 0 || LightCount == 1) {
            handleSaturationLightUSART(saturation_abs);
        } else {
            sendControlCommand(commandString);
            isOnLight = true;
        }

        if (isOpentOjectiveSwith) {
            synchronized (swithObject) {
                removeMessages(MSG_RELEASE_SWITH);
                sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
                objectiveIsSwith = true;
            }

            isOpentOjectiveSwith = false;
        }

    }

    public void handleSaturationLightUSART(int lightID) {       //激发灯光开关控制 :
        byte lightId = 1;
        String lightIDString = "00";
        if (lightID != 0)
            lightIDString = integerToHexString((lightId << (lightID - 1)) & 0xff);

        //加回车换行符 : 0x0D0A
        String commandString = "4A504C59" + "07" + lightIDString + "000000000000" + "0D0A";
        sendControlCommand(commandString);

    }


    public void handleContrastUSART(int contrast_abs) {      //设置物镜状态控制 :
        String commandString = "";
        String selection = "05";
        String direction = "00";
        String stepModle = "11";
        String state = "01";
        String step = "5555";

        if (mobjects.length == 3) {
            selection = "05";    //05电机
            direction = "00";    //电机运作方向; 默认正在0x55 , 反转0xAA
            stepModle = "11";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA
            state = "01";//默认物镜

            for (int i = 0; i < mobjects.length; i++) {
                final int contrast = mobjects[i];

                if (contrast_abs == contrast) {
                    switch (i) {
                        case 0:
                            state = "01";
                            break;
                        case 1:
                            state = "02";
                            break;
                        case 2:
                            state = "03";
                            break;
                    }
                }
            }
            commandString = "4A504C59" + selection + direction + "00" + state + stepModle + "090500" + "0D0A";
        } else if (mobjects.length == 2) {
            direction = "55";//默认正在0x55
            step = "5555";//默认步数 不重要
            if (contrast_abs == mobjects[0]) {
                direction = "AA";
            } else {
                direction = "55";
            }
            commandString = "4A504C59" + selection + direction + step + stepModle + "090500" + "0D0A";
        }
        sendControlCommand(commandString);
        if (isOpentOjectiveSwith) {

            synchronized (swithObject) {
                removeMessages(MSG_RELEASE_SWITH);
                sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
                objectiveIsSwith = true;
            }
            isOpentOjectiveSwith = false;

        }

    }

    private Object swithObject = new Object();
    public static boolean objectiveIsSwith = false;//判断串口是否在工作中
    private boolean isOpentOjectiveSwith = false;
    private boolean isOnLight = false;//判断是否是灯返回值

    public boolean getObjectiveSwithState(boolean isOn) {
        isOpentOjectiveSwith = isOn;

        FileUtils.writeFileToLogFolder("机器正忙：Swith=" + objectiveIsSwith);

        return objectiveIsSwith;
    }
    //===============================================

    public final int DATA_FRAMES_LENGTH = 22;           //数据帧包含的字节长度 ;
    public int mCurrentRecByteCount = 0;                //当前接收到数据帧的第n字节;
    public int mRecByteCountSecure = 0;                 //当前接收到数据帧的第n字节(用于容错计数);
    public boolean mDataFramesHeadFlag = false;      //当正确接收到数据帧帧头标识;
    public boolean mDataFramesSecureFlag = false;    //当正确接收到数据帧帧头标识(用于容错计数);

    public boolean mCheckSumFlag = false;               //校验和比较标识;
    public boolean mDataFramesFinishFlag = false;      //当正确接收完一帧数据标识;

    public boolean LED1_ON_FLAG_T = false;      //LED1 开关标识;    (瞬时值)
    public boolean LED2_ON_FLAG_T = false;      //LED1 开关标识;    (瞬时值)
    public boolean LED3_ON_FLAG_T = false;      //LED1 开关标识;    (瞬时值)
    public boolean LED4_ON_FLAG_T = false;      //LED1 开关标识;    (瞬时值)

    public boolean LIGHTSWITCH1_ON_FLAG_T = false;      //光电开关1 开关标识;    (瞬时值)
    public boolean LIGHTSWITCH2_ON_FLAG_T = false;      //光电开关2 开关标识;    (瞬时值)
    public boolean LIGHTSWITCH3_ON_FLAG_T = false;      //光电开关3 开关标识;    (瞬时值)
    public boolean LIGHTSWITCH4_ON_FLAG_T = false;      //光电开关4 开关标识;    (瞬时值)
    public boolean LIGHTSWITCH5_ON_FLAG_T = false;      //光电开关5 开关标识;    (瞬时值)
    public boolean LIGHTSWITCH6_ON_FLAG_T = false;      //光电开关6 开关标识;    (瞬时值)

    public int mTransientLightMotorStatus = 0;      //灯光电机1 位置状态 (瞬时值)
    public int mTransientLensMotorStatus = 0;       //物镜电机2 位置状态 (瞬时值)
    public int mTransientFocusMotorStatus = 0;      //对焦电机3 位置状态 (瞬时值)
    public float mTransientVoltageValue = 0;        //AD电压值  (瞬时值)
    public long mTransientChecksum = 0;             //校验和  (瞬时值)

    public boolean LED1_ON_FLAG = false;      //LED1 开关标识;  (白光 1)
    public boolean LED2_ON_FLAG = false;      //LED2 开关标识;  (蓝光 2)
    public boolean LED3_ON_FLAG = false;      //LED3 开关标识;  (绿光 3)
    public boolean LED4_ON_FLAG = false;      //LED4 开关标识;  (紫光 4)

    public boolean LIGHTSWITCH1_ON_FLAG = false;      //光电开关1 开关标识;  (激发块复位位置开关状态)
    public boolean LIGHTSWITCH2_ON_FLAG = false;      //光电开关2 开关标识; (激发块开关状态)
    public boolean LIGHTSWITCH3_ON_FLAG = false;      //光电开关3 开关标识; (对焦电机0位置)
    public boolean LIGHTSWITCH4_ON_FLAG = false;      //光电开关4 开关标识; (对焦电机Max位置)
    public boolean LIGHTSWITCH5_ON_FLAG = false;      //光电开关5 开关标识; (物镜电机右端位置)
    public boolean LIGHTSWITCH6_ON_FLAG = false;      //光电开关6 开关标识; (物镜电机左端位置)

    public int mLightMotorStatus = 0;      //灯光电机1 位置状态
    public int mLensMotorStatus = 0;       //物镜电机2 位置状态
    public int mFocusMotorStatus = 0;      //对焦电机3 位置状态
    public float mVoltageValue = 0;        //AD电压值
    private float mVoltageLevel = 0;            //电压百分比
    public long mChecksum = 0;             //校验和
    public int mMaxFocusMotorStatus = 0;   //对焦电机3 位置最大值

    private boolean isReturnTime = true;
    private String lastyear = "";
    private String lastmonth = "";
    private String lastday = "";
    private String year = "";
    private String month = "";
    private String day = "";
    private String hours = "";
    private String minuter = "";
    private String secode = "";
    private int timercount = 0;

    public String dataFramesToValue(byte[] array) {

        String dataFramesString =
                "\nLED1(W) = " + LED1_ON_FLAG +
                        "\nLED2(B) = " + LED2_ON_FLAG +
                        "\nLED3(G) = " + LED3_ON_FLAG +
                        "\nLED4(P) = " + LED4_ON_FLAG +
                        "\nLIGHTSWITCH1(reset) = " + LIGHTSWITCH1_ON_FLAG +
                        "\nLIGHTSWITCH2(light) = " + LIGHTSWITCH2_ON_FLAG +
                        "\nLIGHTSWITCH3(focus0) = " + LIGHTSWITCH3_ON_FLAG +
                        "\nLIGHTSWITCH4(focus+) = " + LIGHTSWITCH4_ON_FLAG +
                        "\nLIGHTSWITCH5(lens_r) = " + LIGHTSWITCH5_ON_FLAG +
                        "\nLIGHTSWITCH6(lens_l) = " + LIGHTSWITCH6_ON_FLAG +
                        "\nVoltageValue = " + mVoltageValue +
                        "\nLightMotorStatus = " + mLightMotorStatus +
                        "\nLensMotorStatus = " + mLensMotorStatus +
                        "\nFocusMotorStatus = " + mFocusMotorStatus +
                        "\nMaxFocus = " + mMaxFocusMotorStatus;

        Log.w(TAG, "array.length=" + array.length);

        for (int i = 0; i < array.length; i++) {
            Log.w(TAG, "array.length[" + i + "]=" + array[i]);
            switch (mCurrentRecByteCount + mRecByteCountSecure) {
                case 0:
                    if (array[i] == 0x4a) {      //J
                        //                        Log.w(TAG, "J 0x4a = " + array[i]);
                        mCurrentRecByteCount = 1;
                    } else {
                        mCurrentRecByteCount = 0;
                    }
                    mDataFramesHeadFlag = false;
                    mRecByteCountSecure = 0;
                    break;
                case 1:
                    if (array[i] == 0x50) {      //P
                        //                        Log.w(TAG, "P 0x50 = " + array[i]);
                        mCurrentRecByteCount = 2;
                    } else {
                        mCurrentRecByteCount = 0;
                    }
                    mDataFramesHeadFlag = false;
                    mRecByteCountSecure = 0;
                    break;
                case 2:
                    if (array[i] == 0x4c) {      //L
                        //                        Log.w(TAG, "L 0x4c = " + array[i]);
                        mCurrentRecByteCount = 3;
                    } else {
                        mCurrentRecByteCount = 0;
                    }
                    mDataFramesHeadFlag = false;
                    mRecByteCountSecure = 0;
                    break;
                case 3:
                    if (array[i] == 0x59) {      //Y
                        //                        Log.w(TAG, "Y 0x59 = " + array[i]);
                        mCurrentRecByteCount = 4;
                        mDataFramesHeadFlag = true;
                    } else {
                        mCurrentRecByteCount = 0;
                        mDataFramesHeadFlag = false;
                    }
                    mRecByteCountSecure = 0;
                    break;
                default:
                    if (mDataFramesHeadFlag & (mCurrentRecByteCount > 3)) {
                        if (mDataFramesSecureFlag) {
                            mCurrentRecByteCount = 4;

                            mRecByteCountSecure = 0;
                            mDataFramesSecureFlag = false;
                        }

                        if (mCurrentRecByteCount < DATA_FRAMES_LENGTH) {
                            switch (mRecByteCountSecure) {      //检测是否重复接收到数据帧帧头 :
                                case 0:
                                    if (array[i] == 0x4a) {      //J
                                        mRecByteCountSecure = 1;
                                    } else {
                                        mRecByteCountSecure = 0;
                                        mDataFramesSecureFlag = false;
                                    }
                                    break;
                                case 1:
                                    if (array[i] == 0x50) {      //P
                                        mRecByteCountSecure = 2;
                                    } else {
                                        mRecByteCountSecure = 0;
                                        mDataFramesSecureFlag = false;
                                    }
                                    break;
                                case 2:
                                    if (array[i] == 0x4c) {      //L
                                        mRecByteCountSecure = 3;
                                    } else {
                                        mRecByteCountSecure = 0;
                                        mDataFramesSecureFlag = false;
                                    }
                                    break;
                                case 3:
                                    if (array[i] == 0x59) {      //Y
                                        mRecByteCountSecure = 4;
                                        mDataFramesSecureFlag = true;
                                    } else {
                                        mRecByteCountSecure = 0;
                                        mDataFramesSecureFlag = false;
                                    }
                                    break;
                                default:
                                    mRecByteCountSecure = 0;
                                    mDataFramesSecureFlag = false;
                                    break;
                            }

                            //处理数据帧 真实数据 :
                            switch (mCurrentRecByteCount) {      //检测是否重复接收到数据帧帧头 :
                                case 4:     //光源开关状态 :
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        if (isReturnTime) {
                                            secode = String.valueOf(Integer.toHexString(array[i]));
                                            if (secode.length() < 2)
                                                secode = "0" + secode;
                                        }

                                        if ((array[i] & 0x01) == 0x01) {   //LED1:bit0  1：打开; 0：关闭

                                            LED1_ON_FLAG_T = true;

                                        } else {
                                            LED1_ON_FLAG_T = false;
                                        }

                                        if ((array[i] & 0x02) == 0x02)     //LED2:bit1  1：打开; 0：关闭
                                            LED2_ON_FLAG_T = true;
                                        else
                                            LED2_ON_FLAG_T = false;
                                        if ((array[i] & 0x04) == 0x04)     //LED3:bit2  1：打开; 0：关闭
                                            LED3_ON_FLAG_T = true;
                                        else
                                            LED3_ON_FLAG_T = false;
                                        if ((array[i] & 0x08) == 0x08)    //LED4:bit3  1：打开; 0：关闭
                                            LED4_ON_FLAG_T = true;
                                        else
                                            LED4_ON_FLAG_T = false;

                                    } else {
                                        LED1_ON_FLAG_T = false;
                                        LED2_ON_FLAG_T = false;
                                        LED3_ON_FLAG_T = false;
                                        LED4_ON_FLAG_T = false;
                                    }
                                    // Log.e("CameraServerHandler","e"+callBackCount);

                                    mChecksum = 0;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 5:     //灯光 电机1 位置状态
                                    if (array[i] != -1) {       //判断是否为有效数据
                                        if (isReturnTime) {
                                            minuter = String.valueOf(Integer.toHexString(array[i]));
                                            if (minuter.length() < 2)
                                                minuter = "0" + minuter;
                                        }
                                        mTransientLightMotorStatus = array[i];
                                    } else
                                        mTransientLightMotorStatus = 0;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 6:     //对焦 电机2位置 bit[31:24]
                                    mTransientFocusMotorStatus = 0;
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        if (isReturnTime) {
                                            hours = String.valueOf(Integer.toHexString(array[i]));
                                            if (hours.length() < 2)
                                                hours = "0" + hours;
                                        }
                                        mTransientFocusMotorStatus += ((array[i] << 24) & 0x00000000FF000000);
                                        //                                        Log.w(TAG, "bit[31:24] = " + array[i] + " , " + (~(array[i] & 0xFF)) + " , " + ((array[i] << 24) & 0x00000000FF000000) + " , " + (((array[i] & 0xFF) << 24) & 0xFF000000) + " , " + (array[i] << 24) + " , " + ((array[i] << 24) & 0xFF000000));
                                        //                                    mTransientFocusMotorStatus &= 0x7FFFFFFF;
                                    } else {
                                        mTransientFocusMotorStatus &= 0x00FFFFFF;
                                    }
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 7:     //对焦 电机2位置 bit[23:16]
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        if (isReturnTime) {
                                            day = String.valueOf(Integer.toHexString(array[i]));
                                            if (day.length() < 2)
                                                day = "0" + day;
                                        }
                                        mTransientFocusMotorStatus += ((array[i] << 16) & 0x0000000000FF0000);
                                        //                                        Log.w(TAG, "bit[23:16] = " + array[i] + " , " + (array[i] & 0xFF) + " , " + ((array[i] << 16) & 0x0000000000FF0000) + " , " + (((array[i] & 0xFF) << 16) & 0x00FF0000) + " , " + (array[i] << 16) + " , " + ((array[i] << 16) & 0x00FF0000));
                                        //                                    mTransientFocusMotorStatus &= 0x7FFFFFFF;
                                    } else {
                                        mTransientFocusMotorStatus &= 0xFF00FFFF;
                                    }
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 8:     //对焦 电机2位置 bit[15:8]
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        if (isReturnTime) {
                                            month = String.valueOf(Integer.toHexString(array[i]));
                                            if (month.length() < 2)
                                                month = "0" + month;
                                        }
                                        mTransientFocusMotorStatus += ((array[i] << 8) & 0x000000000000FF00);
                                        //                                        Log.w(TAG, "bit[15:8] = " + array[i] + " , " + (array[i] & 0xFF) + " , " + ((array[i] << 8) & 0x000000000000FF00) + " , " + (((array[i] & 0xFF) << 8) & 0x0000FF00) + " , " + (array[i] << 8) + " , " + ((array[i] << 8) & 0x0000FF00));
                                        //                                    mTransientFocusMotorStatus &= 0x7FFFFFFF;
                                    } else {
                                        mTransientFocusMotorStatus &= 0xFFFF00FF;
                                    }
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 9:     //对焦 电机2位置 bit[7:0]
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        if (isReturnTime) {
                                            year = String.valueOf(Integer.toHexString(array[i]));
                                            if (year.length() < 2)
                                                year = "0" + year;
                                        }
                                        mTransientFocusMotorStatus += ((array[i] << 0) & 0x00000000000000FF);
                                        //                                        Log.w(TAG, "bit[7:0] = " + array[i] + " , " + (array[i] & 0xFF) + " , " + ((array[i] << 0) & 0x00000000000000FF) + " , " + (((array[i] & 0xFF) << 0) & 0x000000FF) + " , " + (array[i] << 0) + " , " + ((array[i] << 0) & 0x000000FF));
                                        //                                    mTransientFocusMotorStatus &= 0xFFFFF;
                                        //                                    mTransientFocusMotorStatus &= 0xFFFFFFFF;
                                    } else {
                                        mTransientFocusMotorStatus &= 0xFFFFFF00;
                                    }
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 10:    //物镜 电机3位置bit[15:8]
                                    mTransientLensMotorStatus = 0;
                                    if (array[i] != -1) {        //判断是否为有效数据

                                        mTransientLensMotorStatus += ((array[i] << 8) & 0x000000000000FF00);
                                        //                                        Log.w(TAG, "B bit[15:8] = " + array[i] + " , " + (array[i] & 0xFF) + " , " + ((array[i] << 8) & 0x000000000000FF00) + " , " + (((array[i] & 0xFF) << 8) & 0x0000FF00) + " , " + (array[i] << 8) + " , " + ((array[i] << 8) & 0x0000FF00));
                                        //                                    mTransientLensMotorStatus &= 0xFFFF;
                                    } else {
                                        mTransientLensMotorStatus &= 0xFFFF00FF;
                                    }
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 11:    //物镜 电机3位置bit[7:0]
                                    if (array[i] != -1) {        //判断是否为有效数据

                                        mTransientLensMotorStatus += ((array[i] << 0) & 0x00000000000000FF);

                                    } else {
                                        mTransientLensMotorStatus &= 0xFFFFFF00;
                                    }
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 12:        //光电开关触发状态(6个) :  1：打开  0：关闭
                                    /**
                                     * bit0 : m1p1
                                     * bit1 : m1p2
                                     * bit2 : m2p1
                                     * bit3 : m2p2
                                     * bit4 : m3p1
                                     * bit5 : m3p2
                                     */
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        if ((array[i] & 0x01) == 0x01)    //LIGHTSWITCH1 :bit0  1：打开; 0：关闭
                                            LIGHTSWITCH1_ON_FLAG_T = true;
                                        else
                                            LIGHTSWITCH1_ON_FLAG_T = false;
                                        if ((array[i] & 0x02) == 0x02)     //LIGHTSWITCH2 :bit1  1：打开; 0：关闭
                                            LIGHTSWITCH2_ON_FLAG_T = true;
                                        else
                                            LIGHTSWITCH2_ON_FLAG_T = false;
                                        if ((array[i] & 0x04) == 0x04)     //对焦电机0位置 LIGHTSWITCH3 :bit2  1：打开; 0：关闭
                                            LIGHTSWITCH3_ON_FLAG_T = true;
                                        else
                                            LIGHTSWITCH3_ON_FLAG_T = false;
                                        if ((array[i] & 0x08) == 0x08)    //对焦电机Max位置 LIGHTSWITCH4 :bit3  1：打开; 0：关闭
                                            LIGHTSWITCH4_ON_FLAG_T = true;
                                        else
                                            LIGHTSWITCH4_ON_FLAG_T = false;
                                        if ((array[i] & 0x10) == 0x10)    //物镜电机右端位置 LIGHTSWITCH5 :bit3  1：打开; 0：关闭
                                            LIGHTSWITCH5_ON_FLAG_T = true;
                                        else
                                            LIGHTSWITCH5_ON_FLAG_T = false;
                                        if ((array[i] & 0x20) == 0x20)    //物镜电机左端位置 LIGHTSWITCH6 :bit3  1：打开; 0：关闭
                                            LIGHTSWITCH6_ON_FLAG_T = true;
                                        else
                                            LIGHTSWITCH6_ON_FLAG_T = false;
                                    } else {
                                        LIGHTSWITCH1_ON_FLAG_T = false;
                                        LIGHTSWITCH2_ON_FLAG_T = false;
                                        LIGHTSWITCH3_ON_FLAG_T = false;
                                        LIGHTSWITCH4_ON_FLAG_T = false;
                                        LIGHTSWITCH5_ON_FLAG_T = false;
                                        LIGHTSWITCH6_ON_FLAG_T = false;
                                    }
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    //                                    Log.w(TAG, "  case 12 ! === ");
                                    break;
                                case 13:        //电压整数部份 ;
                                    if (array[i] != -1)         //判断是否为有效数据
                                        mTransientVoltageValue = array[i];
                                    else
                                        mTransientVoltageValue = 0;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    //                                    Log.w(TAG, "  case 13 ! === ");
                                    break;
                                case 14:        //电压小数点后一位 ;
                                    if (array[i] != -1)         //判断是否为有效数据
                                        mTransientVoltageValue += (array[i] / 10.0f);
                                    else
                                        mTransientVoltageValue += 0;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    //    Log.w(TAG, "  case 14 ! === ");
                                    break;
                                case 15:        //保留 ;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    //                                    Log.w(TAG, "  case 15 ! === ");
                                    break;
                                case 16:        //保留 ;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    //                                    Log.w(TAG, "  case 16 ! === ");
                                    break;
                                case 17:        //保留 ;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    //                                    Log.w(TAG, "  case 17 ! === ");
                                    break;

                                case 18:        //校验和bit[15:8] ;
                                    mTransientChecksum = 0;
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        mTransientChecksum += ((array[i] << 8) & 0x000000000000FF00);
                                    } else {
                                        mTransientChecksum &= 0xFFFF00FF;
                                    }
                                    //         Log.w(TAG, "  case 18 ! === ");
                                    break;
                                case 19:        //校验和bit[7:0] ;

                                    if (array[i] != -1) {        //判断是否为有效数据
                                        mTransientChecksum += ((array[i] << 0) & 0x00000000000000FF);
                                    } else {
                                        mTransientChecksum &= 0xFFFFFF00;
                                    }

                                    if (mChecksum == mTransientChecksum)    //判断数据校验和 ;
                                        mCheckSumFlag = true;
                                    else
                                        mCheckSumFlag = false;

                                    if (!mCheckSumFlag) {
                                        // Log.e(TAG, "mChecksum = " + mChecksum + " , mTransientChecksum = " + mTransientChecksum);
                                    }

                                    mChecksum = 0;
                                    //                                    Log.w(TAG, "  case 19 ! === ");
                                    break;
                                case 20:        //回车\r 0x0d
                                    if (array[i] == 0x0d)      //回车\r 0x0d
                                        mDataFramesFinishFlag = true;
                                    else
                                        mDataFramesFinishFlag = false;

                                    if (!mDataFramesFinishFlag) {
                                        // Log.e(TAG, "array[i] != 0x0d = " + array[i]);
                                    }

                                    mChecksum = 0;
                                    //                                    Log.w(TAG, "  case 20 ! === ");
                                    break;
                                case 21:        //换行\n  0x0a
                                    if (mDataFramesFinishFlag) {
                                        if (array[i] == 0x0a)      //换行\n  0x0a
                                            mDataFramesFinishFlag = true;
                                        else
                                            mDataFramesFinishFlag = false;
                                    }
                                    if (!mDataFramesFinishFlag) {
                                        // Log.e(TAG, "array[i] != 0x0a = " + array[i]);
                                    }
                                    mChecksum = 0;
                                    //                                    Log.w(TAG, "  case 21 ! === ");
                                case 22:

                                    if (mDataFramesFinishFlag & mCheckSumFlag) {

                                        LED1_ON_FLAG = LED1_ON_FLAG_T;  //LED1 开关标识;  (白光 1)
                                        LED2_ON_FLAG = LED2_ON_FLAG_T;  //LED2 开关标识;  (蓝光 2)
                                        LED3_ON_FLAG = LED3_ON_FLAG_T;  //LED3 开关标识;  (绿光 3)
                                        LED4_ON_FLAG = LED4_ON_FLAG_T;  //LED4 开关标识;  (紫光 4)

                                        LIGHTSWITCH1_ON_FLAG = LIGHTSWITCH1_ON_FLAG_T;  //光电开关1 开关标识;  (激发块复位位置开关状态)
                                        LIGHTSWITCH2_ON_FLAG = LIGHTSWITCH2_ON_FLAG_T;  //光电开关2 开关标识; (激发块开关状态)
                                        LIGHTSWITCH3_ON_FLAG = LIGHTSWITCH3_ON_FLAG_T;  //光电开关3 开关标识; (对焦电机0位置)
                                        LIGHTSWITCH4_ON_FLAG = LIGHTSWITCH4_ON_FLAG_T;  //光电开关4 开关标识; (对焦电机Max位置)
                                        LIGHTSWITCH5_ON_FLAG = LIGHTSWITCH5_ON_FLAG_T;  //光电开关5 开关标识; (物镜电机右端位置)
                                        LIGHTSWITCH6_ON_FLAG = LIGHTSWITCH6_ON_FLAG_T;  //光电开关6 开关标识; (物镜电机左端位置)

                                        mVoltageValue = mTransientVoltageValue;             //AD电压值
                                        mLightMotorStatus = mTransientLightMotorStatus;     //灯光电机1 位置状态
                                        mLensMotorStatus = mTransientLensMotorStatus;       //物镜电机2 位置状态
                                        mFocusMotorStatus = mTransientFocusMotorStatus;     //对焦电机3 位置状态

                                        if (LIGHTSWITCH4_ON_FLAG) {
                                            mMaxFocusMotorStatus = mFocusMotorStatus;      //对焦电机3 位置最大值
                                        }
                                    } else {
                                        // Log.e(TAG, "mDataFramesFinishFlag = " + mDataFramesFinishFlag + " , mCheckSumFlag = " + mCheckSumFlag);
                                    }
                                    mCheckSumFlag = false;
                                    mDataFramesFinishFlag = false;

                                    mDataFramesHeadFlag = false;
                                    mCurrentRecByteCount = 0;

                                    mTransientVoltageValue = 0;
                                    mTransientLightMotorStatus = 0;
                                    mTransientFocusMotorStatus = 0;
                                    mTransientLensMotorStatus = 0;

                                    mRecByteCountSecure = 0;
                                    mDataFramesSecureFlag = false;
                                    if (isReturnTime) {
                                        int myear = 0;
                                        timercount++;
                                        synchronized (swithObject) {
                                            objectiveIsSwith = false;
                                        }
                                        if (!year.equals("")) {

                                            if (isNumeric(year)) {
                                                //FileUtils.writeFileToLogFolder("-----------------------年份有效：");
                                                Log.e("CameraServerHandler", "year=" + year + "   month=" + month + "  day=" + day);
                                                objectiveIsSwith = false;
                                                if (lastmonth.equals(month) && lastday.equals(day) && year.equals(lastyear)) {
                                                    Log.e("CameraServerHandler", "设置系统时间==" + "20" + year + month + day + "." + hours + minuter + secode);
                                                    FileUtils.writeFileToLogFolder("设置系统时间==" + "20" + year + month + day + "." + hours + minuter + secode);
                                                    myear = Integer.valueOf(year);
                                                    if (myear >= 12 && myear < 100) {//2100年后出错

                                                        isReturnTime = false;
                                                        isdebug = true;
                                                        if (requesRootPermission(mContext.getPackageCodePath()) && ConstantUtil.TimerModel) {

                                                            Log.e("CameraServerHandler", "设置系统时间");

                                                            String timestr = "20" + year + month + day + "." + hours + minuter + secode;

                                                            useTheDate("20" + year + month + day);
                                                            FileUtils.writeFileToLogFolder("-----------------------串口接收到的时间：" + timestr);
                                                            setTimerZ();
                                                            setTime(timestr);

                                                        }
                                                    }
                                                } else {
                                                    //
                                                    sendStringToUart("4A504C591200000000000000");
                                                    synchronized (swithObject) {
                                                        removeMessages(MSG_RELEASE_SWITH);
                                                        sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
                                                        objectiveIsSwith = true;
                                                    }
                                                }
                                                lastyear = year;
                                                lastday = day;
                                                lastmonth = month;
                                            }
                                        } else {
                                            //  Log.w("mytestuartmessege","........................str12");
                                            sendStringToUart("4A504C591200000000000000");
                                            synchronized (swithObject) {
                                                removeMessages(MSG_RELEASE_SWITH);
                                                sendMessageDelayed(obtainMessage(CameraServerHandler.MSG_RELEASE_SWITH), 8000);
                                                objectiveIsSwith = true;
                                            }

                                        }
                                        if (timercount > 10) {
                                            isReturnTime = false;
                                            isdebug = true;
                                            synchronized (swithObject) {
                                                objectiveIsSwith = false;
                                            }
                                        }
                                    }

                                    //为状态赋值 :
                                    mGamma = mFocusMotorStatus;
                                    mSaturation = mLightMotorStatus;
                                    SPUtils.put(mContext, "LED1(W)", LED1_ON_FLAG);
                                    SPUtils.put(mContext, "LED2(B)", LED2_ON_FLAG);
                                    SPUtils.put(mContext, "LED3(G)", LED3_ON_FLAG);
                                    SPUtils.put(mContext, "LED4(P)", LED4_ON_FLAG);


                                    SPUtils.put(mContext, "LIGHTSWITCH1(reset)", LIGHTSWITCH1_ON_FLAG);
                                    SPUtils.put(mContext, "LIGHTSWITCH2(light)", LIGHTSWITCH2_ON_FLAG);
                                    SPUtils.put(mContext, "LIGHTSWITCH3(focus0)", LIGHTSWITCH3_ON_FLAG);
                                    SPUtils.put(mContext, "LIGHTSWITCH4(focus+)", LIGHTSWITCH4_ON_FLAG);
                                    SPUtils.put(mContext, "LIGHTSWITCH5(lens_r)", LIGHTSWITCH5_ON_FLAG);
                                    SPUtils.put(mContext, "LIGHTSWITCH6(lens_l)", LIGHTSWITCH6_ON_FLAG);

                                    SPUtils.put(mContext, "VoltageValue", mVoltageValue);
                                    SPUtils.put(mContext, "LightMotorStatus", mLightMotorStatus);
                                    SPUtils.put(mContext, "LensMotorStatus", mLensMotorStatus);
                                    SPUtils.put(mContext, "FocusMotorStatus", mFocusMotorStatus, true);
                                    SPUtils.put(mContext, "MaxFocus", mMaxFocusMotorStatus);
                                    //Log.e("CameraServerHandler","mVoltageValue="+mVoltageValue);
                                    //如果电压小于或者等于10%V则关机
                                    float level = (float) (((mVoltageValue - 9.6f) * 100) / (12.0 - 9.6f));
                                    if (level > 100)
                                        level = 100;
                                    if (level < 0)
                                        level = 0;
                                    mVoltageLevel = level;
                                   /*
                                    if(level<=10&&level>1){
                                        try {
                                            Process process = Runtime.getRuntime().exec("su");
                                            DataOutputStream out = new DataOutputStream(
                                                    process.getOutputStream());
                                            out.writeBytes("reboot -p\n");
                                            out.writeBytes("exit\n");
                                            out.flush();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }*/
                                    if (mVoltageValue > 12.0f) {
                                        mVoltageValue = 12.0f;
                                    }
                                    if (mVoltageValue < 9.6f) {
                                        mVoltageValue = 9.6f;
                                    }
                                    Intent intent = new Intent(CameraServerHandler.ACTION_STATUS_CHANGED);
                                    //    final int level = (currentLevel * 100) / scale;
                                    intent.putExtra(BatteryManager.EXTRA_LEVEL, mVoltageValue * 1.0f);
                                    intent.putExtra(BatteryManager.EXTRA_SCALE, 12 * 1.0f);
                                    intent.putExtra("phonenumber", CameraServerHandler.phone_number);
                                    mContext.sendBroadcast(intent);

                                    Log.i(TAG, dataFramesString);
                                    return dataFramesString;
                                default:
                                    mRecByteCountSecure = 0;
                                    mDataFramesSecureFlag = false;
                                    break;
                            }
                            mCurrentRecByteCount++;
                            //                            Log.w(TAG, "mCurrentRecByteCount " + mCurrentRecByteCount + " , mChecksum = " + mChecksum + " === ");
                        } else {
                            mDataFramesHeadFlag = false;
                            mCurrentRecByteCount = 0;
                        }
                    } else {
                        mDataFramesHeadFlag = false;
                        mCurrentRecByteCount = 0;
                    }
                    break;
            }
        }
        return "";
    }

    private String addSpace(String bankAccountNumber) {
        if (bankAccountNumber == null) {
            return "";
        }
        char[] strs = bankAccountNumber.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strs.length; i++) {
            sb.append(strs[i]);
            if (i == 3 || i == 5) {
                sb.append("-");

            }
        }

        String trim = sb.toString().trim() + " 00:00:00";
        return trim;

    }

    private void useTheDate(String timestr) {

        //  String s = addSpace("20180329");
        // long millis = MDateUtils.Date2ms(s);

        PackageManager packageManager = mContext.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //应用装时间
        long firstInstallTime = packageInfo.firstInstallTime;
        Log.w("Timer", "firstInstallTime=" + firstInstallTime);
        long currentmillis = MDateUtils.Date2ms(addSpace(timestr));
        Log.w("Timer", "currentmillis=" + currentmillis);
        Log.w("Timer", "(currentmillis-millis)/1000=" + (currentmillis - firstInstallTime) / 1000);
        Log.w("Timer", "ConstantUtil.StartLongSeconds=" + ConstantUtil.StartLongSeconds);
        if (ConstantUtil.IS_DEBUG) {
            if ((currentmillis - firstInstallTime) / 1000 > ConstantUtil.StartLongSeconds) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }

    }

    private boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    //请求root权限
    private boolean requesRootPermission(String packCodePath) {

        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd = "chmod 777" + packCodePath;
            process = Runtime.getRuntime().exec("su");//切换到root账号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

            try {
                if (os != null)
                    os.close();
                process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void setTimerZ() {
        AlarmManager mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        mAlarmManager.setTimeZone("Asia/Shanghai");
    }

    //例如：20170626.121212
    private boolean setTime(String time) {
        Log.e("CameraServerHandler", "设置系统时间=" + time);
        Process process = null;
        DataOutputStream os = null;
        try {

            process = Runtime.getRuntime().exec("su");//切换到root账号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("date -s" + time + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

            try {
                if (os != null)
                    os.close();
                process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}