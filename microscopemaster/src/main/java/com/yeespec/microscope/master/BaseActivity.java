package com.yeespec.microscope.master;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVSaveOption;
import com.avos.avoscloud.GetCallback;
import com.avos.avoscloud.SaveCallback;
import com.yeespec.R;
import com.yeespec.libuvccamera.usb.CameraDialog;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.master.receiver.AdminReceiver;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.RequesUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by virgilyan on 15/11/3.
 */
public class BaseActivity extends Activity {

    protected static final boolean DEBUG = false;
    public static String TAG = BaseActivity.class.getSimpleName();

    //private PowerManager powerManager = null;
    //private PowerManager.WakeLock wakeLock = null;

    //2016.09.21 新增 : 通知条的ID :
    public static final int NOTIFICATION_ID = 0x125;   //通知条的ID号 ;
    public static final int REQUEST_CODE = 0x222;   //通知条的ID号 ;

    //2016.09.21 : 取得系统服务
    DevicePolicyManager mPolicyManager;
    ComponentName mComponentName;
    private boolean isregistReceiver=false;
    protected TextView returnView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        //this.wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

        if (getIntent().getBooleanExtra("startScreenAlarmService", false)) {
            //Log.w(TAG, "BaseActivity onCreate()# startScreenAlarmService == true ");
            //2016.09.21 : 取得系统服务
            mPolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            mComponentName = new ComponentName(this, AdminReceiver.class);
            //            startDeviceManager();
            screenWakeLock();

        } else {
           // Log.w(TAG, "BaseActivity onCreate()# startScreenAlarmService == false ");
            initView();
            initListener();
            hideVirtualButtons();

            //2016.07.14 : 在父类添加注册电量变化的广播 :
            getBatteryPercentage();

            //2016.09.19 : 申请获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行 ;
            acquireWakeLock();
        }

        //        //2016.12.31 : 初始化Vitamio视频播放器 ;
        //        Vitamio.initialize(this);   //初始化的时候调用的是 Vitamio.initialize(this);而不是Vitamio.isInitialized(this); 在Application里初始化

        //        if (!Vitamio.isInitialized(this)) {
        //            Log.w(TAG, "!Vitamio.isInitialized(ctx) = true !");
        //        } else
        //            Log.w(TAG, "!Vitamio.isInitialized(ctx) = false !");

    }

    public void screenWakeLock() {
        //        startScreenService();

        //2016.09.21 : 新增 :
        lightScreen();

        //设置通知内容并在onReceive()这个函数执行时开启
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher, "用电脑时间过长了！白痴！"
                , System.currentTimeMillis());
        notification.setLatestEventInfo(this, "快去休息！！！",
                "一定保护眼睛,不然遗传给孩子，老婆跟别人跑啊。", null);
        notification.defaults = Notification.DEFAULT_ALL;
        //        manager.notify(new Random().nextInt(100), notification);
        manager.notify(NOTIFICATION_ID, notification);

       // Log.w("test_BaseActivity", " onStart() # lightScreen() ! === ");

        //                //再次开启LongRunningService这个服务，从而可以
        //                Intent i = new Intent(context, MasterIntentService.class);
        //                context.startService(i);

        //        new Timer().schedule(new TimerTask() {
        //            @Override
        //            public void run() {
        //
        //                while (!mPolicyManager.isAdminActive(mComponentName)) {
        //                    Log.w("test_BaseActivity", " screenWakeLock() # lightScreen() ! mPolicyManager.isAdminActive(mComponentName) == false === ");
        //                    startDeviceManager();
        //                }
        //
        //                //2016.09.21 : 新增 添加强制待机锁屏 :
        //                if (mPolicyManager.isAdminActive(mComponentName)) {
        //                    mPolicyManager.lockNow();
        //                    mPolicyManager.setMaximumTimeToLock(mComponentName, 0);
        //                    startDeviceManager();
        //                    Log.w("test_BaseActivity", " screenWakeLock() # lightScreen() ! === ");
        //                } else {
        //                    Log.w("test_BaseActivity", " screenWakeLock() # lightScreen() ! mPolicyManager.isAdminActive(mComponentName) == false === ");
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
       // wakeLock.release();     //释放唤醒锁
        if (wakeLock!= null) {

            wakeLock.release();
            wakeLock = null;

        }


        // TODO: 2016/6/14 添加屏幕超时自动关闭
        //设置下面的参数可以达到效果 :
        //Settings.System.SCREEN_OFF_TIMEOUT sample code.
        //        long defTimeOut = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, DELAY);  // 获取原来的超时时间ms
        //        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, DELAY); //设置更新的超时时间ms

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(TAG, "------ " + " onActivityResult " + " ------");

        //2016.09.22 : 新增 :
        if (REQUEST_CODE == requestCode) {
            Log.w(TAG, "------ " + "BaseActivity # onActivityResult() " + " 获取权限 ! ------");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initView() {
        returnView = (TextView) findViewById(R.id.btn_return);
    }

    private void initListener() {
        if (returnView != null)
            returnView.setOnClickListener(onClickListener);
    }

    public View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_return:
                    //finish();
                    break;
            }
        }
    };

    public interface OnEnableViewListener {
        void enableView(final boolean enable);
    }

    protected BroadcastReceiver batteryLevelReceiver;

    public static long time = 0l;

    /**
     * 计算电池百分比
     */
   // private int delayCount=0;
    protected void getBatteryPercentage() {
        batteryLevelReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {

               /* float currentLevel = intent.getFloatExtra(BatteryManager.EXTRA_LEVEL, -1f);
              //  float scale = intent.getFloatExtra(BatteryManager.EXTRA_SCALE, -1f);

                if (currentLevel >= 0 ) {

                    final int level = (int) (((currentLevel - 9.6f) * 100) / (12.0 - 9.6f));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConstantUtil.Level = level;
                            //如果电压小于或者等于10%V则关机
                            if( ConstantUtil.Level <=10&& ConstantUtil.Level >1){
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
                            }
                        }
                    });
                    delayCount++;

                    if (ConstantUtil.Level  <= 50&&delayCount==6) {
                        delayCount =0;
                        if(!isOpen)
                            initElectricityAlert(BaseApplication.getContext(), ConstantUtil.Level );
                    }
                }*/
            }
        };
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
        isregistReceiver = true;
    }

    private Dialog dialog;
    protected boolean isOpen = false;

    protected void initElectricityAlert(Context context, int electrity) {
        dialog = new Dialog(context, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_electric_toast);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        Button button = (Button) dialog.findViewById(R.id.electricity_ok_btn);
        TextView electTextView = (TextView) dialog.findViewById(R.id.toast_elect);
        TextView msgTextView = (TextView) dialog.findViewById(R.id.toast_msg);
        if (electrity <= 30 && electrity > 10) {
            electTextView.setText("电池只剩" + electrity + "%");
            msgTextView.setText("电量低\n(请充电)");
        } else if (electrity <= 20) {
            electTextView.setText("电池只剩" + electrity + "%");
            msgTextView.setText("准备关机\n(请充电)");
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog.isShowing())
                dialog.dismiss();
                isOpen = false;
                dialog = null;
            }
        });
        dialog.show();
        isOpen = true;
    }


    private void hideVirtualButtons() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    protected Dialog holdDialog;

    public void showHoldLoading() {
        holdDialog = new Dialog(this, R.style.dialog_loaing);
        View viewToLoad = LayoutInflater.from(this).inflate(R.layout.global_loading_dialog, null);
        holdDialog.setContentView(viewToLoad);
        holdDialog.setCancelable(true);
        holdDialog.setCanceledOnTouchOutside(false);
        holdDialog.show();
    }


    //隐藏加载提示对话框 :
    public void hideHoldLoading() {
        if (holdDialog != null && holdDialog.isShowing()) {
            holdDialog.dismiss();
            holdDialog = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if(ConstantUtil.remoteUserName!=null&&!ConstantUtil.remoteUserName.equals("")&&ConstantUtil.isEnbleRemote&&ConstantUtil.isStartDetection)
        //    openService(ConstantUtil.remoteUserName);
    }

    @Override
    protected void onPause() {
        super.onPause();
       // if(ConstantUtil.remoteUserName!=null&&!ConstantUtil.remoteUserName.equals("")&&ConstantUtil.isEnbleRemote&&ConstantUtil.isStartDetection)
         //   closeService(ConstantUtil.remoteUserName);
    }

    /**
     * @param usernama
     */
    private void closeService(final String usernama) {
        AVQuery<AVObject> query = new AVQuery<>(RequesUtils.CONNECT_STATE);
        query.whereEqualTo("user",usernama);
        query.getFirstInBackground(new GetCallback<AVObject>() {
            @Override
            public void done(AVObject object, AVException e) {
                if(e==null&&object!=null) {
                    object.put("padstate", false);
                    AVSaveOption option = new AVSaveOption();
                    option.query(new AVQuery<>(RequesUtils.CONNECT_STATE).whereEqualTo("user", usernama));
                    option.setFetchWhenSave(true);
                    object.saveInBackground(option, new SaveCallback() {
                        @Override
                        public void done(AVException e) {
                            if (e == null) {

                            } else {

                            }
                        }
                    });
                }
            }
        });
    }
    /**
     * @param usernama
     */
    private void openService(final String usernama) {
        AVQuery<AVObject> query = new AVQuery<>(RequesUtils.CONNECT_STATE);
        query.whereEqualTo("user",usernama);
        query.getFirstInBackground(new GetCallback<AVObject>() {
            @Override
            public void done(AVObject object, AVException e) {
                if(e==null&&object!=null) {
                    object.put("padstate", true);
                    AVSaveOption option = new AVSaveOption();
                    option.query(new AVQuery<>(RequesUtils.CONNECT_STATE).whereEqualTo("user", usernama));
                    option.setFetchWhenSave(true);
                    object.saveInBackground(option, new SaveCallback() {
                        @Override
                        public void done(AVException e) {
                            if (e == null) {

                            } else {

                            }
                        }
                    });
                }
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.wakeLock.release();
        //2016.09.19 : 释放唤醒锁 ;
              releaseWakeLock();
        if(batteryLevelReceiver!=null) {
            if(isregistReceiver) {
                isregistReceiver = false;
                unregisterReceiver(batteryLevelReceiver);
            }
        }
    }


    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock;

    //2016.09.19 : 申请获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行 ;
    private void acquireWakeLock() {
        /**
         * WakeLock如果是一个没有超时的锁 , 而且这个锁没有释放 , 那么系统就无法进入休眠 ; WakeLock这个锁机制 可以被用户态程序和内核获得 ;
         * 这个锁可以是超时的或者没有超时的 , 超时的锁会在超时以后自动解锁. 如果没有了锁 或者超时了 , 内核就会启动休眠的那套机制来进入休眠 ;
         */
        //点亮

        // 获取PowerManager的实例
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "bright");
        //        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        //        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        Log.w("test", TAG + " #acquireWakeLock()");
        //        wakeLock.acquire();     //获取唤醒锁
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

    // 释放设备电源锁          //2016.09.19 : 释放唤醒锁 ;
    private void releaseWakeLock() {
       if (null != mWakeLock && mWakeLock.isHeld()) {

            mWakeLock.release();
            mWakeLock = null;

        }
        if (null != mKeyguardLock) {
            mKeyguardLock.reenableKeyguard();
            mKeyguardLock = null;
        }

    }





    public boolean isWorked(Context mContext, String className) {
        ActivityManager myManager = (ActivityManager) mContext.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(30);

        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString().equals(className))
                return true;
        }
        return false;
    }



}
