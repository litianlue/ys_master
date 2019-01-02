package com.yeespec.microscope.master.service;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.yeespec.R;
import com.yeespec.microscope.master.activity.MasterActivity;
import com.yeespec.microscope.master.receiver.AdminReceiver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class GuardService extends Service {
    private final String TAG = "GuardService";

    private Service mContext;

    private PowerManager mPowerManager;

    //2016.09.21 新增 : 通知条的ID :
    public static final int NOTIFICATION_ID = 0x125;   //通知条的ID号 ;

    //2016.09.21 : 取得系统服务
    DevicePolicyManager mPolicyManager;
    ComponentName mComponentName;

  /*  //2016.09.22 : 新增 : 亮度数值 :
    private static final int LIGHT_NORMAL = 64;
    private static final int LIGHT_50_PERCENT = 127;
    private static final int LIGHT_75_PERCENT = 191;
    private static final int LIGHT_100_PERCENT = 255;
    private static final int LIGHT_AUTO = 0;
    private static final int LIGHT_OFF = 0;
    private static final int LIGHT_ON = 255;
    private static final int LIGHT_ERR = -1;
*/
    // TODO: 2016/6/14  通过后台守护服务 ,开启双线程来实现n分钟无操作后 , 进入应用休眠模式 (包括屏保 , 灯光灭灯等);
    // 定义屏幕超时时间 :
    private int DELAY = 5 * 60 * 1000;      //5min

    //上一次User有动作的Time Stamp :
    private Date lastUpdateTime;
    //计算User有几秒没有动作的 :
    private long timePeriod;

    /* 静止超过N秒将自动进入屏保 */
    private float mHoldStillTime = 10;
    /*标识当前是否进入了屏保*/
    private boolean isRunScreenSaver;

    /*时间间隔*/
    private long intervalScreenSaver = 1000;
    private long intervalKeypadeSaver = 1000;

    KeyguardManager mKeyguardManager = null;
    private KeyguardManager.KeyguardLock mKeyguardLock = null;
    //定义接收屏保广播的接收器 : 监听接收屏幕亮、屏幕灭、屏幕 解锁三个事件ACTION_SCREEN_ON、ACTION_SCREEN_OFF、ACTION_USER_PRESENT三个Action ;
    BroadcastReceiver mMasterResetReciever;

    private static Method mReflectScreenState;

    //定义onBinder方法返回的对象 ;
    private ScreenSaverBinder mScreenSaverBinder = new ScreenSaverBinder();


    public GuardService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, intent.getPackage() + "connect to GuardService ...");
        //返回ScreenSaverBinder对象 ;
        return mScreenSaverBinder;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        Log.v(TAG, "startCommand..");

        //        new Thread(new Runnable() {
        //            @Override
        //            public void run() {
        //                DatagramSocket socket = null;
        //                byte buf[] = new byte[2];
        //                DatagramPacket pak = new DatagramPacket(buf, 2);
        //                for (; ; ) {
        //                    try {
        //                        if (socket == null) {
        //                            socket = new DatagramSocket(3451);
        //                            socket.setSoTimeout(1000);
        //                        }
        //                        socket.receive(pak);
        //
        //                        if (buf[0] == '5') {
        //
        //                            startMasterActivity();
        //
        //                            //reset buf content
        //                            buf[0] = buf[1] = '0';
        //                        }
        //                    } catch (Exception e) {
        //                        e.printStackTrace();
        //                    }
        //                }
        //            }
        //        }).start();

        // TODO: 2016/6/14  添加屏幕保护程序(应用保护) :
        /* 初始取得User可触碰屏幕的时间 */
        lastUpdateTime = new Date(System.currentTimeMillis());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
       // Log.v(TAG, "onCreate--");
        mContext = this;
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        try {
            mReflectScreenState = PowerManager.class.getMethod("isScreenOn", new Class[]{});
        } catch (Exception nsme) {
            nsme.printStackTrace();
          //  Log.d(TAG, "API < 7," + nsme);
        }

        super.onCreate();

        //2016.09.21 : 取得系统服务
        mPolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, AdminReceiver.class);

        //2016.09.21 : 启动设备管理权限
        //        startDeviceManager();

        //        //2016.09.20 : 添加startForeground将进程设置为前台进程 ;
        //        showForegroundNotification();

    }

    /*因为PowerManager提供的函数setBacklightBrightness接口是隐藏的，
     * 所以在基于第三方开发调用该函数时，只能通过反射实现在运行时调用
     */
   /* private void setLight(int light) {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            //得到PowerManager类对应的Class对象
            Class<?> pmClass = Class.forName(pm.getClass().getName());
            //得到PowerManager类中的成员mService（mService为PowerManagerService类型）
            Field field = pmClass.getDeclaredField("mService");
            field.setAccessible(true);
            //实例化mService
            Object iPM = field.get(pm);
            //得到PowerManagerService对应的Class对象
            Class<?> iPMClass = Class.forName(iPM.getClass().getName());
            *//*得到PowerManagerService的函数setBacklightBrightness对应的Method对象，
             * PowerManager的函数setBacklightBrightness实现在PowerManagerService中
             *//*
            Method method = iPMClass.getDeclaredMethod("setBacklightBrightness", int.class);
            method.setAccessible(true);
            //调用实现PowerManagerService的setBacklightBrightness
            method.invoke(iPM, light);

            Log.w(TAG, " onStart() # lightScreen() ! setLight() + " + light + " === ");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //2016.09.21 : 启动设备管理权限
    private void startDeviceManager() {
        //2016.09.21 : 取得系统服务

        //添加一个隐式意图，完成设备权限的添加
        //这个Intent （DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN）跳转到 权限提醒页面
        //并传递了两个参数EXTRA_DEVICE_ADMIN 、 EXTRA_ADD_EXPLANATION
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        //权限列表
        //EXTRA_DEVICE_ADMIN参数中说明了用到哪些权限,
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
        //描述(additional explanation)
        //EXTRA_ADD_EXPLANATION参数为附加的说明
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "--is locked--");
        intent.putExtra("force-locked", DeviceAdminInfo.USES_POLICY_FORCE_LOCK);
        //                mContext.startActivityForResult(intent,0);
        startActivity(intent);

        //        new BaseActivity().startActivityForResult(intent, 0);
    }

    private void showForegroundNotification() {
        //2016.09.20 : 添加startForeground将进程设置为前台进程 ;
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(this, MasterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notify = new Notification.Builder(this)
                .setAutoCancel(false)
                .setTicker(getResources().getString(R.string.ticker_notify))
                .setContentTitle(getResources().getString(R.string.title_notify))
                .setContentText(getResources().getString(R.string.text_notify))
                //                .setSmallIcon(R.mipmap.ic_microscope_cells_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pIntent)
                .build();

        startForeground(new Random().nextInt(100), notify);
        Log.w("test", TAG + " # showForegroundNotification() ");
    }
*/
    @Override
    public void onStart(Intent intent, int startId) {
       // Log.v(TAG, "onStart--");

        //        startScreenService();

        //2016.09.21 : 新增 :
        //        lightScreen();

        //设置通知内容并在onReceive()这个函数执行时开启
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher, "用电脑时间过长了！白痴！"
                , System.currentTimeMillis());
        notification.setLatestEventInfo(this, "快去休息！！！",
                "一定保护眼睛,不然遗传给孩子，老婆跟别人跑啊。", null);
        notification.defaults = Notification.DEFAULT_ALL;
        //        manager.notify(new Random().nextInt(100), notification);
        manager.notify(NOTIFICATION_ID, notification);

        Log.w("test_GuardService", " onStart() # lightScreen() ! === ");

        //                //再次开启LongRunningService这个服务，从而可以
        //                Intent i = new Intent(context, MasterIntentService.class);
        //                context.startService(i);

        //        new Timer().schedule(new TimerTask() {
        //            @Override
        //            public void run() {
        //
        //                while (!mPolicyManager.isAdminActive(mComponentName)) {
        //                    Log.w("test_GuardService", " onStart() # lightScreen() ! mPolicyManager.isAdminActive(mComponentName) == false === ");
        //                    startDeviceManager();
        //                }
        //
        //                //2016.09.21 : 新增 添加强制待机锁屏 :
        //                if (mPolicyManager.isAdminActive(mComponentName)) {
        //                    mPolicyManager.lockNow();
        //                    mPolicyManager.setMaximumTimeToLock(mComponentName, 0);
        //                    startDeviceManager();
        //                    Log.w("test_GuardService", " onStart() # lightScreen() ! === ");
        //                } else {
        //                    Log.w("test_GuardService", " onStart() # lightScreen() ! mPolicyManager.isAdminActive(mComponentName) == false === ");
        //                }
        //
        //                //                                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //
        //                //                setLight(2);
        //                //                Log.w("test_GuardService", " onStart() # lightScreen() ! setLight(2) === ");
        //
        //                //                GuardService.this.stopSelf();   //结束守护后台 ;
        //            }
        //        }, 1000 * 10);  //延时3s后熄屏重新待机 ;

        super.onStart(intent, startId);
    }

    private void startScreenService() {
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mKeyguardLock = mKeyguardManager.newKeyguardLock("");

        mKeyguardLock.disableKeyguard();

        //Intent.ACTION_SCREEN_OFF
        mMasterResetReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {    //接收到ACTION_SCREEN_ON广播 ;
                    // TODO: 2016/6/15 : 添加屏幕解锁点亮后 , 判断应用是否重启 , 是否在前台 : 重启超时计时 ;
                    if (isRunScreenSaver) {
                        //退出屏保标识 :
                        isRunScreenSaver = false;
                    }

                } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {     //接收到ACTION_SCREEN_OFF广播 ;
                    // TODO: 2016/6/15 : 接收到锁屏广播 , 暂停超时计时 ; 关闭LED , 关闭激发块 , 关闭系统 , 进入休眠 :
                    if (isRunScreenSaver == false) {
                        //进入屏保标识 :
                        isRunScreenSaver = true;
                    }

                } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                    // TODO: 2016/6/15 : 添加用户解锁后 , 判断应用是否重启 , 是否在前台 : 重启超时计时 ;
                    if (isRunScreenSaver) {
                        //退出屏保标识 :
                        isRunScreenSaver = false;
                    }

                }


                //                try {
                ////                    Intent i = new Intent();
                ////                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ////                    i.setClass(context, ScreenSaverActivity.class);
                ////                    context.startActivity(i);
                //
                //                } catch (Exception e) {
                //                    Log.i("mMasterResetReciever:", e.toString());
                //                }
            }
        };

        //启动screen状态广播接收器
        startScreenBroadcastReceiver();
    }

    /**
     * 启动screen状态广播接收器
     */
    private void startScreenBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);       //ACTION_USER_PRESENT是解锁动作 ;
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);                           //设置动态接收器的优先级为最高 ;
        mContext.registerReceiver(mMasterResetReciever, filter);
    }

    /**
     * 因为屏幕在灭了以后是否锁定可以由用户设置，所以我们不能通过跟踪解锁事件来判断其状态，需要一个可以时时获取状态的函数：
     *
     * @param c
     * @return
     */
    //判断屏幕是否被锁定
    public final static boolean isScreenLocked(Context c) {
        KeyguardManager mKeyguardManager = (KeyguardManager) c.getSystemService(c.KEYGUARD_SERVICE);
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    /**
     * screen是否打开状态
     */
    private static boolean isScreenOn(PowerManager pm) {
        boolean screenState;
        try {
            screenState = (Boolean) mReflectScreenState.invoke(pm);
        } catch (Exception e) {
            e.printStackTrace();
            screenState = false;
        }
        return screenState;
    }

    private void lightScreen() {
        /**
         * WakeLock如果是一个没有超时的锁 , 而且这个锁没有释放 , 那么系统就无法进入休眠 ; WakeLock这个锁机制 可以被用户态程序和内核获得 ;
         * 这个锁可以是超时的或者没有超时的 , 超时的锁会在超时以后自动解锁. 如果没有了锁 或者超时了 , 内核就会启动休眠的那套机制来进入休眠 ;
         */
        //点亮
        // 获取PowerManager的实例
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "bright");
        //        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        //        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "bright");
        //        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "bright");
        //        wakeLock.acquire();     //获取唤醒锁
        if (!wakeLock.isHeld()) {
            // 唤醒屏幕
            wakeLock.acquire();     //获取唤醒锁
        }

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unlock");
        //        kl.disableKeyguard();
        if (km.inKeyguardRestrictedInputMode()) {
            // 解锁键盘
            kl.disableKeyguard();
        }

        kl.reenableKeyguard();
        wakeLock.release();     //释放唤醒锁

        Log.w("test_MasterIntSer", "lightScreen() ! === ");

        // TODO: 2016/6/14 添加屏幕超时自动关闭
        //设置下面的参数可以达到效果 :
        //Settings.System.SCREEN_OFF_TIMEOUT sample code.
        //        long defTimeOut = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, DELAY);  // 获取原来的超时时间ms
        //        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, DELAY); //设置更新的超时时间ms

    }


    Handler handler = new Handler();

    private void startMasterActivity() {
        //        lightScreen();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAppOnForeground()) {
                    Intent intent = new Intent(mContext, MasterActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                } else {
                    Log.v(TAG, "not started---");
                }
            }
        }, 2000);
    }

    public boolean isAppOnForeground() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = getApplicationContext().getPackageName();
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName) && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy--");
        super.onDestroy();

        unregisterReceiver(mMasterResetReciever);

        //2016.09.21 :　删除通知条 ;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);

        GuardService.this.stopSelf();   //结束守护后台 ;
    }

    // TODO: 2016/6/14 新增 : 双线程实现计时和屏保休眠计时 :
    /**
     * 计时线程
     */
    private Runnable mTask01 = new Runnable() {

        @Override
        public void run() {
            Date timeNow = new Date(System.currentTimeMillis());
            /* 计算User静止不动作的时间间距 */
            /**当前的系统时间 - 上次触摸屏幕的时间 = 静止不动的时间**/
            timePeriod = (long) timeNow.getTime() - (long) lastUpdateTime.getTime();

            /*将静止时间毫秒换算成秒*/
            float timePeriodSecond = ((float) timePeriod / 1000);

            if (timePeriodSecond > mHoldStillTime) {
                if (isRunScreenSaver == false) {  //说明没有进入屏保
                    /* 启动线程去显示屏保 */
                    //                    mHandler02.postAtTime(mTask02, intervalScreenSaver);
                    /*显示屏保置为true*/
                    isRunScreenSaver = true;

                    //创建Intent对象 :
                    Intent intent = new Intent();
                    //设置Intent的Action属性 :
                    intent.setAction(Intent.ACTION_SCREEN_OFF);
                    intent.putExtra("msg", "time out to screen saver ! ...");
                    //发送锁屏广播 : 无序广播 , 所有监听器都可以监听到 ;(还是 有序广播 ?)
                    //                    sendBroadcast(intent);
                    sendOrderedBroadcast(intent, null);     //发送有序广播 ;

                } else {
                    /*屏保正在显示中*/
                }
            } else {
                /*说明静止之间没有超过规定时长*/
                isRunScreenSaver = false;
            }
            /*反复调用自己进行检查*/
            //            mHandler01.postDelayed(mTask01, intervalKeypadeSaver);
        }
    };

    /**
     * 持续屏保显示线程
     */
    private Runnable mTask02 = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (isRunScreenSaver == true) {  //如果屏保正在显示，就计算不断持续显示
                //              hideOriginalLayout();
                //                showScreenSaver();
                //                mHandler02.postDelayed(mTask02, intervalScreenSaver);
            } else {
                //                mHandler02.removeCallbacks(mTask02);  //如果屏保没有显示则移除线程
            }
        }
    };


    //通过继承Binder来实现IBinder类 :
    public class ScreenSaverBinder extends Binder {

        public void setCount() {
            //有用户操作时 , 更新初始化超时时间 :


        }

        /*用户有操作的时候不断重置静止时间和上次操作的时间*/
        public void updateUserActionTime() {
            Date timeNow = new Date(System.currentTimeMillis());
            timePeriod = timeNow.getTime() - lastUpdateTime.getTime();
            lastUpdateTime.setTime(timeNow.getTime());
        }

    }


}

