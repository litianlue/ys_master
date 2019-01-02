package com.yeespec.libuvccamera.uvccamera.service;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: UVCService.java
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
 * 相机所有功能都在这里，实现相机和平板之间的通讯s
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVSaveOption;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.GetCallback;
import com.avos.avoscloud.SaveCallback;
import com.orhanobut.hawk.Hawk;
import com.yeespec.R;
import com.yeespec.libuvccamera.usb.DeviceFilter;
import com.yeespec.libuvccamera.usb.IFrameCallback;
import com.yeespec.libuvccamera.usb.USBMonitor;
import com.yeespec.libuvccamera.usb.USBMonitor.OnDeviceConnectListener;
import com.yeespec.libuvccamera.usb.USBMonitor.UsbControlBlock;
import com.yeespec.libuvccamera.usb.UVCCamera;
//import com.yeespec.libuvccamera.uvccamera.serviceclient.CameraHandler;
import com.yeespec.libuvccamera.uvccamera.glutils.JniUtils;
import com.yeespec.microscope.master.activity.MasterActivity;
import com.yeespec.microscope.master.activity.OnePiexlActivity;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.master.service.MasterIntentService;
//import com.yeespec.microscope.master.service.power.PowerService;
import com.yeespec.microscope.master.service.server.http.HttpServer;
import com.yeespec.microscope.master.service.server.socket.NettySocketServer;
import com.yeespec.microscope.master.service.server.websocket.CustomWebSocketServer;
import com.yeespec.microscope.master.service.server.websocket.ServerChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.CameraOperationChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.CameraOptionsChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.ClientNumberChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.DeviceOperationControlChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.DeviceStatusChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.SynchronousScreenChannel;
import com.yeespec.microscope.master.service.server.websocket.model.DeviceStatusModel;
import com.yeespec.microscope.utils.ByteZip;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.PictureUtils;
import com.yeespec.microscope.utils.RequesUtils;
import com.yeespec.microscope.utils.SPHelper;
import com.yeespec.microscope.utils.SPUtils;
import com.yeespec.microscope.utils.SettingUtils;
import com.yeespec.microscope.utils.bluetooth.DataUtil;
import com.yeespec.microscope.utils.detector.ImageGradient;
import com.yeespec.microscope.utils.log.Logger;

import org.java_websocket.exceptions.WebsocketNotConnectedException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.util.FloatMath.sqrt;
import static com.yeespec.R.string.what;
import static com.yeespec.libuvccamera.uvccamera.service.CameraServerHandler.LightCount;
import static com.yeespec.libuvccamera.uvccamera.service.CameraServerHandler.LightType;
import static com.yeespec.libuvccamera.uvccamera.service.CameraServerHandler.MESSAGE_REFRESH;
import static com.yeespec.libuvccamera.uvccamera.service.CameraServerHandler.REMOTE_LOGIN;
import static com.yeespec.libuvccamera.uvccamera.service.CameraServerHandler.mobjects;
import static com.yeespec.microscope.master.service.MasterIntentService.EXECUTOR_SERVICE_SCHEDULED;

/**
 * 实现com.serenegiant.usb的IFrameCallback.aidl远程接口;
 * 实现com.serenegiant.service的IUVCService.aidl远程接口;
 * 实现com.serenegiant.service的IUVCSlaveService.aidl远程接口;
 * <p/>
 * 此外 ,调用了libuvccamera的com.serenegiant.usb.USBMonitor ;
 */

public class UVCService extends Service {
    private boolean ISENBLE_REMOTE=false;
    private boolean  []CheckNums = new boolean[DataUtil.CONTRACKCOUNT];//记录已选择的对照组
    private boolean isExport = false;
    private int photosize=0;
    private int mediosize=0;
    private boolean isStopProcess = false;
    private String autophotoview = "";
    private String checkAutoFocusView = "";
    private String reCodingTimer="";
    public static String rockerState ="yes";

    private boolean isStopAutoPhoto=false;
    private boolean isFill=true;
    private int processBarAutoPhoto=0;
    private int autoPhotoCount =0;
    private String stopAutoPhotoStr="";
    private String autoPhotoCNumber="50";
    private String intevalAndPercentageStr;

    public static int maxbrightness;
    public static int maxiso;
    private static final boolean DEBUG = false;
    private static final String TAG = "test_UVCService";

    private int pTranlateX =0;
    private int pTranlateY =0;
    public static int  restartAppState= 0;
    //创建一个可重用固定线程数的线程池
    //public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(1);
    //2016.05.25 新增 :
    //创建一个固定线程数的延时线程池
    public static final ScheduledExecutorService EXECUTOR_SERVICE_SCHEDULED = Executors.newScheduledThreadPool(3);
    public static final ScheduledExecutorService RESTART_SERVICE_SCHEDULED = Executors.newScheduledThreadPool(3);

    //定义与MasterActivity通讯OnBind方法返回的对象 :
    private UVCServiceDataBinder mUVCServiceDataBinder = new UVCServiceDataBinder();

    //2016.09.21 新增 : 通知条的ID :
    public static final int NOTIFICATION_ID = 0x124;   //通知条的ID号 ;

    //====================================================================

    // 屏幕同步WEB_SOCKET
    public static final CustomWebSocketServer WEB_SOCKET_PICTURE_SERVER;
    public static NettySocketServer TCP_SERVER;
    // 手机通过服务中转对相机操作WEB_SOCKET
    public static final CustomWebSocketServer WEB_SOCKET_OPTIONS_SERVER;
    // 设备状态获取设备状态获取WEB_SOCKET
    public static final CustomWebSocketServer WEB_SOCKET_STATUS_SERVER;
    // 服务对相机操作WEB_SOCKET
    public static final CustomWebSocketServer WEB_SOCKET_OPERATION_SERVER;
    // 手机对相机的操作状态进行监听，监听是否进行屏蔽WEB_SOCKET
    public static final CustomWebSocketServer WEB_SOCKET_OPERATION_CONTROL_SERVER;
    // 获取客户端数量WEB_SOCKET
    public static final CustomWebSocketServer WEB_SOCKET_CLIENT_NUMBER_SERVER;

    public static final HttpServer HTTP_SERVER;

    private static USBMonitor mUSBMonitor;

    public int level;
    static {

        WEB_SOCKET_PICTURE_SERVER = CustomWebSocketServer.getInstance(CustomWebSocketServer.PORT1);
        WEB_SOCKET_OPTIONS_SERVER = CustomWebSocketServer.getInstance(CustomWebSocketServer.PORT2);
        WEB_SOCKET_STATUS_SERVER = CustomWebSocketServer.getInstance(CustomWebSocketServer.PORT3);
        WEB_SOCKET_OPERATION_SERVER = CustomWebSocketServer.getInstance(CustomWebSocketServer.PORT4);
        WEB_SOCKET_OPERATION_CONTROL_SERVER = CustomWebSocketServer.getInstance(CustomWebSocketServer.PORT5);
        WEB_SOCKET_CLIENT_NUMBER_SERVER = CustomWebSocketServer.getInstance(CustomWebSocketServer.PORT6);
        HTTP_SERVER = HttpServer.getInstance(HttpServer.PORT);
        EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
            @Override
            public void run() {
                TCP_SERVER = new NettySocketServer(9092);
            }
        },0,TimeUnit.MILLISECONDS);
       /* EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                TCP_SERVER = new NettySocketServer(9092);
            }
        });*/
    }
    //电量改变监听 : 通过getBatteryPercentage()动态注册 :
    private BroadcastReceiver mBatteryLevelReceiver;

    //2016.05.31 新增 : 用于记录当前采样图像帧中采样的像素的 灰度最小 和 灰度最大值 ; 用于反差式对焦的计算 :
    private int mMinium = 0;
    private int mMaximal = 0;

    //2016.05.31 新增 : 触摸点映射到图像Bitmap的比例 :
    private float mScaleX = 0.5f;   //初始值为0.5 , 即默认采样中心位置 : 采样大小为100*100的像素块 :
    private float mScaleY = 0.5f;

    //2016.06.01 新增 :
    public int mLayerMinium = 0;
    public int mLayerMaximal = 0;

    //最终的最大反差值 :
    public int mContrast = 0;

    //2016.07.02 : 新增 :     用于统计当前视场的灰白像素比 :
    public int mWhiteCount = 0;
    public int mGrayCount = 0;
    //public static int THRESHOLD_VALUE = 100; //用于灰白像素划分的阈值 ;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private static ScheduledExecutorService mThreadPool = Executors.newScheduledThreadPool(1);
    private List<UsbDevice> deviceList = null;
    private UsbControlBlock ctrlBlock = null;

    public UVCService() {

    }


    private void destroyUVserviece() {
        mServiceHandler.removeMessages(what);
        mByteArrayBuffer = null;
        mPixelsArrayBuffer = null;
        if (!BITMAP.isRecycled()&&BITMAP!=null) {
            BITMAP.recycle();
        }
//        Intent start = new Intent(UVCService.this, PowerService.class);
//        stopService(start);
        if (checkReleaseService()) {
            if (mUSBMonitor != null) {
                mUSBMonitor.unregister();
                mUSBMonitor = null;
            }
        }

        //2016.05.25 新增 : 用于注销动态注册的电量改变广播监听器 :
        if (mBatteryLevelReceiver != null) {
            unregisterReceiver(mBatteryLevelReceiver);
        }

        //2016.09.21 :　删除通知条 ;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
    }

    private float scale;
    private void greadUserParamter(){
        if(CameraServerHandler.userName==null||CameraServerHandler.userName.equals("")){
            return;
        }
       // Log.d(TAG, "CameraServerHandler.userName="+CameraServerHandler.userName);
        //查询是否有该用户记录，没有则创建一条
        AVQuery<AVObject> query = new AVQuery<>(RequesUtils.PARAMETER);
        query.whereEqualTo("user",CameraServerHandler.userName);
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                Log.d(TAG, "e===="+e +"list="+list);
                if(e==null) {
                    if (list.size() < 1) {
                        addUserParameter();
                    }
                }else if(e.equals("com.avos.avoscloud.AVException: java.net.SocketTimeoutException: timeout")) {
                    addUserParameter();
                }
            }
        });
        AVQuery<AVObject> cfg = new AVQuery<>(RequesUtils.CONFIGURATION);
        cfg.whereEqualTo("user",CameraServerHandler.userName);
        cfg.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {

                if(e==null) {
                    if (list.size() < 1) {
                        addUserConfiguration();
                    }else {
                        updataUserConfiguration();
                    }
                }else if(e.equals("com.avos.avoscloud.AVException: java.net.SocketTimeoutException: timeout")) {
                    addUserConfiguration();
                }
            }
        });

    }
    //添加用户配置
    private void addUserConfiguration(){


        CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
        AVObject avObject = new AVObject(RequesUtils.CONFIGURATION);
        avObject.put("user", CameraServerHandler.userName);
        avObject.put("light_number", ""+ LightCount);
        avObject.put("light_type", ""+ LightType);
        avObject.put("maxbrightness",serverHandler.getMaxBrightness());
        avObject.put("maxiso",serverHandler.getMaxNuberType(2));
        avObject.put("lonza",ConstantUtil.LONZA);
        avObject.put("rockerstate",rockerState);
        StringBuffer constansstr = new StringBuffer();
        for (int i = 0; i < CameraServerHandler.mobjects.length; i++) {
            if(i==(CameraServerHandler.mobjects.length-1)){
                constansstr.append(CameraServerHandler.mobjects[i]+"");
            }else{
                constansstr.append(CameraServerHandler.mobjects[i]+",");
            }

        }
        avObject.put("constans",constansstr.toString());
        Log.d(TAG, "constansstr"+constansstr.toString() );
        constansstr = null;
        avObject.saveInBackground();

    }
    private void updataUserConfiguration(){
        final CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
        AVQuery<AVObject> query = new AVQuery<>(RequesUtils.CONFIGURATION);
        query.whereEqualTo("user",CameraServerHandler.userName);
        query.getFirstInBackground(new GetCallback<AVObject>() {
            @Override
            public void done(final AVObject account, AVException e) {
                if(e==null) {
                    account.put("maxbrightness",serverHandler.getMaxBrightness());
                    account.put("maxiso",serverHandler.getMaxNuberType(2));
                    account.put("light_number", ""+ LightCount);
                    StringBuffer constansstr = new StringBuffer();
                    for (int i = 0; i < CameraServerHandler.mobjects.length; i++) {
                        if(i==(CameraServerHandler.mobjects.length-1)){
                            constansstr.append(CameraServerHandler.mobjects[i]+"");
                        }else{
                            constansstr.append(CameraServerHandler.mobjects[i]+",");
                        }

                    }
                    account.put("constans",constansstr.toString());
                    constansstr = null;
                    AVSaveOption option = new AVSaveOption();
                    option.query(new AVQuery<>(RequesUtils.CONFIGURATION).whereEqualTo("user", CameraServerHandler.userName));
                    option.setFetchWhenSave(true);
                    account.saveInBackground();
                }
            }
        });
    }
    private ImageGradient gradient;
    public static byte[] getBytesByBitmap(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
        return buffer.array();
    }
    public static char[] getChars (byte[] bytes) {
        Charset cs = Charset.forName ("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate (bytes.length);
        bb.put (bytes);
        bb.flip ();
        CharBuffer cb = cs.decode (bb);
        return cb.array();
    }
    public static int processId = 0;
    @Override
    public void onCreate() {
        super.onCreate();
        restartAppState++;
        //Log.w("lsdjfllslds","my"+Hawk.get("mytest",false));
        FileUtils.writeFileToLogFolder("UVCservice服务开启：onCreate");
        Boolean exception_count = Hawk.get("exception_count", false);
        Log.w("cameraclient", "exception_count=" +  exception_count);
        if(exception_count){
            FileUtils.writeFileToLogFolder("进程异常，重启");
            RestartUtil  restartUtil = new RestartUtil();
            restartUtil.killProcess();
        }

        if (mUSBMonitor == null) {

            gradient  = new ImageGradient();

            mUSBMonitor = new USBMonitor(getApplicationContext(), mOnDeviceConnectListener);

            if (mUSBMonitor != null) {
                final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(this, R.xml.device_filter_camera);
                mUSBMonitor.setDeviceFilter(filter);
            }
            mUSBMonitor.register();

            //2016.09.02 : 修改 : 直接在远程服务注册好usb设备 ; 默认注册设备号为0的usb ;
            deviceList = mUSBMonitor.getDeviceList();
            Log.v("mytestlist", "++++++++++++++++++查找到的USB设备总数："+deviceList.size());

            FileUtils.writeFileToLogFolder("++++++++++++++++++查找到的USB设备总数："+deviceList.size());
            if (deviceList.size() > 0) {

                for (int i = 0; i < deviceList.size(); i++) {
                    if(deviceList.get(i).getProductId()==CameraServerHandler.mProductId){
                        Log.v("mytestlist", "deviceList.get(i).getDeviceName().trim()="+deviceList.get(i).getDeviceName().trim());
                        CameraServerHandler.devicename = deviceList.get(i).getDeviceName().trim();
                    }
                }

                FileUtils.writeFileToLogFolder("+++++++++++++++++设置当前的设备："+deviceList.get(0)+"deviceList.get(0).getDeviceName()="+deviceList.get(0).getDeviceName());

                mUSBMonitor.requestPermission(deviceList.get(0));
                mUSBMonitor.setCurrentDevice(deviceList.get(0));

                ctrlBlock = new UsbControlBlock(mUSBMonitor, deviceList.get(0));

                for (UsbDevice usbDevice : deviceList) {
                    if(!mUSBMonitor.hasPermission(usbDevice)) {
                        getUsbPermission();
                        break;
                    }
                }
            }
            if (mServiceHandler == null) {
                mServiceHandler = CameraServerHandler.createServerHandler(getApplicationContext(), ctrlBlock, 0, 0, mISynchronousScreenFrameCallback);
                mServiceHandler.sendEmptyMessage(MESSAGE_REFRESH);
            }
        }

       // BaseApplication.setContext(UVCService.this);
        initChannel();
        RESTART_SERVICE_SCHEDULED.schedule(new Runnable() {
            @Override
            public void run() {
                greadUserParamter();//添加当前用户参数到后台
                Hawk.put("exception_count", true);
                FileUtils.writeFileToLogFolder("CameraClient：exception_count=true");
            }
        },3000,TimeUnit.MILLISECONDS);
//        EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
//            @Override
//            public void run() {
//                Hawk.put("exception_count", true);
//            }
//        }, 30 * 1000, TimeUnit.MILLISECONDS);
        getBatteryPercentage();

        SPUtils.put(this, "enable", true);

        //2016.09.19 : 申请获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行 ;
        acquireWakeLock();

        //2016.09.20 : 添加startForeground将进程设置为前台进程 ;
        //showForegroundNotification();
        scale = getApplicationContext().getResources().getDisplayMetrics().density;
        // startOnePixelActivity();

    }

    public String killProcess() {

        EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
            @Override
            public void run() {
                Intent LaunchIntent =getBaseContext().getPackageManager().getLaunchIntentForPackage(getApplication().getPackageName());
                LaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getBaseContext().startActivity(LaunchIntent);
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
                    Log.w("mytestprocess", "processname=" + procInfo.processName);
                    Log.w("mytestprocess", "processid=" + procInfo.pid);
                    Process.killProcess(procInfo.pid);
                    am.restartPackage(procInfo.processName);
                }
                System.gc();
                System.exit(0);
            }
        }, 1500, TimeUnit.MILLISECONDS);

        return null;
    }
    public void exceptionKillMethod() {
        RestartUtil  restartUtil = new RestartUtil();
        restartUtil.exceptionKillMethod(getBaseContext(),UVCService.processId);

    }


    private void getUsbPermission() {

        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                tryGetUsbPermission();
            }
        });

    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private  IntentFilter filter=null;
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static UsbManager mUsbManager;
    private void tryGetUsbPermission() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbPermissionActionReceiver, filter);

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        //here do emulation to ask all connected usb device for permission
        for (final UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {

            if (mUsbManager.hasPermission(usbDevice)) {
                afterGetUsbPermission(usbDevice);
            } else {
                mUsbManager.requestPermission(usbDevice, mPermissionIntent);
            }

        }
    }


    private void afterGetUsbPermission(UsbDevice usbDevice) {
        openUsbDevice(usbDevice);
    }

    private void openUsbDevice(UsbDevice usbDevice) {
        UsbDeviceConnection connection = mUsbManager.openDevice(usbDevice);
    }

    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                        if (null != usbDevice) {
                            afterGetUsbPermission(usbDevice);
                        }
                    } else {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
                        // Toast.makeText(context, String.valueOf("Permission denied for device" + usbDevice), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };
    private void startOnePixelActivity() {
        Intent it = new Intent(this, OnePiexlActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);
    }

    /* private void foregroundNotification(){
         //启用前台服务，主要是startForeground()
         Notification notification = new Notification(R.drawable.icon,"用电脑时间过长了！白痴！"
                 ,System.currentTimeMillis());
         notification.setLatestEventInfo(this,"Yeespec正在运行",
                 "Yeespec服务运行中",null);
         //设置通知默认效果
         notification.flags= Notification.FLAG_SHOW_LIGHTS;
         startForeground(1,notification);


         AlarmManager manager=(AlarmManager)getSystemService(ALARM_SERVICE);
         //读者可以修改此处的Minutes从而改变提醒间隔时间
         //此处是设置每隔1分钟启动一次
         int Minutes=1*60*1000;
          //SystemClock.elapsedRealtime()表示1970年1月1日0点至今所经历的时间
         long triggerAtTime=SystemClock.elapsedRealtime()+Minutes;
         //此处设置开启AlarmReceiver这个Service
         Intent i=new Intent(this,MasterActivity.class);
         PendingIntent pi=PendingIntent.getBroadcast(this,0,i,0);
          //ELAPSED_REALTIME_WAKEUP表示让定时任务的出发时间从系统开机算起，并且会唤醒CPU。
         manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);



     }*/
    private void showForegroundNotification() {
        //2016.09.20 : 添加startForeground将进程设置为前台进程 ;
        //        PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(this, MasterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(this, MasterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notify = new Notification.Builder(this)
                .setAutoCancel(false)
                .setTicker(getResources().getString(R.string.ticker_notify))
                .setContentTitle(getResources().getString(R.string.title_notify))
                .setContentText(getResources().getString(R.string.text1_notify))
                //                .setSmallIcon(R.mipmap.ic_microscope_cells_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pIntent)
                .build();

        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        //        startForeground(new Random().nextInt(100), notify);
        startForeground(NOTIFICATION_ID, notify);
      //  Log.w("test", TAG + " # showForegroundNotification() ");
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     *//*
    void stopForegroundCompat(int id) {
        if (mReflectFlg) {
            // If we have the new stopForeground API, then use it.
            if (mStopForeground != null) {
                mStopForegroundArgs[0] = Boolean.TRUE;
                invokeMethod(mStopForeground, mStopForegroundArgs);
                return;
            }

            // Fall back on the old API.  Note to cancel BEFORE changing the
            // foreground state, since we could be killed at that point.
            mNM.cancel(id);
            mSetForegroundArgs[0] = Boolean.FALSE;
            invokeMethod(mSetForeground, mSetForegroundArgs);
        } else {
            *//* 还可以使用以下方法，当sdk大于等于5时，调用sdk现有的方法stopForeground停止前台运行，
             * 否则调用反射取得的sdk level 5（对应Android 2.0）以下才有的旧方法setForeground停止前台运行 *//*

            if(Build.VERSION.SDK_INT >= 5) {
                stopForeground(true);
            } else {
                // Fall back on the old API.  Note to cancel BEFORE changing the
                // foreground state, since we could be killed at that point.
                mNM.cancel(id);
                mSetForegroundArgs[0] = Boolean.FALSE;
                invokeMethod(mSetForeground, mSetForegroundArgs);
            }
        }
    }*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //foregroundNotification();

        //  return START_REDELIVER_INTENT;
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        Log.v(TAG, "onStart--");

        //String currentTime = MediaMuxerWrapper.getDateTimeString();
        //设置通知内容并在onReceive()这个函数执行时开启
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //        Notification notification = new Notification(R.drawable.ic_launcher, "用电脑时间过长了！白痴！"
        //                , System.currentTimeMillis());
        //        notification.setLatestEventInfo(this, "快去休息！！！",
        //                "UVCService # onStart() ==== " + currentTime, null);
        //        notification.defaults = Notification.DEFAULT_ALL;

        //        PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(this, UVCService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setAutoCancel(false)
                .setTicker(getResources().getString(R.string.ticker_notify))
                .setContentTitle(getResources().getString(R.string.title_notify))
                .setContentText(getResources().getString(R.string.text1_notify))
                //                .setSmallIcon(R.mipmap.ic_microscope_cells_launcher)
                .setWhen(System.currentTimeMillis())
                //                .setContentIntent(pIntent)
                .build();

        //        manager.notify(new Random().nextInt(100), notification);
        manager.notify(NOTIFICATION_ID, notification);

        startScreenAlarmService(1 * 60);  //1分钟之后 ;


    }


    //开启待机定时唤醒服务
    public void startScreenAlarmService(int seconds) {
        //获取AlarmManager系统服务
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //包装需要执行Service的Intent
        //        Intent intent = new Intent(this, ScreenAlarmReceiver.class);
        //        Intent intent = new Intent(this, MasterIntentService.class);
        //        Intent intent = new Intent(this, GuardService.class);
        //        Intent intent = new Intent(this, UVCService.class);
        Intent intent = new Intent(this, MasterIntentService.class);
        //        Intent intent = new Intent(this, BaseActivity.class);
        //        intent.putExtra("startScreenAlarmService",true);
        //        PendingIntent pendingIntent = PendingIntent.getService(this, 0,intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //触发服务的起始时间
        long triggerAtTime = SystemClock.elapsedRealtime();

        //使用AlarmManger的setRepeating方法设置定期执行的时间间隔（seconds秒）和需要执行的Service
        //        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime,seconds * 1000, pendingIntent);
        //        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, seconds * 1000, pendingIntent);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime + seconds * 1000, pendingIntent);
        //        manager.set(AlarmManager.RTC_WAKEUP, triggerAtTime + seconds * 1000, pendingIntent);


    }


    /**
     * 计算电池百分比
     */
    protected void getBatteryPercentage() {
        mBatteryLevelReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (currentLevel >= 0 && scale > 0) {
                    level = (currentLevel * 100) / scale;
                }
            }
        };
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryLevelReceiver, batteryLevelFilter);
    }

    private void initChannel() {
        // 屏幕同步（完成）
        WEB_SOCKET_PICTURE_SERVER.attachChannel(SynchronousScreenChannel.CHANNEL_NAME, new SynchronousScreenChannel());
        // 手机通过服务中转对相机操作（完成）    特殊 ; 数据发送到本地 ;
        WEB_SOCKET_OPTIONS_SERVER.attachChannel(CameraOptionsChannel.CHANNEL_NAME, new CameraOptionsChannel());
        // 设备状态获取（完成）
        WEB_SOCKET_STATUS_SERVER.attachChannel(DeviceStatusChannel.CHANNEL_NAME, new DeviceStatusChannel());
        // 服务对相机操作（完成）
        WEB_SOCKET_OPERATION_SERVER.attachChannel(CameraOperationChannel.CHANNEL_NAME, new CameraOperationChannel());
        // 手机对相机的操作状态进行监听，监听是否进行屏蔽（完成）
        WEB_SOCKET_OPERATION_CONTROL_SERVER.attachChannel(DeviceOperationControlChannel.CHANNEL_NAME, new DeviceOperationControlChannel());
        // 获取客户端数量
        WEB_SOCKET_CLIENT_NUMBER_SERVER.attachChannel(ClientNumberChannel.CHANNEL_NAME, new ClientNumberChannel());
    }

    public static USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDestroy() {
        FileUtils.writeFileToLogFolder("UVCservice服务销毁：onDestroy");
       /* AlarmManager manager=(AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i =new Intent(this,MasterActivity.class);
        PendingIntent pi=PendingIntent.getBroadcast(this,0,i,0);
        manager.cancel(pi);*/
        showNotification("YEESPEC已退出");

        destroyUVserviece();
        if(filter!=null) {
            unregisterReceiver(mUsbPermissionActionReceiver);
            filter = null;
        }
        super.onDestroy();
    }

    private void showNotification(String info) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.mipmap.ic_microscope_cells_launcher, "通知", System.currentTimeMillis());
        Intent intent = new Intent(getApplicationContext(), MasterActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 100, intent, 0);
        notification.setLatestEventInfo(getApplicationContext(), "YEESPEC", info, contentIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        manager.notify(100, notification);
    }

    //2016.05.25 新增 :

    @Override
    public IBinder onBind(final Intent intent) {
        if (DEBUG)
            Log.d(TAG, "onBind:" + intent);

        if (IUVCService.class.getName().equals(intent.getAction())) {
          //  Log.w(TAG, "return mBasicBinder " + intent.getAction() + (mBasicBinder == null ? " null " : " not null "));
            return mBasicBinder;
        }

        //2016.05.25 新增 :
        //判断是否是绑定了MasterActivity :
        if (MasterActivity.class.getName().equals(intent.getAction())) {

            return mUVCServiceDataBinder;
        }
        //2016.06.23 新增 :
        //判断是否是绑定了MasterIntentService :
        if (MasterIntentService.class.getName().equals(intent.getAction())) {

            return mUVCServiceDataBinder;
        }
        return null;
    }

    @Override
    public void onRebind(final Intent intent) {

    }

    @Override
    public boolean onUnbind(final Intent intent) {
        if (DEBUG)
            Log.d(TAG, "onUnbind:" + intent);
        if (checkReleaseService() && mUSBMonitor != null) {
            mUSBMonitor.unregister();
            mUSBMonitor = null;
        }
        return true;
    }

    //public static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocateDirect(UVCCamera.DEFAULT_PREVIEW_WIDTH * UVCCamera.DEFAULT_PREVIEW_HEIGHT * 4);
    //    public static Bitmap BITMAP = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);//RGB565
    public static Bitmap BITMAP = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);//RGB565

    //  public static final Bitmap mBITMAP = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);//RGB565

    //2016.05.25 新增 : 用于获取图片的字节数据 :
    //    public static final ByteBuffer BYTE_BUFFER_ARRAY = ByteBuffer.allocateDirect(UVCCamera.DEFAULT_PREVIEW_WIDTH * UVCCamera.DEFAULT_PREVIEW_HEIGHT * 4);;

    public int mCountBitmap = 0;
    public int mStandardDeviation = 0;

    public int mByteCount = 0;
    public int mRowBytes = 0;
    public static int mWidth = 0;
    public static int mHeigth = 0;
    public int mSum = 0;
    public int mArrayLength = 0;
    public int mAvg = 0;
    public int mSDSum = 0;
    public int mSDSumAvg = 0;

    public static int[] mByteArrayBuffer = null;
    public static int[] mPixelsArrayBuffer = null;

    //public Bitmap mBitmap = null;

    //定义像素采样的步长 : mStepLength :
    //    public static int mStepLength = 16;
    public static int mStepLength = 1;

    //2016.06.01 新增 : 定义一个采样截取Bitmap大小的宽高 :
    public static int CAPTURE_WIDTH = 150;
    public static int CAPTURE_HEIGTH = 150;
    public static int CAPTURE_BYTE_COUNT = CAPTURE_WIDTH * CAPTURE_HEIGTH;


    private int NN = 8;
    private int MM = NN / 2;


    private ArrayList<Float> mFreshnes = new ArrayList<Float>();
    private int mUart_Gamma = 0;
    private int RUN_STIP = 10;
    private int interception_x = 0;
    private int interception_y = 0;
    private int valueCount1 = 0;
    private int valueCount = 0;
    private float lastFreshnes = 0;
    private float treeFreshnes = 0;
    private int mStip = 0;
    private int di_count = 0;
    private boolean lastRun = false;
    private boolean DIRECTION = true;
    private static boolean isAutoFocus = false;
    private boolean isStartFreshnes = true;
    private boolean isATsuccessful = false;
    private int COUNT_TO_RESULT = 0;
    private static ScheduledExecutorService mService = Executors.newScheduledThreadPool(1);

    private float OutLinCount(Bitmap bitmap) {

        //得到图像的宽度和长度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float tempgray;
        float blackcount = 0;
        float whitecount = 0;
        //依次循环对图像的像素进行处理
        for (int i = 0; i < width;i++ ) {

            for (int j = 0; j < height; j++) {
                //得到每点的像素值
                int col = bitmap.getPixel(i, j);
                // int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);


                tempgray = (float) (0.114 * blue + 0.587 * green + 0.299 * red);

                if(tempgray<128)
                   blackcount++;
                else
                    whitecount++;

            }
        }
        return whitecount;
    }
    private float mFreshnes_toAllII(Bitmap bitmap) {
        float result = 0;
        int WhiteCell[];
        //得到图像的宽度和长度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        WhiteCell = new int[width*height];

        float Whitegray = 0,whiteAvg = 0, tempgray;

        int whitecount = 0;
        //依次循环对图像的像素进行处理
        for (int i = 0; i < width;i++ ) {

            for (int j = 0; j < height; j++) {
                //得到每点的像素值
                int col = bitmap.getPixel(i, j);
               // int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);


                tempgray = (float) (0.114 * blue + 0.587 * green + 0.299 * red);
                if(tempgray>128)
                WhiteCell[whitecount++] = (int) tempgray;

            }
        }



        for (int i = 0; i < whitecount; i++) {
            Whitegray += WhiteCell[i];
        }
        if (whitecount != 0)
            whiteAvg = Whitegray / whitecount;

        float whiteS = 0;
        float xx = 0;

        xx = whiteAvg;//    x~=Egi/n
        for (int j = 0; j < whitecount; j++) {
            float b2 = WhiteCell[j] - xx;//xi-x~
            b2 = b2 * b2;//(xi-x~)^2
            whiteS = whiteS + b2;//E(xi-x~)^2
        }
        if (whitecount != 0)
            whiteS = whiteS / whitecount;
        if (whiteAvg == 0) {
            whiteS = 0;
        } else
            whiteS = sqrt(sqrt(whiteS)) / whiteAvg;

        result =  whiteS;

        return result;
    }
    private float mFreshnes_AVG(Bitmap bitmap) {
        float result = 0;
        int BlackCell[];
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        BlackCell = new int[width * height];

        float Blackgray = 0, blackAvg = 0, whiteAvg = 0, tempgray;
        int blackcount = 0;
        int whitecount = 0;


        //得到图像的宽度和长度


        //创建线性拉升灰度图像
        //依次循环对图像的像素进行处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //得到每点的像素值
                int col = bitmap.getPixel(i, j);

                //int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);


                tempgray = (float) (0.114 * blue + 0.587 * green + 0.299 * red);

                BlackCell[blackcount++] = (int) tempgray;


            }
        }
        for (int i = 0; i < blackcount; i++) {
            Blackgray += BlackCell[i];
        }
        if (blackcount != 0)
            blackAvg = Blackgray / blackcount;


        result = blackAvg;

        return result;
    }

   /* private float mFreshnes_toSelect(Bitmap bitmap, int bpp, int x, int y, int len) {
        float result = 0;
        int BlackCell[], WhiteCell[];
        BlackCell = new int[len * len];
        WhiteCell = new int[len * len];

        float Blackgray = 0, Whitegray = 0, blackAvg = 0, whiteAvg = 0, tempgray;
        int blackcount = 0;
        int whitecount = 0;


        //得到图像的宽度和长度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int startH, endH, startW, endW;
        if (y <= len / 2)
            startH = 0;
        else
            startH = y - len / 2;

        if (height - y - len / 2 <= 0)
            endH = height;
        else
            endH = y + len / 2;

        if (x - len / 2 <= 0)
            startW = 0;
        else
            startW = x - len / 2;

        if (width - x - len / 2 <= 0)
            endW = width;
        else
            endW = x + len / 2;

        //依次循环对图像的像素进行处理

        for (int i = startW; i < endW; i++) {

            for (int j = startH; j < endH; j++) {
                //得到每点的像素值
                int col = bitmap.getPixel(i, j);
                // int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);
                tempgray = (float) (0.114 * blue + 0.587 * green + 0.299 * red);
                //Log.w("UVCService","tempgray="+tempgray);
                if(tempgray<128)
                BlackCell[blackcount++] = (int) tempgray;
                else
                WhiteCell[whitecount++] = (int) tempgray;


            }
        }

        for (int i = 0; i < blackcount; i++) {
            Blackgray += BlackCell[i];
        }
        if (blackcount != 0)
            blackAvg = Blackgray / blackcount;

        for (int i = 0; i < whitecount; i++) {
            Whitegray += WhiteCell[i];
        }
        if (whitecount != 0)
            whiteAvg = Whitegray / whitecount;

        float blackS = 0, whiteS = 0;
        float xx = blackAvg;//    x~=Egi/n
        for (int j = 0; j < blackcount; j++) {
            float b1 = BlackCell[j] - xx;//xi-x~
            b1 = b1 * b1;//(xi-x~)^2
            blackS = blackS + b1;//E(xi-x~)^2
        }
        if (blackcount != 0)
            blackS = blackS / blackcount;
        if (blackAvg == 0) {
            blackS = 0;
        } else
            blackS = sqrt(sqrt(blackS)) / blackAvg;

        xx = whiteAvg;//    x~=Egi/n
        for (int j = 0; j < whitecount; j++) {
            float b2 = WhiteCell[j] - xx;//xi-x~
            b2 = b2 * b2;//(xi-x~)^2
            whiteS = whiteS + b2;//E(xi-x~)^2
        }
        if (whitecount != 0)
            whiteS = whiteS / whitecount;
        if (whiteAvg == 0) {
            whiteS = 0;
        } else
            whiteS = sqrt(sqrt(whiteS)) / whiteAvg;

        result = blackS + whiteS;

        return result;
    }
*/
    private float mFreshnes_toAll(Bitmap bitmap, int bpp, int x, int y, int len) {
        float result = 0;
        int BlackCell[], WhiteCell[];
        BlackCell = new int[22100];
        WhiteCell = new int[22100];

        float Blackgray = 0, Whitegray = 0, blackAvg = 0, whiteAvg = 0, tempgray;
        int blackcount = 0;
        int whitecount = 0;


        //得到图像的宽度和长度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        //创建线性拉升灰度图像
        Bitmap linegray = null;
        linegray = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        //依次循环对图像的像素进行处理
        for (int i = 0; i < width; ) {

            for (int j = 0; j < height; ) {
                //得到每点的像素值
                int col = bitmap.getPixel(i, j);


                int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);


                tempgray = (float) (0.114 * blue + 0.587 * green + 0.299 * red);
                if(tempgray<128)
                BlackCell[blackcount++] = (int) tempgray;
                else
                WhiteCell[whitecount++] = (int) tempgray;


                j = j + 8;


            }
            i = i + 8;
        }

        for (int i = 0; i < blackcount; i++) {
            Blackgray += BlackCell[i];
        }
        if (blackcount != 0)
            blackAvg = Blackgray / blackcount;

        for (int i = 0; i < whitecount; i++) {
            Whitegray += WhiteCell[i];
        }
        if (whitecount != 0)
            whiteAvg = Whitegray / whitecount;

        float blackS = 0, whiteS = 0;
        float xx = blackAvg;//    x~=Egi/n
        for (int j = 0; j < blackcount; j++) {
            float b1 = BlackCell[j] - xx;//xi-x~
            b1 = b1 * b1;//(xi-x~)^2
            blackS = blackS + b1;//E(xi-x~)^2
        }
        if (blackcount != 0)
            blackS = blackS / blackcount;
        if (blackAvg == 0) {
            blackS = 0;
        } else
            blackS = sqrt(sqrt(blackS)) / blackAvg;

        xx = whiteAvg;//    x~=Egi/n
        for (int j = 0; j < whitecount; j++) {
            float b2 = WhiteCell[j] - xx;//xi-x~
            b2 = b2 * b2;//(xi-x~)^2
            whiteS = whiteS + b2;//E(xi-x~)^2
        }
        if (whitecount != 0)
            whiteS = whiteS / whitecount;
        if (whiteAvg == 0) {
            whiteS = 0;
        } else
            whiteS = sqrt(sqrt(whiteS)) / whiteAvg;

        result = blackS + whiteS;

        return result;
    }
    private  boolean isgray=false;

    private int mOperationBrignes(Bitmap bitmap) {

        int result = 0;
        int WhiteCell[];
        float Whitegray = 0, tempgray = 0;

        int whitecount = 0;


        //得到图像的宽度和长度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int w = width/8+8;
        int h= height/8+8;
        WhiteCell = new int[w*h];


        //依次循环对图像的像素进行处理
        for (int i = 0; i < width; ) {

            for (int j = 0; j < height; ) {
                //得到每点的像素值
                int col = bitmap.getPixel(i, j);

                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);

                tempgray = (float) (0.114 * blue + 0.587 * green + 0.299 * red);
                WhiteCell[whitecount++] = (int) tempgray;

                j = j + 8;

            }
            i = i + 8;
        }
        for (int i = 0; i < whitecount; i++) {
            Whitegray += WhiteCell[i];
        }
        if (whitecount != 0)
            result = (int) (Whitegray / whitecount);
        WhiteCell  =null;
        return result;
    }
    public final IFrameCallback mISynchronousScreenFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {     //在UVCPreview的do_capture_callback中最终回调onFrame :

            //2016.09.18 : 新增 : 当出现回调函数没有被回调时 , 重启C++底层的预览线程 ;
            //selfRestartPreview();
            // TODO C++处理完毕，得到新图片
           // Log.w(TAG,"IFrameCallback");
            synchronized (BITMAP) {
              //
//                if(autophotoview.equals("visible")){
//                    if(isFill){
//                        reFillBitmp(frame);
//                    }
//                }else{
//                    reFillBitmp(frame);
//                }
                reFillBitmp(frame);
                frame.rewind();
                frame.clear();
            }

            startFreshnnes();//计算清晰度
            if (!isAutoFocus) {

                if(CameraServerHandler.REMOTE_LOGIN){

                }else {

                    final ServerChannel screenChannel = WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(SynchronousScreenChannel.CHANNEL_NAME);
                    //当检测到外部有网络通道通讯共享时 , 将图片压缩编码后 再发送 :
                    if (screenChannel.getConnectionsCount() > 0) {


                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        BITMAP.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                        try {
                            baos.flush();
                            byte[] bytes = baos.toByteArray();
                            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length);

                            screenChannel.broadcast(ByteZip.gZip(byteBuffer.array()));
                        } catch (IOException e) {
                            FileUtils.writeFileToLogFolder("=====================局域网发送压缩图片IO异常 e="+e+"======================");
                            e.printStackTrace();
                        } finally {
                            if (baos != null) {
                                try {
                                    baos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    private void reFillBitmp(ByteBuffer frame) {
        if(!BITMAP.isRecycled()) {
            BITMAP.copyPixelsFromBuffer(frame);
        }  else {
            BITMAP = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);
            BITMAP.copyPixelsFromBuffer(frame);
        }
    }


    //   private float freshness[] = new float[3];

    public static boolean isAutoFocus() {
        return isAutoFocus;
    }

   /* private void savabitmap(Bitmap bitmap){

       // Bitmap btp = Bitmap.createBitmap(bitmap, interception_x-75, interception_y-75, 150, 150);

        String path = MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, "mytestphoto" + ".jpg","testbmp").toString();
        FileOutputStream fos =null;
        try {

            fos = new FileOutputStream(path);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)) {
                fos.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
  */  private  ArrayList<Float> avgs = new ArrayList<Float>();
    private float maxAvg;
    public float ArrayListMax(ArrayList sampleList)
    {
        try
        {
            float maxDevation = 0;
            int totalCount = sampleList.size();
            if (totalCount >= 1)
            {
                float max = (float) sampleList.get(0);
                for (int i = 0; i < totalCount; i++)
                {
                    float temp = (float) sampleList.get(i);
                    if (temp > max)
                    {
                        max = temp;
                    }
                } maxDevation = max;
            }
            return maxDevation;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }
    private void startFreshnnes() {
        if (isAutoFocus) {

            if (isStartFreshnes) {
                FileUtils.writeFileToLogFolder("开始自动对焦：startFreshnnes");
                float freShnes;
                isStartFreshnes = false;

                mService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        isStartFreshnes = true;
                    }
                }, 100 * COUNT_TO_RESULT, TimeUnit.MILLISECONDS);

                CameraServerHandler serverHandler = getCameraServerHandler(0);
                if (!DIRECTION) {

                  /*  for (int i = 0; i < freshness.length; i++) {
                        freshness[i] = mFreshnes_toSelect(BITMAP, 32, interception_x, interception_y, 300);
                    }
                    freShnes = (freshness[0] + freshness[1] + freshness[2]) / 3;*/
                   /* long l = System.currentTimeMillis();
                    Log.w("mytest","currentmillis ="+l);
                    Bitmap bitmap = Bitmap.createBitmap(BITMAP, interception_x - 75, interception_y - 75, 150, 150);
                    Bitmap sgbitmap = gradient.LaplaceGradient(bitmap);
                    freShnes = mFreshnes_toAllII(sgbitmap);
                    Log.w("mytest","currentmillis11 ="+(System.currentTimeMillis()-l));
                    Log.w("UVCService","<<<<<<<<freShnes="+freShnes);*/

                    Bitmap bitmap = Bitmap.createBitmap(BITMAP, interception_x - 75, interception_y - 75, 150, 150);
                    final Bitmap sgbitmap = gradient.SobelGradient(bitmap);

                    freShnes= mFreshnes_AVG(sgbitmap);


                } else {

                   // freShnes = mFreshnes_toAll(BITMAP, 32, interception_x, interception_y, 150);
                    Bitmap bitmap = Bitmap.createBitmap(BITMAP, interception_x - 75, interception_y - 75, 150, 150);
                    Bitmap sgbitmap = gradient.SobelGradient(bitmap);
                    freShnes = mFreshnes_AVG(sgbitmap);
                    avgs.add(freShnes);
                }
                if (lastRun) {


                    if (lastFreshnes > freShnes) {

                        if(freShnes/lastFreshnes>0.95){
                            serverHandler.mHandleGammaUart(RUN_STIP/2, false, -1,"08");
                            return;
                        }


                      //  Log.w("UVCService","treefreshnes="+treeFreshnes+"lastFreshnes="+lastFreshnes+" ::freShnes="+freShnes+"::="+freShnes/lastFreshnes);
                        if (treeFreshnes > lastFreshnes) {

                            serverHandler.mHandleGammaUart(RUN_STIP , true, -1,"08");

                        } else if (treeFreshnes > freShnes) {
                            serverHandler.mHandleGammaUart(RUN_STIP/2, true, -1,"08");

                        } else {
                            serverHandler.mHandleGammaUart(RUN_STIP/2, true, -1,"08");
                        }
                        isATsuccessful = true;
                        isAutoFocus = false;


                    } else {

                        serverHandler.mHandleGammaUart(RUN_STIP/2, false, -1,"08");
                       // Log.w("UVCService",">>>>>>>>treefreshnes="+treeFreshnes+"lastFreshnes="+lastFreshnes+" ::freShnes="+freShnes+"::="+freShnes/lastFreshnes);
                    }
                    treeFreshnes = lastFreshnes;
                    lastFreshnes = freShnes;
                    return;
                } else {

                    if (DIRECTION) {

                        if ((treeFreshnes - lastFreshnes) > 0 && (lastFreshnes - freShnes) > 0) {

                            di_count++;

                            float f = (freShnes) / treeFreshnes;
                            if (di_count == 1 && f < 0.95&&f>0.5) {
                                DIRECTION = false;
                                COUNT_TO_RESULT = (COUNT_TO_RESULT * 2 + 2);
                                mStip = 2;
                                maxAvg = ArrayListMax(avgs);

                            }else {
                                di_count = 0;
                            }

                        } else {
                            di_count = 0;
                        }

                        serverHandler.mHandleGammaUart(RUN_STIP, true, -1,"08");
                        valueCount++;

                    } else {
                        if (valueCount1 > (mStip-1)) {


                            serverHandler.mHandleGammaUart(RUN_STIP/2, false, -1,"08");

                            lastRun = true;
                            treeFreshnes = lastFreshnes;
                            lastFreshnes = freShnes;

                            return;
                           /* Log.w("UVCService","<<<<<<<<freShnes1="+freShnes+" :::lastfreshnes="+lastFreshnes);
                            float f = freShnes/maxAvg;
                            if(freShnes>maxAvg||f>0.9){

                                isATsuccessful = true;
                                isAutoFocus = false;
                                return;
                            }*/
                          /*  if(lastFreshnes/freShnes>1.2){
                                isATsuccessful = true;
                                isAutoFocus = false;
                                serverHandler.mHandleGammaUart(RUN_STIP, true, -1);
                                return;
                            }*/

                        }
                        serverHandler.mHandleGammaUart(RUN_STIP/2, false, -1,"08");
                        valueCount1++;
                    }
                  //  Log.w("UVCService","<<<<<<<<freShnes1="+freShnes+" :::lastfreshnes="+lastFreshnes);
                    treeFreshnes = lastFreshnes;
                    lastFreshnes = freShnes;
                }


            }
        }
    }

    /*
        //2016.09.18 : 新增 : 当出现回调函数没有被回调时 , 重启C++底层的预览线程 ;
        *//*
    两种方案 :
    1.发送handleMessages延时处理重启;
    2.发送解屏唤醒广播,在广播接收器重启 ;
    * */
    public void selfRestartPreview() {
        CameraServerHandler serverHandler = getCameraServerHandler(0);  //获取默认第0位置索引的key-map值 ;
        if (serverHandler == null) {

            return;
        }
        serverHandler.removeMessages(CameraServerHandler.MSG_RESTART_PREVIEW);
        serverHandler.restartPreview(3000); //延时3000ms后 , 重启预览线程 ;
    }

    //2016.09.19 : 申请获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行 ;
    private void acquireWakeLock() {
        /**
         * WakeLock如果是一个没有超时的锁 , 而且这个锁没有释放 , 那么系统就无法进入休眠 ; WakeLock这个锁机制 可以被用户态程序和内核获得 ;
         * 这个锁可以是超时的或者没有超时的 , 超时的锁会在超时以后自动解锁. 如果没有了锁 或者超时了 , 内核就会启动休眠的那套机制来进入休眠 ;
         */
        //点亮

        // 获取PowerManager的实例
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
        //        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        if (!mWakeLock.isHeld()) {
            // 唤醒屏幕
            mWakeLock.acquire();     //获取唤醒锁
        }

        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mKeyguardLock = mKeyguardManager.newKeyguardLock("unlock");
        //        kl.disableKeyguard();
        if (mKeyguardManager.inKeyguardRestrictedInputMode()) {
            // 解锁键盘
            mKeyguardLock.disableKeyguard();
        }

    }

    //更新缩略图文件名到手机
    public void uadateThumbnailPicterFileName(String fileName) {
        CameraOptionsChannel optionsChannel = (CameraOptionsChannel) WEB_SOCKET_OPTIONS_SERVER.CHANNELS.get(CameraOptionsChannel.CHANNEL_NAME);
        if (optionsChannel.getConnectionsCount() > 0) {
            JSONObject object = new JSONObject();
            try {
                object.put("filename", fileName);
                optionsChannel.broadcast(object.toJSONString());
            } catch (WebsocketNotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageToPhoneMethod(String type, String number) {
        CameraOptionsChannel optionsChannel = (CameraOptionsChannel) WEB_SOCKET_OPTIONS_SERVER.CHANNELS.get(CameraOptionsChannel.CHANNEL_NAME);
        if (optionsChannel.getConnectionsCount() > 0) {
            JSONObject object = new JSONObject();
            if (number.equals("") || number == null)
                return;
            object.put(type, number);

            SPUtils.put(getApplicationContext(), "mFocusMotorStatus", Integer.valueOf(number), true);
            try {
                //发送网络数据的传输方法 :
                optionsChannel.broadcast(object.toJSONString());

            } catch (WebsocketNotConnectedException e) {
                e.printStackTrace();
            }
        }
    }
    private void updataParameter(AVObject avObject){
        CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
        StringBuffer chesknums =new StringBuffer();
        for (int i = 0; i < CheckNums.length; i++) {
            chesknums.append(String.valueOf(CheckNums[i])+",");
        }
        avObject.put("contrastkey", chesknums.substring(0,chesknums.length()-1));
        avObject.put("brightness", ""+CameraServerHandler.cbrightness);
        avObject.put("gain", "" + CameraServerHandler.cgain);
        avObject.put("voltagelevel", "" + (int) serverHandler.getVoltageLevel());
        avObject.put("saturation", "" + CameraServerHandler.csaturation);
        avObject.put("contrast", "" + CameraServerHandler.ccontrast);
        avObject.put("current_contrast", "50");
        avObject.put("recolor", "" + CameraServerHandler.crecolor);
        avObject.put("gamma", "" + CameraServerHandler.cgamma);
        avObject.put("isAutofocus", isAutoFocus);
        avObject.put("stopautophotodelay", isStopAutoPhoto);
        if (checkAutoFocusView == null || checkAutoFocusView.equals("")) {
            avObject.put("vessels", checkAutoFocusView + "1");
        } else
            avObject.put("vessels", checkAutoFocusView + "");
        avObject.put("isexport", isExport);
        long currentTimeMillis = System.currentTimeMillis();
        avObject.put("startdatetimer", currentTimeMillis);
        avObject.put("enddatetimer", currentTimeMillis + (60 * 60 * 1000 * 24));

        EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
            @Override
            public void run() {
                List<File> pngFiles = PictureUtils.getPicturesScaled(getApplicationContext());
                List<File> moviesFiles = PictureUtils.getMovies(getApplicationContext());
                photosize = pngFiles.size();
                mediosize = moviesFiles.size();
            }
        }, 0, TimeUnit.MILLISECONDS);

        String currentReColorString = CameraServerHandler.recolorString;
        avObject.put("recolorstring", currentReColorString);
        avObject.put("pngfiles", photosize);
        avObject.put("moviesfiles", mediosize);
        avObject.put("autophotoview", autophotoview);

        avObject.put("isswitch", CameraServerHandler.objectiveIsSwith);
        avObject.put("is_record", SPUtils.get(UVCService.this, "isRecording", false));
        avObject.put("pad_width", getScreenWidth(UVCService.this));
        avObject.put("pad_height", getScreenHeight(UVCService.this));
        avObject.put("autophotoprocess", processBarAutoPhoto);
        avObject.put("autophotocount", autoPhotoCount);
        avObject.put("autophototimer", stopAutoPhotoStr+"");
        avObject.put("convergence", autoPhotoCNumber+"");
        avObject.put("recondingtimer", reCodingTimer);
        avObject.put("percentage", intevalAndPercentageStr+"");
        File path = Environment.getDataDirectory();
        try {
            StatFs statFs = new StatFs(path.getPath());
            /** Block 的 size*/
            long blockSize = statFs.getBlockSizeLong();
            /** 总 Block 数量 */
            long totalBlocks = statFs.getBlockCountLong();
            /** 剩余的 Block 数量 */
            avObject.put("employMemoery", SettingUtils.convertFileSize(statFs.getFreeBlocksLong() * blockSize));
            avObject.put("totalMemoery", SettingUtils.convertFileSize(totalBlocks * blockSize));
        } catch (Exception e) {

            e.printStackTrace();

        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(BITMAP!=null)
        BITMAP.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        //avObject.put("image", new AVFile("productPic", baos.toByteArray()));
        avObject.put("image", baos.toByteArray());
    }
    private  void addUserParameter(){
        Log.d(TAG, "CameraServerHandler.userName1 ="+CameraServerHandler.userName);
        if(CameraServerHandler.userName!=null&&!CameraServerHandler.userName.equals("")) {

            AVObject avObject = new AVObject(RequesUtils.PARAMETER);
            avObject.put("user", CameraServerHandler.userName);
            updataParameter(avObject);
            avObject.saveInBackground(new SaveCallback() {
                @Override
                public void done(AVException e) {
                    Log.d(TAG, "addSuccess"+e);
                }
            });
        }
    }
    //更新消息到手机端
    public void updateMessageToPhoneMethod() {
        Log.w(TAG,"updateMessageToPhoneMethod="+CameraServerHandler.REMOTE_LOGIN+"  "+CameraServerHandler.userName);
        if(CameraServerHandler.REMOTE_LOGIN){

            if(CameraServerHandler.userName==null||CameraServerHandler.userName.equals(""))
                return;
            AVQuery<AVObject> mquery = new AVQuery<>(RequesUtils.PARAMETER);
            mquery.whereEqualTo("user",CameraServerHandler.userName);
            mquery.getFirstInBackground(new GetCallback<AVObject>() {
                @Override
                public void done(final AVObject account, AVException e) {
                    if(e==null&&account!=null) {
                        updataParameter(account);
                        AVSaveOption option = new AVSaveOption();
                        option.query(new AVQuery<>(RequesUtils.PARAMETER).whereEqualTo("user", CameraServerHandler.userName.trim()));
                        option.setFetchWhenSave(true);
                        account.saveInBackground(option, new SaveCallback() {
                            @Override
                            public void done(AVException e) {

                                if (e == null) {

                                } else {
                                    FileUtils.writeFileToLogFolder("远程同步消息异常: e="+e);
                                }
                            }
                        });
                    }
                }
            });


        }else {
            CameraOptionsChannel optionsChannel = (CameraOptionsChannel) WEB_SOCKET_OPTIONS_SERVER.CHANNELS.get(CameraOptionsChannel.CHANNEL_NAME);
            if (optionsChannel.getConnectionsCount() > 0) {
                JSONObject object = new JSONObject();
                object.put(DataUtil.CONTRASTKEY, CheckNums);
                int brightness = CameraServerHandler.cbrightness;
                object.put("brightness", String.valueOf(brightness));
                // 成功
                int gain = CameraServerHandler.cgain;
                object.put("gain", String.valueOf(gain));

                //发送最大亮度值及电压百分比
                CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
                // int maxBrightness = serverHandler.getMaxBrightness();
                int voltageLevel = (int) serverHandler.getVoltageLevel();
                // object.put("maxbrightness", String.valueOf(maxBrightness));
                object.put("voltagelevel", String.valueOf(voltageLevel));
                // 成功
                //激发块灯光 :
                int saturation = CameraServerHandler.csaturation;
                object.put("saturation", saturation);
                // 成功
                //切换物镜 :

                int contrast = CameraServerHandler.ccontrast;
                object.put("contrast", contrast);

                //2016.08.02 :　新增　：
                if (contrast == 10) {
                    if(mobjects.length>1)
                    SPUtils.put(getApplicationContext(), "current_contrast", CameraServerHandler.mobjects[0]);
                } else if (contrast == 20) {
                    if(mobjects.length>1)
                    SPUtils.put(getApplicationContext(), "current_contrast",CameraServerHandler.mobjects[1]);
                }

                //2016.08.02 : 新增 :
                Object current_contrast_String = SPUtils.get(getApplicationContext(), "current_contrast", 10);
                int current_contrast = (current_contrast_String == null ? 0 : (int) current_contrast_String);
                object.put("current_contrast", current_contrast);

                // int color = BaseApplication.getInstance().getRecolor();
                int color = CameraServerHandler.crecolor;
                object.put("recolor", color);

                // int mgamma = BaseApplication.getInstance().getGamma();
                int mgamma = CameraServerHandler.cgamma;
                object.put("gamma", mgamma);
                if (checkAutoFocusView == null || checkAutoFocusView.equals("")) {
                    object.put("vessels", checkAutoFocusView + "1");
                } else
                    object.put("vessels", checkAutoFocusView + "");

                object.put("isAutofocus", isAutoFocus);
                object.put("stopautophotodelay", isStopAutoPhoto);
                object.put("isexport", isExport);
                long currentTimeMillis = System.currentTimeMillis();
                object.put("startdatetimer", currentTimeMillis);
                object.put("enddatetimer", currentTimeMillis + (60 * 60 * 1000 * 24));


                EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
                    @Override
                    public void run() {
                        List<File> pngFiles = PictureUtils.getPicturesScaled(getApplicationContext());
                        List<File> moviesFiles = PictureUtils.getMovies(getApplicationContext());
                        photosize = pngFiles.size();
                        mediosize = moviesFiles.size();
                    }
                }, 0, TimeUnit.MILLISECONDS);

                //String currentReColorString = BaseApplication.getInstance().getCurrentReColorString();
                String currentReColorString = CameraServerHandler.recolorString;
                object.put("recolorstring", currentReColorString);


                object.put("pngfiles", photosize);

                object.put("moviesfiles", mediosize);
               // Log.e("MasterIntentService", "sendstring=" + photosize + "::mediosize=" + mediosize);
                object.put("autophotoview", autophotoview);
                object.put("isswitch", CameraServerHandler.objectiveIsSwith);
                object.put("is_record", String.valueOf(SPUtils.get(UVCService.this, "isRecording", false)));
                // object.put("camera_width", UVCCamera.DEFAULT_PREVIEW_WIDTH);
                //  object.put("camera_height", UVCCamera.DEFAULT_PREVIEW_HEIGHT);
                object.put("pad_width", getScreenWidth(UVCService.this));
                object.put("pad_height", getScreenHeight(UVCService.this));

                object.put("autophotoprocess", processBarAutoPhoto);
                object.put("autophotocount", autoPhotoCount);
                object.put("autophototimer", stopAutoPhotoStr);
                object.put("convergence", autoPhotoCNumber);
                object.put("recondingtimer", reCodingTimer);
                object.put("percentage", intevalAndPercentageStr+"");
                Log.i(TAG,"percentage="+intevalAndPercentageStr);
                try {
                    //发送网络数据的传输方法 :
                    optionsChannel.broadcast(object.toJSONString());
                    //Log.w("UVCService","object.toJSONString()="+object.toJSONString());
                } catch (WebsocketNotConnectedException e) {
                    FileUtils.writeFileToLogFolder("===================局域网链接异常: e="+e+"=========================");
                    e.printStackTrace();
                }
            }
        }
    }

    //获取屏幕的宽度
    private final int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getWidth();
    }

    //获取屏幕的高度
    private final int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getHeight();
    }

    //********************************************************************************
    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (DEBUG)
                Log.d(TAG, "OnDeviceConnectListener#onAttach:");
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG)
                Log.d(TAG, "OnDeviceConnectListener#onConnect:");
            synchronized (sServiceSync) {
                int key = device.hashCode();
                //                CameraServerHandler serviceHandler = sCameraServerHandlers.get(key);
                if (mServiceHandler == null) {
                    mServiceHandler = CameraServerHandler.createServerHandler(getApplicationContext(), ctrlBlock, device.getVendorId(), device.getProductId(), mISynchronousScreenFrameCallback);
                    //                    //处理集合器 添加成员 ：
                    //                    sCameraServerHandlers.append(key, serviceHandler);

                } else {

                }
                mServiceHandler.sendEmptyMessage(MESSAGE_REFRESH);

                SPUtils.put(UVCService.this, "current_device_key", key);

                SPUtils.put(UVCService.this, "isRecording", false);

                // 开启相机状态同步
                sendCameraParameter(null);
                // 开启平板状态同步,同步平板设备状态到手机客户端
                //sendDeviceStatus();  //有可能会导致拔优盘出现BUG
                // 开启客户端数量同步
                //sendClientNumber();
                // 监控客户端是否能够使用
                //sendClientEnable();

                sServiceSync.notifyAll();
            }
        }

        private Timer timer;                    //CameraOptionsChannel  特殊 , 发送到本地 ;
        private Timer statusTimer;              //DeviceStatusChannel
        private Timer clientNumberTimer;        //ClientNumberChannel
        private Timer clientEnableTimer;        //DeviceOperationControlChannel

        // 监控客户端是否能够使用
        public void sendClientEnable() {        //发送JSON字符串
            if (clientEnableTimer != null) {
                clientEnableTimer.cancel();
                clientEnableTimer = null;
            }
            if (clientEnableTimer == null && WEB_SOCKET_CLIENT_NUMBER_SERVER.CHANNELS.get(DeviceOperationControlChannel.CHANNEL_NAME) != null) {
                clientEnableTimer = new Timer();
                clientEnableTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        DeviceOperationControlChannel clientEnableChannel = (DeviceOperationControlChannel) WEB_SOCKET_CLIENT_NUMBER_SERVER.CHANNELS.get(DeviceOperationControlChannel.CHANNEL_NAME);
                        if (clientEnableChannel.getConnectionsCount() > 0) {
                            JSONObject object = new JSONObject();
                            Object o = SPUtils.get(UVCService.this, "enable", true);

                            object.put("enable", o == null ? true : (boolean) o);
                            try {
                                if (DEBUG)
                                    Logger.e(TAG, object.toJSONString());
                                clientEnableChannel.broadcast(object.toJSONString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, 0, 1000);
            }
        }

        //发送    // 开启客户端数量同步
        public void sendClientNumber() {        //发送JSON字符串
            if (clientNumberTimer != null) {
                clientNumberTimer.cancel();
                clientNumberTimer = null;
            }
            if (clientNumberTimer == null && WEB_SOCKET_CLIENT_NUMBER_SERVER.CHANNELS.get(ClientNumberChannel.CHANNEL_NAME) != null) {
                clientNumberTimer = new Timer();
                clientNumberTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        ClientNumberChannel clientNumberChannel = (ClientNumberChannel) WEB_SOCKET_CLIENT_NUMBER_SERVER.CHANNELS.get(ClientNumberChannel.CHANNEL_NAME);
                        final ServerChannel screenChannel = WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(SynchronousScreenChannel.CHANNEL_NAME);
                        int connections = screenChannel.getConnectionsCount()-1;

                        //Log.w("UVCService","connections2="+screenChannel.getConnectionsCount());
                        if (clientNumberChannel.getConnectionsCount() > 0) {
                            JSONObject object = new JSONObject();
                            object.put("count", connections);
                            try {
                                clientNumberChannel.broadcast(object.toJSONString());
                            } catch (WebsocketNotConnectedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, 0, 1000);
            }
        }
        private int timerdecount =0;
        //发送         // 开启相机状态同步
        public void sendCameraParameter(final CameraServerHandler serviceHandler) {     //发送JSON字符串 CameraOptionsChannel

            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            if (timer == null && WEB_SOCKET_OPTIONS_SERVER.CHANNELS.get(CameraOptionsChannel.CHANNEL_NAME) != null) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {     //特殊 , 重写了broadcast()方法 ;
                        updateMessageToPhoneMethod();
                        timerdecount ++;
                        if(timerdecount>11){
                            CameraOptionsChannel optionsChannel = (CameraOptionsChannel) WEB_SOCKET_OPTIONS_SERVER.CHANNELS.get(CameraOptionsChannel.CHANNEL_NAME);
                            if (optionsChannel.getConnectionsCount() > 0) {
                                sendPadDeviceStatus();
                            }
                            timerdecount = 0;
                        }
                    }
                }, 0, 1000);
            }
        }

        // 开启平板状态同步,同步平板设备状态到手机客户端  //有可能会导致拔优盘出现BUG
        public void sendDeviceStatus() {        //发送JSON字符串

            if (statusTimer != null) {
                statusTimer.cancel();
                statusTimer = null;
            }
            if (statusTimer == null && WEB_SOCKET_STATUS_SERVER.CHANNELS.get(DeviceStatusChannel.CHANNEL_NAME) != null) {

                statusTimer = new Timer();
                statusTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {


                    }
                }, 0, 10000);

            }
        }


        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG)
                Log.d(TAG, "OnDeviceConnectListener#onDisconnect:");

            //2016.11.07 : 添加掉线清空 控制对象 ；
            checkReleaseService();
            //            sCameraServerHandlers.clear();
            removeService(device);
        }

        @Override
        public void onDettach(final UsbDevice device) {
            if (DEBUG)
                Log.d(TAG, "OnDeviceConnectListener#onDettach:");

            //2016.11.07 : 添加掉线清空 控制对象 ；
            checkReleaseService();
            //            sCameraServerHandlers.clear();
            removeService(device);
        }

        @Override
        public void onCancel() {
            if (DEBUG)
                Log.d(TAG, "OnDeviceConnectListener#onCancel:");
            //2016.11.07 : 添加掉线清空 控制对象 ；
            checkReleaseService();
            //            sCameraServerHandlers.clear();
            //            removeService(device);

            synchronized (sServiceSync) {
                sServiceSync.notifyAll();
            }
        }
    };

    private void sendPadDeviceStatus() {
        //TODO 获取电池、内存、wifi连接、相机连接、连接当前手机数、IP、MAC
        DeviceStatusModel status = new DeviceStatusModel();
        //TODO ip
        status.setDeviceIP(SettingUtils.getIPAddress(BaseApplication.getContext()));
        //TODO mac
        status.setDeviceMac(SettingUtils.getLocalMacAddress(BaseApplication.getContext()));
        //TODO ssid
        status.setWifi(SettingUtils.getCurrentSSID(BaseApplication.getContext()));
        //TODO battery
        CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
        int voltageLevel = (int) serverHandler.getVoltageLevel();
        status.setBattery(voltageLevel + "%");
        //TODO memory
        /** 获取存储卡路径 */

        //  Log.i("UVCService","sdcardDir="+sdcardDir +"  leng="+sdcardDir.length());
        boolean getSdCardStatus = true;
        //if (sdcardDir != null && sdcardDir.length() > 0) {
        /** StatFs 看文件系统空间使用情况 */
        File path = Environment.getDataDirectory();
        try {
            StatFs statFs = new StatFs(path.getPath());
            /** Block 的 size*/
            long blockSize = statFs.getBlockSizeLong();
            /** 总 Block 数量 */
            long totalBlocks = statFs.getBlockCountLong();
            /** 剩余的 Block 数量 */
            status.setEmployMemory(SettingUtils.convertFileSize(statFs.getFreeBlocksLong() * blockSize));
            status.setTotalMemory(SettingUtils.convertFileSize(totalBlocks * blockSize));

        } catch (Exception e) {
            FileUtils.writeFileToLogFolder("读取储存卡异常: e="+e);
            e.printStackTrace();
            getSdCardStatus = false;
            Log.v(TAG, e.toString());
        }
        //  }
        if (!getSdCardStatus) {
            status.setEmployMemory(String.valueOf("0"));
            status.setTotalMemory("0");
        }
        //TODO connectCount
        if (getUSBMonitor() != null) {
            UsbDevice usbDevice = getUSBMonitor().getCurrentDevice();
            if (usbDevice == null) {
                status.setCameraConnect("未连接");
            } else {
                status.setCameraConnect(usbDevice.getDeviceName());
            }
        }
        //TODO connectCount
        //   Log.e("UVCService","connectcount="+String.valueOf(WEB_SOCKET_STATUS_SERVER.getConnectionCount())+"::"+WEB_SOCKET_CLIENT_NUMBER_SERVER.getOtherConnectionCount());
        DeviceStatusChannel deviceStatusChannel = (DeviceStatusChannel) WEB_SOCKET_STATUS_SERVER.CHANNELS.get(DeviceStatusChannel.CHANNEL_NAME);
        final ServerChannel screenChannel = WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(SynchronousScreenChannel.CHANNEL_NAME);
        int connections = screenChannel.getConnectionsCount()-1;
        CameraServerHandler.phone_number = connections;
        status.setConnectCount(String.valueOf(connections));
        try {
            deviceStatusChannel.broadcast(JSON.toJSONString(status));
        } catch (WebsocketNotConnectedException e) {
            e.printStackTrace();
        }
    }

    private void removeService(final UsbDevice device) {
        synchronized (sServiceSync) {
            int key = device.hashCode();
            //            CameraServerHandler serviceHandler = sCameraServerHandlers.get(key);
            if (mServiceHandler != null)
                mServiceHandler.release();
            //            sCameraServerHandlers.remove(key);
            sServiceSync.notifyAll();
        }
        if (checkReleaseService()) {
            if (mUSBMonitor != null) {
                mUSBMonitor.unregister();
                mUSBMonitor = null;
            }
        }
    }

    //********************************************************************************
    private static final Object sServiceSync = new Object();
    //    private static final SparseArray<CameraServerHandler> sCameraServerHandlers = new SparseArray<CameraServerHandler>();

    public static CameraServerHandler mServiceHandler = null;

    /**
     * get CameraService that has specific ID<br>
     * if zero is provided as ID, just return top of CameraServerHandler instance(non-blocking method) if exists or null.<br>
     * if non-zero ID is provided, return specific CameraService if exist. block if not exists.<br>
     * return null if not exist matched specific ID<br>
     *
     * @param serviceId
     * @return
     */
    public static CameraServerHandler getCameraServerHandler(final int serviceId) {
        synchronized (sServiceSync) {


            try {
                if (mServiceHandler == null) {
                    sServiceSync.wait();
                }
            } catch (Exception e) {
                FileUtils.writeFileToLogFolder("================getCameraServerHandler异常 e="+e+"==================");
                e.printStackTrace();


                //2016.11.07 新增 掉线释放对象
                mServiceHandler = null;
                //                release();
                sServiceSync.notifyAll();
            }
        }
        return mServiceHandler;
    }

    /**
     * @return true if there are no camera connection
     */
    private static boolean checkReleaseService() {
        synchronized (sServiceSync) {


            if (mServiceHandler != null && !mServiceHandler.isConnected()) {
                //                sCameraServerHandlers.removeAt(i);
                mServiceHandler.release();
            }

            //2016.11.08 : 新增 ： 添加注销USB设备;
            if (mUSBMonitor != null) {
                mUSBMonitor.setCurrentDevice(null);

            }

            //            return sCameraServerHandlers.size() == 0;
            return mServiceHandler == null;
        }

    }

    // TODO: 2016/7/4  : 添加为图片添加水印的 方法 :
    //给图片添加水印的基本思路都是载入原图，添加文字或者载入水印图片，保存图片这三个部分 :

    /* //添加水印图片：
     private static Bitmap createWaterMaskImage(Context gContext, Bitmap src, Bitmap watermark, int x, int y) {

         String tag = "createBitmap";
         Log.d(tag, "create a new bitmap");
         if (src == null) {
             return null;
         }
         int w = src.getWidth();
         int h = src.getHeight();

         // create the new blank bitmap
         Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
         Canvas cv = new Canvas(newb);
         // draw src into
         cv.drawBitmap(src, 0, 0, null);// 在 0，0坐标开始画入src
         // draw watermark into
         cv.drawBitmap(watermark, x, y, null);// 在src的右下角画入水印
         //        cv.drawBitmap(watermark, 1100, 850, null);// 在src的右下角画入水印    图像大小 : width = 1360 , height = 1040
         // save all clip
         cv.save(Canvas.ALL_SAVE_FLAG);// 保存
         // store
         cv.restore();// 存储
         return newb;
     }


     public static Bitmap drawTextToBitmap(Context gContext, int gResId, String gText, int x, int y) {
         Resources resources = gContext.getResources();
         float scale = resources.getDisplayMetrics().density;
         Bitmap bitmap = BitmapFactory.decodeResource(resources, gResId);


         Bitmap.Config bitmapConfig = bitmap.getConfig();


         if (bitmapConfig == null) {
             bitmapConfig = Bitmap.Config.ARGB_8888;
         }

         bitmap = bitmap.copy(bitmapConfig, true);


         Canvas canvas = new Canvas(bitmap);
         // new antialised Paint
         Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
         // text color - #3D3D3D
         paint.setColor(Color.WHITE);
         paint.setTextSize((int) (18 * scale));
         paint.setDither(true); //获取跟清晰的图像采样
         paint.setFilterBitmap(true);//过滤一些
         Rect bounds = new Rect();
         paint.getTextBounds(gText, 0, gText.length(), bounds);
         //        int x = 30;
         //        int y = 30;
         canvas.drawText(gText, x * scale, y * scale, paint);

         return bitmap;
     }
 */
    ////////////////////////////////////////////////////////
    //此处实现aidl借口 IUVCService.aidl
    //********************************************************************************
    public int currentServiceId = 0;
    private final IUVCService.Stub mBasicBinder = new IUVCService.Stub() {
        private IUVCServiceCallback mCallback;


        @Override
        public int select(final UsbDevice device, final IUVCServiceCallback callback) throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#select:device=" + (device != null ? device.getDeviceName() : null));
            mCallback = callback;

            synchronized (sServiceSync) {
                if (mServiceHandler == null) {

                    try {
                        sServiceSync.wait();
                    } catch (final Exception e) {
                        e.printStackTrace();

                    }

                    //                    serverHandler = sCameraServerHandlers.get(currentServiceId);
                    if (mServiceHandler == null) {
                        //                        throw new RuntimeException("failed to open USB device(has no permission)");
                        return -1;
                    }
                }
            }
            if (mServiceHandler != null) {
                mServiceHandler.registerCallback(callback);

            }
            return currentServiceId;
        }

        @Override
        public void release() throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#release:");
            synchronized (sServiceSync) {
                //                CameraServerHandler serverHandler = sCameraServerHandlers.get(currentServiceId);
                if (mServiceHandler != null) {
                    if (mServiceHandler.unregisterCallback(mCallback)) {
                        if (!mServiceHandler.isConnected()) {
                            //                            sCameraServerHandlers.remove(currentServiceId);
                            mUSBMonitor.setCurrentDevice(null);
                            if (mServiceHandler != null) {
                                mServiceHandler.release();
                            }
                            //                            final CameraServerHandler srvHandler = sCameraServerHandlers.get(currentServiceId);
                            //                            Log.w(TAG, "srv=" + srvHandler);
                        }
                    }
                }
            }
            mCallback = null;
        }

        @Override
        public boolean isSelected() throws RemoteException {
            return getCameraServerHandler(currentServiceId) != null;
        }

        @Override
        public void releaseAll() throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#releaseAll:");
            synchronized (sServiceSync) {
                CameraServerHandler serverHandler;
                mUSBMonitor.setCurrentDevice(null);

                if (mServiceHandler != null) {
                    mServiceHandler.release();
                }

            }
        }

        @Override
        public void resize(final int width, final int height) {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#resize:");
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler == null) {
                //                throw new IllegalArgumentException("invalid currentServiceId");
                return;
            }
            serverHandler.resize(width, height);

        }

        @Override
        public void connect() throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#connect:");
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler == null) {
                //                throw new IllegalArgumentException("invalid currentServiceId");
                return;
            }
            serverHandler.connect();
        }

        //2016.09.14 : 新增 : 用于待机唤醒后重新设置并开始预览图像 :
        @Override
        public void restartPreview() throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#restartPreview:");
            FileUtils.writeFileToLogFolder("开始预览图像:restartPreview");
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler == null) {
                return;
            }
            serverHandler.restartPreview(0);    //延时0ms后 , 重启预览线程 ;


        }

        @Override
        public void sotpProcess() throws RemoteException {
            stopMethod();
        }

        @Override
        public int getBackProcessPid() throws RemoteException {
            int pid = Process.myPid();
            return pid;
        }

        @Override
        public boolean isConnectUSB() throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            return (serverHandler != null) && serverHandler.isConnected();

        }

        @Override
        public void reStartCamera() throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.reStartCamera();
            }
        }

        @Override
        public void stopCamer() throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.stopCamera();
            }
        }

        @Override
        public void sendAutoFocusMessege(int count, int x, int y, boolean isstop, int movestep) throws RemoteException {
            if (isstop) {//开始自动对焦
                isAutoFocus = true;
                lastFreshnes = 0;
                treeFreshnes = 0;
                valueCount1 = 0;
                valueCount = 0;
                mStip = 0;
                mFreshnes.clear();
                DIRECTION = true;
                di_count = 0;
                mUart_Gamma = 0;
                lastRun = false;

                interception_x = x;//截取坐标
                interception_y = y;
                isATsuccessful = false;
                isStartFreshnes = true;

                maxAvg=0;
                avgs.clear();

               // int msaturation = BaseApplication.getInstance().getSaturation();
                int msaturation = CameraServerHandler.csaturation;

                if (msaturation == 1) {  //如果是白色

                    COUNT_TO_RESULT = movestep;

                } else
                    COUNT_TO_RESULT = 2;

            } else { //结束自动对焦
                isAutoFocus = false;
                lastFreshnes = 0;
                treeFreshnes = 0;
                valueCount1 = 0;
                valueCount = 0;
                isStartFreshnes = true;
                mStip = 0;
                mFreshnes.clear();
                DIRECTION = true;
                di_count = 0;
                lastRun = false;

                interception_x = x;//截取坐标
                interception_y = y;
                isATsuccessful = false;
                COUNT_TO_RESULT = 0;

            }
        }

        @Override
        public void mSetGamma(int focus, boolean isForward, int currentstep,String speed) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.mHandleGammaUart(focus, isForward, currentstep,speed);
            }
        }

        @Override
        public void releadFocus() throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.releadFocus();
            }
        }

        @Override
        public boolean getAutoFocusResult() throws RemoteException {
            if (isATsuccessful) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int getUartGamma(int uartgamma) throws RemoteException {
            if (!isATsuccessful) {//没对焦成功之前把值赋给mUart_Gamma
                if (uartgamma == 0) {
                   // BaseApplication.getInstance().setGamma(uartgamma);
                    CameraServerHandler.cgamma = uartgamma;
                }

            }
           // return BaseApplication.getInstance().getGamma();
            return CameraServerHandler.cgamma;
        }

        @Override
        public void setUartGamma(int uartgamma) throws RemoteException {
           // BaseApplication.getInstance().setGamma(uartgamma);
            CameraServerHandler.cgamma = uartgamma;
        }

        @Override
        public float getOperationBrignes() throws RemoteException {
            float result = 0;
            synchronized (BITMAP) {
                if (BITMAP == null || BITMAP.isRecycled())
                    return result;
               // final float freshnes =(float) JniUtils.getOperationBrignes(BITMAP);

                final float freshnes = mOperationBrignes(BITMAP);
                result = freshnes;
            }
            return result;
        }

        @Override
        public float getFreshnes() throws RemoteException {
            float result = 0;
            synchronized (BITMAP) {
                if (BITMAP == null || BITMAP.isRecycled())
                    return result;
               // final float freshnes = mFreshnes_toAll(BITMAP, 32, 650, 650, 100);

                Bitmap bitmap = Bitmap.createBitmap(BITMAP, (680 - 125), (520 - 125), 250, 250);
                Bitmap sgbitmap = gradient.SobelGradient(bitmap);
                float freShnes = mFreshnes_AVG(sgbitmap);
                bitmap.recycle();
                sgbitmap.recycle();
                bitmap = null;
                sgbitmap =null;
                result = freShnes;
            }
            return result;
        }

        @Override
        public int getMaxBrightness() throws RemoteException {

            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                int brighness = serverHandler.getMaxBrightness();
                maxbrightness = brighness;
                return brighness;
            }

            return 0;
        }
        @Override
        public void setRockerState(String state) throws RemoteException {

            rockerState = state;


        }

        @Override
        public int getAppRestartState() throws RemoteException {
            return restartAppState;
        }

        @Override
        public boolean getObjectiveIsSwith(boolean isOn) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                return serverHandler.getObjectiveSwithState(isOn);
            }
            return false;
        }

        @Override
        public void updateMessagePhoneMethod(String type, String number) throws RemoteException {
            sendMessageToPhoneMethod(type, number);
        }

        @Override
        public void uadateTPicterFileName(String filename) throws RemoteException {
            uadateThumbnailPicterFileName(filename);
        }

        @Override
        public void sendToBackgroundProgram(String type, String message) throws RemoteException {
            if (type.equals("autophotoview")) {
                autophotoview = message;
            }
            if (type.equals("processid")) {
                processId = Integer.valueOf(message);
            }
            if(type.equals("isfillbitmap")){
                isFill  = true;
            }
            if (type.equals("check_aotufocus_object")) {
                checkAutoFocusView = message;
            }
            if(type.equals("stopautophoto")){
                if(message.equals("stop")){
                    isStopAutoPhoto = false;
                }else if(message.equals("istostop")){
                    isStopAutoPhoto = true;

                }
            }
            if(type.equals("recodingtime")){
                reCodingTimer = message;
            }
            if(type.equals("isexport")){
                if(message.equals("istrue")){
                    isExport = true;
                }else if(message.equals("isflase")){
                    isExport = false;
                }
            }
            if(type.equals("autophotoprocess")){
                if(!message.equals("")){
                     processBarAutoPhoto = Integer.valueOf(message.trim());
                }
            }
            if(type.equals("autophotocount")){
                if(!message.equals("")){
                   autoPhotoCount = Integer.valueOf(message.trim());
                }
            }
            if(type.equals("autophototimer")){
                   stopAutoPhotoStr = message.trim();
            }
            if(type.equals("convergence")){
                autoPhotoCNumber = message.trim();
            }
            if(type.equals("goneseekbar")){
                stopAutoPhotoStr ="";
                processBarAutoPhoto =0;
                autoPhotoCount =0;
            }

            if(type.equals("mytest")){

                if(message.equals("mtrue")){
                    isgray = true;
                }else if(message.equals("mfalse")){
                    isgray = false;
                }
            }
            if(type.equals(DataUtil.CONTRASTKEY)){
                String []strs = message.split("&");
                if(strs.length>1){
                    int position = Integer.parseInt(strs[0]);
                    boolean ischeck= Boolean.parseBoolean(strs[1]);
                    CheckNums[position] = ischeck;
                }
            }
            if(type.equals("percentage")){
                intevalAndPercentageStr = message;
            }

        }

        @Override
        public void sendStringToUart(String str) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.sendStringToUart(str);
            }
        }

        @Override
        public void closeUVC() throws RemoteException {
            FileUtils.writeFileToLogFolder("关闭摄像头: closeUVC");
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler == null) {
                return;
            }
            serverHandler.rhandleCloseUVCCamera();
        }

        @Override
        public void openUVC() throws RemoteException {
            FileUtils.writeFileToLogFolder("打开摄像头: openUVC");
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler == null) {
                return;
            }
            serverHandler.rhandleOpenUVCCamera();
        }

        @Override
        public void sendRecolorString(String recolorstring) throws RemoteException {
            //BaseApplication.getInstance().setCurrentReColorString(recolorstring);
            CameraServerHandler.recolorString = recolorstring;
        }

        @Override
        public void correctionPictureXY(int x, int y) throws RemoteException {
            pTranlateX = x;
            pTranlateY = y;
        }

        @Override
        public int getSaturationState() {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler == null) {
                return 0;
            }

            return serverHandler.getSaturationState();
        }

        @Override
        public void setResponeModel(boolean remote) throws RemoteException {
            FileUtils.writeFileToLogFolder("手机客户端远程登录状态："+remote+" (true 为登录 false 为退出)");
            CameraServerHandler.REMOTE_LOGIN = remote;
        }

        @Override
        public int getPicPercentage(int temp) throws RemoteException {
            int percentage;
            if(BITMAP==null||BITMAP.isRecycled())
                return 0;
            PictureUtils pictureUtils = new PictureUtils();

            int percentage1 = pictureUtils.decountPercentage(BITMAP, 95);
            //Log.i(TAG,"percentage1="+percentage1);
            percentage = percentage1;
            pictureUtils = null;
         //   int percentage1 = (int)JniUtils.changeBrightness(95,BITMAP);
            return percentage1;
        }

        @Override
        public int getMaxNuberType(int type) throws RemoteException {

            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                int iso = serverHandler.getMaxNuberType(type);
                maxiso = iso;
                return iso;
            }
            return 100;

        }


        @Override
        public void disconnect() throws RemoteException {

            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler == null) {
                return;
            }
            serverHandler.disconnect();
        }

        @Override
        public boolean isConnected(int constant,int gamma) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if(CameraServerHandler.cgamma!=gamma){
                CameraServerHandler.cgamma = gamma;
            }
            //showNotification("YEESPEC正在运行");
            // serverHandler.sendEmptyMessage(serverHandler.MESSAGE_IS_CONNECT);
            boolean uartConnect = serverHandler.isUartConnect(constant);

            return uartConnect;
        }

        @Override
        public void addSurface(final int id_surface, final Surface surface, final boolean isRecordable) throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#addSurface:id=" + id_surface + ",surface=" + surface);
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.addSurface(id_surface, surface, isRecordable, null);
            }
        }

        @Override
        public void removeSurface(final int id_surface) throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#removeSurface:id=" + id_surface);
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null)
                serverHandler.removeSurface(id_surface);
        }

        @Override
        public boolean isRecording() throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);

            if (serverHandler != null) {

            } else {
                //Log.w("test_UVCService", "isRecording()#CameraServerHandler == null !");
            }

            return serverHandler != null && serverHandler.isRecording();
        }

        @Override
        public void startRecording(int filenumber) throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#startRecording:");
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if ((serverHandler != null) && !serverHandler.isRecording()) {
                serverHandler.startRecording(filenumber);
                SPUtils.put(UVCService.this, "isRecording", true);
            }
        }

        @Override
        public void stopRecording() throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#stopRecording:");
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if ((serverHandler != null) && serverHandler.isRecording()) {
                serverHandler.stopRecording();
                SPUtils.put(UVCService.this, "isRecording", false);
            }
        }

        @Override
        public void captureStillImage(final String path) throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "mBasicBinder#captureStillImage:" + path);
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.captureStill(path);
            }
        }

        @Override
        public void cameraControl(int control_type, int control_parameter) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.sendMessage(serverHandler.obtainMessage(control_type, control_parameter));


            }
        }

        @Override
        public void setCameraAutoFocus(boolean is_auto_focus) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                serverHandler.sendMessage(serverHandler.obtainMessage(CameraServerHandler.MSG_IS_AUTO_FOCUS, is_auto_focus));
            }
        }

        @Override
        public int cameraParameter(int parameter_type) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                switch (parameter_type) {
                    case CameraServerHandler.MSG_BRIGHTNESS_CONTROL:
                        return serverHandler.getBrightness();
                    case CameraServerHandler.MSG_CONTRAST_CONTROL:
                        return serverHandler.getContrast();
                    case CameraServerHandler.MSG_GAIN_CONTROL:
                        return serverHandler.getGain();
                    case CameraServerHandler.MSG_SATURATION_CONTROL:
                        return serverHandler.getSaturation();
                    case CameraServerHandler.MSG_FOCUS_CONTROL:
                        return serverHandler.getFocus();
                    case CameraServerHandler.MSG_GAMMA_CONTROL:
                        return serverHandler.getGamma();
                    default:
                        break;
                }
            }
            return 0;
        }

        @Override
        public void clientControl(boolean type) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                SPUtils.put(UVCService.this, "enable", type);
            }
        }

        @Override
        public void setColor(int color) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {
                //SPUtils.put(BaseApplication.getContext(), "recolor", color);
                //BaseApplication.getInstance().setRecolor_pos(color);
                serverHandler.sendMessage(serverHandler.obtainMessage(CameraServerHandler.MSG_SET_COLOR, color));
            }
        }

        @Override
        public void setGray(int gray) throws RemoteException {
            CameraServerHandler serverHandler = getCameraServerHandler(currentServiceId);
            if (serverHandler != null) {

             serverHandler.msetGamma(gray);
            }
        }
        // final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        // Bitmap waterBitmap = BitmapFactory.decodeResource( getApplicationContext().getResources(), R.mipmap.scaleplate);
        /**
         * 偏移效果
         * @param origin 原图
         * @return 偏移后的bitmap
         * X 正 往右移动   Y 正往下移动
         * 为零不移动
         */
        private Bitmap transBitmapXY(Bitmap origin,int pTranslateX,int pTranslateY) {

            Bitmap bmp =Bitmap.createBitmap(origin.getWidth(),origin.getHeight(), Bitmap.Config.ARGB_8888);
            if (origin == null) {
                return bmp ;
            }
            int width = origin.getWidth();
            int height = origin.getHeight();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {

                    int pixel = origin.getPixel(i,j);
                    int y ;
                    int x;
                    //y坐标校准
                    if((j+pTranslateY)<0) {
                        y = 0;
                    }else if((j+pTranslateY)>(height-1)) {
                        y = height-1;
                    } else {
                        y = j + pTranslateY;
                    }
                    //x坐标校准
                    if((i+pTranslateX)<0) {
                        x = 0;
                    }else if((i+pTranslateX)>(width-1)) {
                        x = width-1;
                    } else {
                        x = i + pTranslateX;
                    }

                    bmp.setPixel(x,y, pixel);

                }
            }

            return bmp;
        }
        /**
         *
         * @param origin
         * @param pTranslateX  为正的对左边剪切
         * @param pTranslateY  为正对上面剪切
         * @return
         */
        private Bitmap shearBitmapXY(Bitmap origin,int pTranslateX,int pTranslateY) {

            Bitmap bmp =Bitmap.createBitmap(origin.getWidth(),origin.getHeight(), Bitmap.Config.ARGB_8888);
            if (origin == null) {
                return bmp ;
            }
            int width = origin.getWidth();
            int height = origin.getHeight();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {

                    int pixel = origin.getPixel(i,j);

                    if(pTranslateY>0){
                        if((j-pTranslateY)>0)
                            bmp.setPixel(i,j, pixel);
                        else
                            continue;
                    }else {
                        if((height+pTranslateY-j)>0)
                            bmp.setPixel(i,j, pixel);
                        else
                            continue;
                    }
                    if(pTranslateX>0){
                        if((i-pTranslateX)>0)
                            bmp.setPixel(i,j, pixel);
                        else
                            break;
                    }else {
                        if((width+pTranslateX-i)>0)
                            bmp.setPixel(i,j, pixel);
                        else
                            break;

                    }

                }
            }

            return bmp;
        }
        //填充图片边边一个像素
        private void fillOnePixelBitmap(Bitmap origin){
            if (origin == null) {
                return;
            }
            int width = origin.getWidth();
            int height = origin.getHeight();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {

                    if(i==(width-1)){
                        int pixel = origin.getPixel(i-1,j);
                        origin.setPixel(i,j,pixel);
                    }
                    if(j==0){
                        int pixel2 = origin.getPixel(i,j+1);
                        origin.setPixel(i,j,pixel2);
                    }
                }
            }
        }
        @Override
        public void capture(String path,String pathrequest,String pathscale) throws RemoteException {
            synchronized (BITMAP) {

                if (BITMAP == null || BITMAP.isRecycled()) {
                    BITMAP = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);
                }
                fillOnePixelBitmap(BITMAP);
               // JniUtils.fillOnePixelBitmap(BITMAP);
                while (BITMAP.isRecycled()){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                int width = BITMAP.getWidth();
                int heigth = BITMAP.getHeight();
                //20170723注释
                //将bitmap转换成int数组 :
                // int[] pixelsArrayBuffer = new int[width * heigth];//保存所有的像素的数组，图片宽×高
                //BITMAP.getPixels(pixelsArrayBuffer, 0, width, 0, 0, width, heigth);


                String stringBuff = "";

                int multiple = CameraServerHandler.ccontrast;
                //2016.07.17 新增 :　根据选择的物镜倍数更新　标尺的单位数值　：
                if (multiple == 5) {     //5x 物镜 : 标尺单位数值100 um
                    //TODO :
                    stringBuff = "100 um";
                } else if (multiple == 10) {     //10x 物镜 : 标尺单位数值100 um
                    //TODO :
                    stringBuff = "50 um";
                } else if (multiple == 20) {     //20x 物镜 : 标尺单位数值100 um
                    //TODO :
                    stringBuff = "25 um";
                } else if (multiple == 50) {     //20x 物镜 : 标尺单位数值100 um
                    //TODO :
                    stringBuff = "1 um";
                } else {
                    stringBuff = "50 um";
                }

                Bitmap newb = Bitmap.createBitmap(width, heigth, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图

                Canvas cv = new Canvas(newb);

                cv.drawBitmap(BITMAP, 0, 0, null);

                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                paint.setColor(Color.WHITE);
                paint.setTextSize((int) (18 * scale));
                paint.setDither(true); //获取跟清晰的图像采样
                paint.setFilterBitmap(true);//过滤一些
                Rect bounds = new Rect();
                paint.getTextBounds(stringBuff, 0, stringBuff.length(), bounds);
                int x=0,y=0;
                if(ConstantUtil.isV820Pad){
                    //
                    x =95;
                    y = 73;
                }

                if(CameraServerHandler.csaturation==2||CameraServerHandler.csaturation==3||CameraServerHandler.csaturation==4){
                    cv.drawText(stringBuff, 1096-pTranlateX-x, 925-pTranlateY-y, paint);
                    paint.setStrokeWidth((float) 4.0);
                    cv.drawLine(1100-pTranlateX-x, 950-pTranlateY-y, 1180-pTranlateX-x, 950-pTranlateY-y, paint);//------
                    cv.drawLine(1102-pTranlateX-x, 940-pTranlateY-y, 1102-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1140-pTranlateX-x, 940-pTranlateY-y, 1140-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1178-pTranlateX-x, 940-pTranlateY-y, 1178-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    paint.setStrokeWidth((float) 2.0);
                    cv.drawLine(1108-pTranlateX-x, 945-pTranlateY-y, 1108-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1116-pTranlateX-x, 945-pTranlateY-y, 1116-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1124-pTranlateX-x, 945-pTranlateY-y, 1124-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1132-pTranlateX-x, 945-pTranlateY-y, 1132-pTranlateX-x, 950-pTranlateY-y, paint);//|

                    cv.drawLine(1148-pTranlateX-x, 945-pTranlateY-y, 1148-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1156-pTranlateX-x, 945-pTranlateY-y, 1156-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1164-pTranlateX-x, 945-pTranlateY-y, 1164-pTranlateX-x, 950-pTranlateY-y, paint);//|
                    cv.drawLine(1172-pTranlateX-x, 945-pTranlateY-y, 1172-pTranlateX-x, 950-pTranlateY-y, paint);//|
                }else {
                    cv.drawText(stringBuff, 1096-x, 925-y, paint);
                    paint.setStrokeWidth((float) 4.0);
                    cv.drawLine(1100-x, 950-y, 1180-x, 950-y, paint);//------
                    cv.drawLine(1102-x, 940-y, 1102-x, 950-y, paint);//|
                    cv.drawLine(1140-x, 940-y, 1140-x, 950-y, paint);//|
                    cv.drawLine(1178-x, 940-y, 1178-x, 950-y, paint);//|
                    paint.setStrokeWidth((float) 2.0);
                    cv.drawLine(1108-x, 945-y, 1108-x, 950-y, paint);//|
                    cv.drawLine(1116-x, 945-y, 1116-x, 950-y, paint);//|
                    cv.drawLine(1124-x, 945-y, 1124-x, 950-y, paint);//|
                    cv.drawLine(1132-x, 945-y, 1132-x, 950-y, paint);//|

                    cv.drawLine(1148-x, 945-y, 1148-x, 950-y, paint);//|
                    cv.drawLine(1156-x, 945-y, 1156-x, 950-y, paint);//|
                    cv.drawLine(1164-x, 945-y, 1164-x, 950-y, paint);//|
                    cv.drawLine(1172-x, 945-y, 1172-x, 950-y, paint);//|
                }


                cv.save(Canvas.ALL_SAVE_FLAG);// 保存

                cv.restore();// 存储

                String substring = path.substring(0, path.lastIndexOf("/") + 1);



                pathscale = pathscale.substring(0, pathscale.indexOf("."));
                pathscale += ".scaled.bmp";
                FileUtils.capture(newb, pathscale, true, false);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                pathrequest = pathrequest.substring(0, pathrequest.indexOf("."));
                FileUtils.capture(newb, pathrequest + ".scaled.jpg", false, true);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                FileUtils.capture(newb, path, false, false);
                //清除bitmap
                if (newb != null) {
                    newb.recycle();
                    newb = null;
                }
                isFill  = false;
                if(!BITMAP.isRecycled()){
                    BITMAP.recycle();
                }
                System.gc();

            }
        }
    };

    private void stopMethod() {
        isStopProcess = true;
        mThreadPool.schedule(new Runnable() {
            @Override
            public void run() {
                if (!BITMAP.isRecycled()) {
                    BITMAP.recycle();
                }
                Process.killProcess(Process.myPid());
                System.gc();
               // System.exit(0);
                isStopProcess = false;
            }


        }, 1000, TimeUnit.MILLISECONDS);
    }

    //2016.05.25 新增 :
    //定义Parcel的 序列化令牌 DESCRIPTOR :
    private static final String DESCRIPTOR = "com.yeespec.libuvccamera.uvccamera.service.UVCService";
    //定义一系列判断标志 :
    public static final int TRANSACTION_imageData = 0;  //方法标识码  传输图像数据信息 ;
    public static final int TRANSACTION_USBDEVICE_LIST = 1;  //方法标识码  传输USB Monitor对象队列list ;


    //=====================================================
    //2016.05.25 新增 :
    //通过继承Binder来实现IBinder类 :
    public class UVCServiceDataBinder extends Binder {

        /**
         * 获取当前图像的计数 :
         *
         * @return
         */
       /* public int getBitmapCount() {
            return mCountBitmap;
        }
*/
        //服务端：定义了一个Binder对象，并且重写其中的onTransact方法来处理来自客户端的请求，该Binder对象会注册到系统中，以便客户端绑定使用
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {

            switch (code) {
                case TRANSACTION_imageData://方法标识码  传输图像数据信息 ;
                    //transactionImageData(data, reply);
                    return true;
                case TRANSACTION_USBDEVICE_LIST:

                    data.enforceInterface(DESCRIPTOR);//读取系列化令牌，和客户端的data.writeInterfaceToken对应
                    if ((0 != data.readInt())) {
                        //                        int arg0 = data.readInt();//读取参数
                        //                        int arg1 = data.readInt();//读取参数
                        //                        mScaleX = data.readFloat();//读取参数
                        //                        mScaleY = data.readFloat();//读取参数
                    } else {
                        //TODO
                    }
                    reply.writeNoException();
                    reply.writeInt(1);
                    //                    reply.writeInt(mCountBitmap);    //回写结果值        返回图像bitmap计数值

                    //                    reply.writeParcelable(mUSBMonitor.getDeviceList().get(0), 1);
                    //                    UsbDevice mUsbDevi = reply.readParcelable(UsbDevice.class.getClassLoader());
                    Log.v("mytestlist", "reply.writeTypedList " );
                    reply.writeTypedList(mUSBMonitor != null ? (ArrayList<? extends Parcelable>) mUSBMonitor.getDeviceList() : null);

                    //                    List<UsbDevice> usbDevices = new ArrayList<>();
                    //                    reply.readTypedList(usbDevices, UsbDevice.CREATOR);

                    return true;

            }
            return super.onTransact(code, data, reply, flags);
        }

      /*  private void transactionImageData(Parcel data, Parcel reply) {
            //======================================================
            //2016.05.25 新增 :
            //            BYTE_BUFFER_ARRAY.clear();
            //            BYTE_BUFFER_ARRAY.put(frame);


            //                    //                    mStandardDeviation ++ ;
            //
            //                    //                    if (BITMAP != null & BITMAP.getByteCount() > 0) {
            //                    //                        // BITMAP.getPixel(10, 10);
            //

            //================================问题分析 :====================================
            //发现了问题是Junit里面，在其他线程运行结束之前主线程结束了导致的。

            EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
                @Override
                public void run() {

                    //                            mWhiteCount = 0;
                    //                            mGrayCount = 0;

                    long whiteCount = 0;
                    long grayCount = 0;

                    //获取每一行的像素 :
                    //                        final int rowBytes = BITMAP.getRowBytes();
                    //                        mRowBytes = rowBytes;

                    if (BITMAP == null | BITMAP.isRecycled()) {
                        return;
                    }

                    final int width = BITMAP.getWidth();
                    mWidth = width;
                    final int heigth = BITMAP.getHeight();
                    mHeigth = heigth;

                    int xCoordinate = (int) (width * mScaleX);
                    int yCoordinate = (int) (heigth * mScaleY);

                    //2016.06.02 新增 : 添加一个用于 判断截取的图像方块是否越出屏幕边界的判断 :
                    if ((xCoordinate + CAPTURE_WIDTH) > width) {     //x方向超出屏幕范围 :
                        xCoordinate = width - CAPTURE_WIDTH - 5;   //x坐标倒退到 有效范围
                    }
                    if ((yCoordinate + CAPTURE_HEIGTH) > heigth) {     //Y方向超出屏幕范围 :
                        yCoordinate = heigth - CAPTURE_HEIGTH - 5;   //Y坐标倒退到 有效范围
                    }

                    //获取图片的总像素 :
                    //                int byteCount = BITMAP.getByteCount();
                    //                            int byteCount = width * heigth;
                    int byteCount = CAPTURE_WIDTH * CAPTURE_HEIGTH;

                    mByteCount = byteCount;

                    int row = 0;
                    int column = 0;

                    final int[] byteArrayBuffer = new int[byteCount / mStepLength];
                    int sum = 0;
                    //                //每16个像素采样一个点 :
                    //                for (row = 0; row < heigth; ) {
                    //                    for (int i = 0; i <= byteCount; i += 16) {
                    //                        //递增计算row行的值 :
                    //                        if ((i / width > row))
                    //                            row++;
                    //                        //计算column列的值 :
                    //                        column = i % width;
                    //
                    //                        int pixel_ARGB = BITMAP.getPixel(column, row);
                    //                        //截取蓝色 Blue 值 :
                    //                        byteArrayBuffer[i / 16] = pixel_ARGB & 0x000000ff;
                    //                        //                        byteArrayBuffer[i / 16] = pixel_ARGB & 0x000f;      //ARGB_8888模式 ;或ARGB_565
                    //                        //                                        byteArrayBuffer[i / 16] = (pixel_ARGB >> 4) & 0x000f;      //RGBX_8888模式 ;或RGBX_565
                    //
                    //                        //计算总和 :
                    //                        sum += byteArrayBuffer[i / 16];
                    //                    }
                    //                }


                    //2016.05.31 修改 :　从全幅图像采样改为　以触摸点（ｘ，ｙ）开始的　300*300的图像块 :

                    //将bitmap转换成int数组 :
                    //                            mPixelsArrayBuffer = new int[byteCount];//保存所有的像素的数组，图片宽×高
                    //                            BITMAP.getPixels(mPixelsArrayBuffer, 0, width, 0, 0, width, heigth);

                    //---------------------------------------------------------------
                    //2016.06.01 修改 : 从Bitmap中截取对焦块 :
                    mPixelsArrayBuffer = new int[CAPTURE_BYTE_COUNT];//保存所有的像素的数组，图片宽×高
                    if(BITMAP!=null)
                    BITMAP.getPixels(mPixelsArrayBuffer, 0, CAPTURE_WIDTH, xCoordinate, yCoordinate, CAPTURE_WIDTH, CAPTURE_HEIGTH);

                    mLayerMaximal = 0;
                    mLayerMinium = 0;

                    //每16个像素点采样一个 :      Pixel Format : GGL_PIXEL_FORMAT_RGBA_8888 :
                    for (int i = 0; i <= mPixelsArrayBuffer.length - 1; i += mStepLength) {
                        int index = i / mStepLength;
                        int clr = mPixelsArrayBuffer[i];
                        int blue = clr & 0x000000ff; //取低两位

                        //                                int green = (clr & 0x0000ff00) >> 8; //取中两位
                        //                                int red = (clr & 0x00ff0000) >> 16; //取高两位
                        //                                //                                int blue = (clr & 0xff000000) >> 24; //取低两位 Pixel Format : GGL_PIXEL_FORMAT_RGBA_8888 :
                        //                                //                    System.out.println("r="+red+",g="+green+",b="+blue);
                        //
                        //                                //RGB转灰度图 : 求灰阶 :
                        //                           *//* 所以16位运算下最好的计算公式是使用7位精度，
                        //                           比先前那个系数缩放100倍的精度高，而且速度快：
                        //                            Gray = (R*38 + G*75 + B*15) >> 7
                        //                            *//*
                        //                                //7位进度 :
                        //                                //                    float gray = (red * 38 + green * 75 + blue * 15) >> 7 ;
                        //                                //20位进度 : Gray = (R*313524 + G*615514 + B*119538) >> 20
                        //                                float gray = (red * 313524 + green * 615514 + blue * 119538) >> 20;

                        int gray = blue;

                        byteArrayBuffer[index] = gray;

                        //计算总和 :
                        sum += byteArrayBuffer[index];

                        //第二层比较计算反差值 : 层间的计算 :
                        if (index >= CAPTURE_WIDTH) {
                            if ((index % CAPTURE_WIDTH) == 0) {
                                int midContrast = mLayerMaximal - mLayerMinium;

                                if ((index / CAPTURE_WIDTH) == 1) {  //采集第一层灰度反差值时 , midContrast既是最大值 , 也是最小值 ;
                                    mContrast = midContrast;
                                }
                                if (midContrast > mContrast) {
                                    mContrast = midContrast;
                                    mMaximal = mLayerMaximal;
                                    mMinium = mLayerMinium;
                                }
                                //计算完一层后 , 初始化最大最小值 , 进行下一层的反差比较计算 :
                                mLayerMaximal = 0;
                                mLayerMinium = 0;
                            }
                        }

                        //第一层比较计算反差值 : 层内的计算 :
                        if ((index % CAPTURE_WIDTH) == 0)
                            mLayerMinium = gray;   //采集各层第一个灰度值时 , gray既是最大值 , 也是最小值 ;

                        if (gray > mLayerMaximal)
                            mLayerMaximal = gray;
                        if (gray < mMinium)
                            mLayerMinium = gray;

             *//*               //2016.06.01 新增 :
                            float mLayerMinium = 0 ;
                            float mLayerMaximal = 0 ;

                            //最终的最大反差值 :
                            float mContrast = 0 ;*//*

                        //                                //上一层的各层反差值 , 最大 / 最小 :
                        //                                public float mContrastLayerMinium = 0 ;
                        //                                public float mContrastLayerMaximal = 0 ;

                    }

                    //2016.07.19 :
                    //标准偏差：显示层次值的变换幅度,该值越小,所有像素的色调分布越靠近平均值.
                    //标准偏差公式 平方 =  ((n1 - 平均值)平方 + (n2 - 平均值)平方 + ...) /(像素总数 - 1)


                    //-------------------------------------------------------
                    // TODO: 2016/7/2 : 修改图像采样的 整体逻辑 :
                    // 采样整张图像转成灰度值数组 , 从中处理两方面的数据 : 统计灰白像素比 , 用于控制视场亮度 ;
                    //计算对焦点的反差值 , 用于自动对焦 ;

                    //                            //2016.07.03 修改 : 配合修改后的自动亮度调整 :
                    //                            mPixelsArrayBuffer = new int[mWidth * mHeigth];//保存所有的像素的数组，图片宽×高
                    //                            BITMAP.getPixels(mPixelsArrayBuffer, 0, mWidth, 0, 0, mWidth, mHeigth);
                    //
                    //                            mLayerMaximal = 0;
                    //                            mLayerMinium = 0;
                    //
                    //                            long startIndex = (mWidth * yCoordinate) + xCoordinate;
                    //                            long endIndex = (mWidth * (yCoordinate + CAPTURE_HEIGTH)) + xCoordinate + CAPTURE_WIDTH;
                    //                            long yIncrement = 0;        //用于记录自增长的Y轴索引 ;
                    //                            long startRangeIndex = (mWidth * (yCoordinate + yIncrement)) + xCoordinate;      //范围索引 ; 即符合对焦点采样的范围索引 ;
                    //                            long endRangeIndex = (mWidth * (yCoordinate + yIncrement)) + xCoordinate + CAPTURE_WIDTH;      //范围索引 ; 即符合对焦点采样的范围索引 ;
                    //
                    //                            //每16个像素点采样一个 :      Pixel Format : GGL_PIXEL_FORMAT_RGBA_8888 :
                    //                            for (int i = 0; i <= mPixelsArrayBuffer.length - 1; i += mStepLength) {
                    //                                int index = i / mStepLength;
                    //                                int clr = mPixelsArrayBuffer[i];
                    //                                int red = (clr & 0x00ff0000) >> 16; //取高两位
                    //                                int green = (clr & 0x0000ff00) >> 8; //取中两位
                    //                                int blue = clr & 0x000000ff; //取低两位
                    //                                //                                int blue = (clr & 0xff000000) >> 24; //取低两位 Pixel Format : GGL_PIXEL_FORMAT_RGBA_8888 :
                    //                                //                    System.out.println("r="+red+",g="+green+",b="+blue);
                    //
                    //                                //RGB转灰度图 : 求灰阶 :
                    //                                                                           *//* 所以16位运算下最好的计算公式是使用7位精度，
                    //                                                                           比先前那个系数缩放100倍的精度高，而且速度快：
                    //                                                                            Gray = (R*38 + G*75 + B*15) >> 7
                    //                                                                            *//*
                    //                                //7位进度 :
                    //                                //                    float gray = (red * 38 + green * 75 + blue * 15) >> 7 ;
                    //                                //20位进度 : Gray = (R*313524 + G*615514 + B*119538) >> 20
                    //                                float gray = (red * 313524 + green * 615514 + blue * 119538) >> 20;
                    //
                    //                                //添加灰白像素的比较阈值 :此处暂定为100 , 大于 100 统计为白 ; 小于100 统计为黑灰 ;
                    //                                if (gray > THRESHOLD_VALUE) {
                    //                                    //                                    mWhiteCount++;
                    //                                    whiteCount++;
                    //
                    //                                } else {
                    //                                    //                                    mGrayCount++;
                    //                                    grayCount++;
                    //                                }
                    //
                    //
                    //                                byteArrayBuffer[index] = gray;
                    //
                    //                                //计算总和 :
                    //                                sum += byteArrayBuffer[index];
                    //
                    //                                if ((index > startIndex) & (index < endIndex)) {    //到达对焦点的第一个开始坐标 :
                    //
                    //                                    if (index > startRangeIndex) {
                    //
                    //                                        //第二层比较计算反差值 : 层间的计算 :
                    //                                        if (index >= endRangeIndex) {
                    //                                            if ((index % mWidth) == 0) {
                    //                                                if (yIncrement < CAPTURE_HEIGTH) {
                    //                                                    yIncrement++;
                    //                                                } else
                    //                                                    yIncrement = 0;
                    //                                                startRangeIndex = (mWidth * (yCoordinate + yIncrement)) + xCoordinate;
                    //                                                endRangeIndex = (mWidth * (yCoordinate + yIncrement)) + xCoordinate + CAPTURE_WIDTH;
                    //
                    //                                                float midContrast = mLayerMaximal - mLayerMinium;
                    //
                    //                                                if ((index / mWidth) == yCoordinate + 1) {  //采集第一层灰度反差值时 , midContrast既是最大值 , 也是最小值 ; 取整 ;
                    //                                                    mContrast = midContrast;
                    //                                                }
                    //                                                if (midContrast > mContrast) {
                    //                                                    mContrast = midContrast;
                    //                                                    mMaximal = mLayerMaximal;
                    //                                                    mMinium = mLayerMinium;
                    //                                                }
                    //                                                //计算完一层后 , 初始化最大最小值 , 进行下一层的反差比较计算 :
                    //                                                mLayerMaximal = 0;
                    //                                                mLayerMinium = 0;
                    //                                            }
                    //                                        }
                    //
                    //                                        //第一层比较计算反差值 : 层内的计算 :
                    //                                        if ((index % mWidth) == yCoordinate)
                    //                                            mLayerMinium = gray;   //采集各层第一个灰度值时 , gray既是最大值 , 也是最小值 ;
                    //
                    //                                        if (gray > mLayerMaximal)
                    //                                            mLayerMaximal = gray;
                    //                                        if (gray < mMinium)
                    //                                            mLayerMinium = gray;
                    //
                    //                                                                         *//*               //2016.06.01 新增 :
                    //                                                                                        float mLayerMinium = 0 ;
                    //                                                                                        float mLayerMaximal = 0 ;
                    //
                    //                                                                                        //最终的最大反差值 :
                    //                                                                                        float mContrast = 0 ;*//*
                    //
                    //                                        //                                //上一层的各层反差值 , 最大 / 最小 :
                    //                                        //                                public float mContrastLayerMinium = 0 ;
                    //                                        //                                public float mContrastLayerMaximal = 0 ;
                    //
                    //                                    }
                    //
                    //                                }
                    //
                    //                            }
                    //
                    //                            mWhiteCount = whiteCount;
                    //                            mGrayCount = grayCount;


                    //--------------------------------------------------------
                    //                            mContrast = mContrastLayerMaximal - mContrastLayerMinium;

                    //                            mStandardDeviation = mMaximal - mMinium;
                    //                            mStandardDeviation = mContrast;

                    mSum = sum;


                    //====================2016.07.20 修改 =====================
                    EXECUTOR_SERVICE_SCHEDULED.schedule(new Runnable() {
                        @Override
                        public void run() {

                            mByteArrayBuffer = byteArrayBuffer;
                            mArrayLength = byteArrayBuffer.length;

                            //标准差 :
                            int standardDeviation = 0;
                            //计算平均值 :

                            //==============================出现问题处 : 此线程程序跑到这里就被 正常终止 :========================================
                            int avg = mSum / byteArrayBuffer.length;
                            mSDSumAvg = byteArrayBuffer[byteArrayBuffer.length - 1];
                            int DValue = 0;
                            for (int i = 0; i <= byteArrayBuffer.length - 1; i++) {
                                DValue = byteArrayBuffer[i] - avg;     //算法精度不够
                                DValue = DValue * DValue;
                                standardDeviation += DValue;
                            }

                            mSDSum = standardDeviation;

                            standardDeviation = standardDeviation / byteArrayBuffer.length;
                            standardDeviation = (int) ((Math.sqrt(standardDeviation)) * 10);
                            mStandardDeviation = standardDeviation;   //乘以10 , 增加倍率 , 方便显示

                            mAvg = avg;

                        }
                    }, 0, TimeUnit.SECONDS);


                }
            }, 0, TimeUnit.SECONDS);


            data.enforceInterface(DESCRIPTOR);//读取系列化令牌，和客户端的data.writeInterfaceToken对应
            if ((0 != data.readInt())) {
                //                        int arg0 = data.readInt();//读取参数
                //                        int arg1 = data.readInt();//读取参数
                mScaleX = data.readFloat();//读取参数
                mScaleY = data.readFloat();//读取参数
            } else {
                //TODO
            }
            //                    int result = this.add(arg0,arg1);//本地方法
            reply.writeNoException();
            //                    reply.writeInt(result);//回写结果值

            //                    Bitmap bitmap = this.getBitmap();//调用本地方法getBitmap()获取位图 :
            //                    if (bitmap != null) {
            reply.writeInt(1);
            reply.writeInt(mCountBitmap);    //回写结果值        返回图像bitmap计数值


            reply.writeInt(mSum);              //发送总和
            reply.writeInt(mMinium);             //发送平均值
            reply.writeInt(mMaximal);        //发送数组长度length
            reply.writeInt(mContrast);
            reply.writeInt(mAvg);  //发送平均值
            reply.writeInt(mStandardDeviation);  //发送标准偏差

            reply.writeInt(mGrayCount);    //发送灰像素统计数值
            reply.writeInt(mWhiteCount);   //发送白像素统计数值


        }
*/

    }
}
