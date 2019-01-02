package com.yeespec.microscope.master.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.yeespec.R;
import com.yeespec.libuvccamera.bluetooth.ChatActivity;
import com.yeespec.libuvccamera.bluetooth.DeviceActivity;
import com.yeespec.libuvccamera.uvccamera.service.CameraServerHandler;
import com.yeespec.microscope.master.BaseActivity;
import com.yeespec.microscope.master.adapter.CameraSelectAdapter;
import com.yeespec.microscope.master.adapter.Contrack_Set_Adapter;
import com.yeespec.microscope.master.adapter.RecolorAdapter;
import com.yeespec.microscope.master.adapter.WifiSelectAdapter;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.master.service.MasterIntentService;
//import com.yeespec.microscope.master.service.power.PowerService;
import com.yeespec.microscope.master.service.server.websocket.CustomWebSocketServer;
import com.yeespec.microscope.master.service.server.websocket.api.channel.ClientNumberChannel;
import com.yeespec.microscope.master.service.system.OnErrorListener;
import com.yeespec.microscope.master.service.system.ShutdownThread;
import com.yeespec.microscope.master.service.system.Utils;
import com.yeespec.microscope.master.service.system.disk.ExternalHDD;
import com.yeespec.microscope.master.service.system.disk.ExternalSD;
import com.yeespec.microscope.utils.AndroidUtil;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.DateTimePickDialogUtil;
import com.yeespec.microscope.utils.FileUtils;
import com.yeespec.microscope.utils.SPHelper;
import com.yeespec.microscope.utils.SPUtils;
import com.yeespec.microscope.utils.SettingUtils;
import com.yeespec.microscope.utils.UIUtil;
import com.yeespec.microscope.utils.bluetooth.BlueUtil;
import com.yeespec.microscope.utils.bluetooth.DataUtil;
import com.yeespec.microscope.utils.detector.Maths;
import com.yeespec.microscope.utils.log.Logger;
import com.yeespec.microscope.utils.sql.ConfigurationParameter;
import com.yeespec.microscope.utils.sql.MSQLUtil;
import com.yeespec.microscope.utils.wifi.WifiAdmin;
import com.yeespec.microscope.utils.wifi.WifiConnect;
import com.yeespec.microscope.utils.wifi.WifiConnector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.yeespec.microscope.utils.ConstantUtil.ConvergenceNumber;


/**
 * 更多选项
 */
public class SettingActivity extends BaseActivity implements OnErrorListener, DialogInterface.OnKeyListener {

    /**
     * 操作
     */
    //时间textview
    private TextView tvdate;
    private TextView tvTime;
    // 关机按钮
    private View turnOffBtn;
    // 着色按钮
    private View recolorBtn;
    // 连接相机按钮
    private View connectCameraBtn;
    // 连接WIFI按钮
    private View connectWIFIBtn;
    // 设置自动对焦按键
    private View connectAutoFocusBtn;
    // 设置蓝牙连接
    private View connectBlueBtn;
    // 开发员设置
    private View developerBtn;
    // 自动更新时间按钮
    private View autoUpdateTimeBtn;
    // 更新APP按钮
    private View updateAppBtn;
    // 关于我们
    private View aboutBtn;
    //自动拍照设置
    private View autoCapture;
    //时间设置按键
    private View timebtn;
    private View contractview;
    /**
     * 状态
     */
    // 当前IP
    private TextView ipView;
    // 当前MAC
    private TextView macView;
    // 当前连接WIFI
    private TextView wifiStatusView;
    private TextView wifiNameView;
    // 当前连接的相机
    private TextView cameraStatusView;
    private TextView cameraNameView;
    // 当前连接的手机数量
    private TextView phoneNumberView;
    // 当前时间
    private TextView timeView;
    // 当前版本
    private TextView versionView;
    /**
     * 电量指示
     */
    private SeekBar electricitySeekBar;
    private TextView electricityView;
    /**
     * 内存指示
     */
    private SeekBar storageSeekBar;
    private TextView storageView;

    private Activity activity;
    //色等选择
    private Set<String> checkboxs = new HashSet<>();
    private CheckBox mCheckBox1, mCheckBox2, mCheckBox3, mCheckBox4, mIsAutofocusCheck;
    private CheckBox mIsSynthetic, isStartContract;
    public static final ScheduledExecutorService DELAY_SCHEDULED = Executors.newScheduledThreadPool(1);
    private LinearLayout powerDisplay;
    private LinearLayout wifiDisplay;
    private LinearLayout captureDisplay;
    private LinearLayout connectPhone;
    private CameraSelectAdapter adapter;
    private  WifiSelectAdapter wfadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG = SettingActivity.class.getSimpleName();
        setContentView(R.layout.activity_setting);
        super.onCreate(savedInstanceState);
        activity = this;
        Util.SUPRESS_DEBUG_EXCEPTIONS = true;


        /**
         *初始化Activity的时候 , 将activity添加到设置有Thread.UncaughtExceptionHandler的Application中 ,
         * 会在程序出错崩溃的时候 , 不断重启应用 ; 可以在Thread.UncaughtExceptionHandler中释放资源 ,
         * 可以解决崩溃重启 ;
         */


        //  BaseApplication.setContext(this);

        initView();
        initListener();
        initData();
        setDate();//日期设置
        setTime();
//        Intent start = new Intent(this, PowerService.class);
//        startService(start);

      /*  // TODO: 2016/6/16 : 添加bindService
        //在Splash页面启动时 , 启动系统的主服务 : MasterIntentService
        Intent bindIntent = new Intent(getApplicationContext(), MasterIntentService.class);
        //记得设置Intent的Action , 用于在MasterIntentService端进行判断 :
        bindIntent.setAction(SettingActivity.class.getName());
        //    bindService(bindIntent, conn, Service.BIND_AUTO_CREATE);
        startService(bindIntent);*/

    }

    @Override
    protected void onStart() {
        super.onStart();


        Intent bindIntent = new Intent(getApplicationContext(), MasterIntentService.class);

        bindIntent.setAction(SettingActivity.class.getName());
        bindService(bindIntent, conn, Service.BIND_AUTO_CREATE);

    }

    //初始化checkBox
    private LinearLayout bluell, greenll, purplell, writell;
    private TextView isSyntheticTv;

    private void initCheckBox(Dialog view) {
        mCheckBox1 = ((CheckBox) view.findViewById(R.id.mCheckBox1));
        mCheckBox2 = ((CheckBox) view.findViewById(R.id.mCheckBox2));
        mCheckBox3 = ((CheckBox) view.findViewById(R.id.mCheckBox3));
        mCheckBox4 = ((CheckBox) view.findViewById(R.id.mCheckBox4));
        writell = ((LinearLayout) view.findViewById(R.id.writell));
        bluell = ((LinearLayout) view.findViewById(R.id.bulell));
        greenll = ((LinearLayout) view.findViewById(R.id.greenll));
        purplell = ((LinearLayout) view.findViewById(R.id.purplell));
        isSyntheticTv = ((TextView) view.findViewById(R.id.issynthetic_tv));
        writell.setVisibility(View.GONE);
        bluell.setVisibility(View.GONE);
        greenll.setVisibility(View.GONE);
        purplell.setVisibility(View.GONE);
        if (ConstantUtil.LIGHTSCOUNT == 1)
            isSyntheticTv.setVisibility(View.GONE);

        String[] strings = ConstantUtil.LIGHTTYPE.split(",");
        if (strings.length > 0) {

            for (int i = 0; i < strings.length; i++) {
                int integer = Integer.valueOf(strings[i]);
                switch (integer) {
                    case 1:
                        writell.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        greenll.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        bluell.setVisibility(View.VISIBLE);
                        break;
                    case 4:
                        purplell.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }

        mCheckBox1.setChecked(false);
        mCheckBox2.setChecked(false);
        mCheckBox3.setChecked(false);
        mCheckBox4.setChecked(false);
        mCheckBox1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    addSharePreferentCheckbox("1");
                } else {
                    removidSharePreferentCheckbox("1");
                }
                String timerString = stopTimerString();
                if (timerString.contains("!")) {
                    ultimatelyDateTimer.setTextColor(Color.rgb(255, 32, 32));
                } else {
                    ultimatelyDateTimer.setTextColor(Color.rgb(75, 77, 77));
                }
                ultimatelyDateTimer.setText(timerString);
            }
        });
        mCheckBox2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    addSharePreferentCheckbox("2");
                } else {
                    removidSharePreferentCheckbox("2");
                }
                String timerString = stopTimerString();
                if (timerString.contains("!")) {
                    ultimatelyDateTimer.setTextColor(Color.rgb(255, 32, 32));
                } else {
                    ultimatelyDateTimer.setTextColor(Color.rgb(75, 77, 77));
                }
                ultimatelyDateTimer.setText(timerString);
            }
        });
        mCheckBox3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    addSharePreferentCheckbox("3");
                } else {
                    removidSharePreferentCheckbox("3");
                }
                String timerString = stopTimerString();
                if (timerString.contains("!")) {
                    ultimatelyDateTimer.setTextColor(Color.rgb(255, 32, 32));
                } else {
                    ultimatelyDateTimer.setTextColor(Color.rgb(75, 77, 77));
                }
                ultimatelyDateTimer.setText(timerString);
            }
        });
        mCheckBox4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    addSharePreferentCheckbox("4");
                } else {
                    removidSharePreferentCheckbox("4");
                }
                String timerString = stopTimerString();
                if (timerString.contains("!")) {
                    ultimatelyDateTimer.setTextColor(Color.rgb(255, 32, 32));
                } else {
                    ultimatelyDateTimer.setTextColor(Color.rgb(75, 77, 77));
                }
                ultimatelyDateTimer.setText(timerString);
            }
        });
        Set<String> checkboxs2 = getSharePreferentsCheckbox();
        if (checkboxs2 == null) {
            return;
        }

        checkboxs.clear();
        checkboxs.addAll(checkboxs2);

        Iterator iterator = checkboxs.iterator();
        while (iterator.hasNext()) {
            String s = (String) iterator.next();
            if (s.equals("1")) {
                mCheckBox1.setChecked(true);
            } else if (s.equals("2")) {
                mCheckBox2.setChecked(true);
            } else if (s.equals("3")) {
                mCheckBox3.setChecked(true);
            } else if (s.equals("4")) {
                mCheckBox4.setChecked(true);
            }
        }

    }

    //添加色灯
    private void addSharePreferentCheckbox(String str) {
        checkboxs.add(str);
       // ConstantUtil.autoPhotoChecks = checkboxs;
       SharedPreferences preferences = getApplicationContext().getSharedPreferences(ConstantUtil.CHECKBOXSTATE, 0);
       SharedPreferences.Editor edit = preferences.edit();
       edit.putStringSet(ConstantUtil.CHECKBOXS, checkboxs);
       edit.commit();
    }

    //删除色灯
    private void removidSharePreferentCheckbox(String str) {
        checkboxs.remove(str);
        //ConstantUtil.autoPhotoChecks = checkboxs;
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(ConstantUtil.CHECKBOXSTATE, 0);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putStringSet(ConstantUtil.CHECKBOXS, checkboxs);
        edit.commit();
    }

    //获取色灯
    private Set<String> getSharePreferentsCheckbox() {
        Set<String> strings = new HashSet<>();
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(ConstantUtil.CHECKBOXSTATE, 0);
        strings = preferences.getStringSet(ConstantUtil.CHECKBOXS, null);
        return strings;
    }

    //2016.06.16 新增 : 用于连接绑定的mMasterIntentServiceBinder服务 :
    private MasterIntentService.SettingActivityBinder mSettingActivityBinder;
    //定义一个ServiceConnection对象 :
    private ServiceConnection conn = new ServiceConnection() {
        //当Activity与service连接成功时 , 回调该方法 :
        @Override
        public void onServiceConnected(ComponentName name, final IBinder service) {
            //客户端：通过绑定指定的服务来获取服务端的Binder对象，然后调用IBinder接口类中的transact方法进行 远程调用。 远程通信 .

            if (service != null) {
                //不同进程间 , 不能直接通信 ;
                mSettingActivityBinder = (MasterIntentService.SettingActivityBinder) service;
                //mSettingActivityBinder.setActivity(activity);
            }

        }

        //当Activity与service断开连接时 , 回调该该方法 :
        @Override
        public void onServiceDisconnected(ComponentName name) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UIUtil.toast(activity, "远端服务已断开", false);
                }
            });
        }
    };

    /**
     * 日期
     */
    private void setDate() {
        long now = System.currentTimeMillis();

        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        String data2 = sdf.format(date);
        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.DAY_OF_WEEK);
        String weekc = null;
        ;
        if (week - 1 == 0) {
            weekc = "星期天";
        }//1
        if (week - 1 == 1) {
            weekc = "星期一";
        }//2
        if (week - 1 == 2) {
            weekc = "星期二";
        }
        if (week - 1 == 3) {
            weekc = "星期三";
        }
        if (week - 1 == 4) {
            weekc = "星期四";
        }
        if (week - 1 == 5) {
            weekc = "星期五";
        }
        if (week - 1 == 6) {
            weekc = "星期六";
        }
        tvdate.setText(data2 + " " + weekc);
    }

    /**
     * 时间
     */
    private void setTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        tvTime.setText(sdf.format(new Date()));
    }

    private boolean isShowLevel = true;

    @Override
    protected void getBatteryPercentage() {
        //        super.getBatteryPercentage();

        batteryLevelReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                float currentLevel = intent.getFloatExtra(BatteryManager.EXTRA_LEVEL, -1f);
                float scale = intent.getFloatExtra(BatteryManager.EXTRA_SCALE, -1f);
                final int phone_number = intent.getIntExtra("phonenumber", 0);

                if (currentLevel >= 0 && scale > 0) {

                    final int level = (int) (((currentLevel - 9.6f) * 100) / (12.0 - 9.6f));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int mlevel = Math.abs(ConstantUtil.Level-level);
                            if(mlevel>5){
                                ConstantUtil.Level = level;
                            }
                            electricitySeekBar.setProgress(ConstantUtil.Level);
                            electricityView.setText(ConstantUtil.Level + "%");
                            phoneNumberView.setText(phone_number + "");//显示手机连接数
                            //如果电压小于或者等于10%V则关机
                            if (ConstantUtil.Level <= 10 && ConstantUtil.Level > 1) {
                                FileUtils.writeFileToLogFolder("----------------电量过低自动关机");
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


                    if (ConstantUtil.Level <= 30 && ConstantUtil.Level > 1 && isShowLevel) {
                        isShowLevel = false;
                        DELAY_SCHEDULED.schedule(new Runnable() {
                            @Override
                            public void run() {
                                isShowLevel = true;
                            }
                        }, 180000, TimeUnit.MILLISECONDS);
                        //if(!isOpen)
                        minitElectricityAlert(BaseApplication.getContext(), ConstantUtil.Level);
                    }
                }
            }
        };

        IntentFilter batteryLevelFilter = new IntentFilter(CameraServerHandler.ACTION_STATUS_CHANGED);
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
    }

    private Dialog mElectricdialog;

    protected void minitElectricityAlert(Context context, int electrity) {
        if (mElectricdialog != null) {
            mElectricdialog.dismiss();
            mElectricdialog = null;
        }
        mElectricdialog = new Dialog(context, R.style.Dialog_Radio);
        mElectricdialog.setContentView(R.layout.dialog_electric_toastset);
        mElectricdialog.setCancelable(false);
        mElectricdialog.setCanceledOnTouchOutside(false);
        Button button = (Button) mElectricdialog.findViewById(R.id.electricity_ok_btn);
        TextView electTextView = (TextView) mElectricdialog.findViewById(R.id.toast_elect);
        TextView msgTextView = (TextView) mElectricdialog.findViewById(R.id.toast_msg);

        if (electrity <= 30 && electrity > 10) {
            msgTextView.setText("电量低\n(请充电)");
        } else if (electrity <= 20) {
            //electTextView.setText("电池只剩" + electrity + "%");
            msgTextView.setText("准备关机\n(请充电)");
        }
        electTextView.setText("电池只剩" + electrity + "%");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mElectricdialog.isShowing())
                    mElectricdialog.dismiss();
                isOpen = false;
                mElectricdialog = null;
            }
        });
        mElectricdialog.show();
        isOpen = true;
    }


    private void initView() {
        tvdate = (TextView) findViewById(R.id.tv_date);
        tvTime = (TextView) findViewById(R.id.tv_time);
        turnOffBtn = findViewById(R.id.button_turn_off);
        recolorBtn = findViewById(R.id.recolor_button);
        connectCameraBtn = findViewById(R.id.camera_button);
        connectWIFIBtn = findViewById(R.id.wifi_button);
        connectAutoFocusBtn = findViewById(R.id.autofocus_button);
        connectBlueBtn = findViewById(R.id.blueset_button);
        developerBtn = findViewById(R.id.developerset_button);
        autoUpdateTimeBtn = findViewById(R.id.synchronous_time_button);
        updateAppBtn = findViewById(R.id.app_update_button);
        aboutBtn = findViewById(R.id.about_us_button);
        autoCapture = findViewById(R.id.auto_photo_select);
        contractview = findViewById(R.id.contract_view);
        ipView = (TextView) findViewById(R.id.ip_view);
        macView = (TextView) findViewById(R.id.mac_view);
        wifiNameView = (TextView) findViewById(R.id.wifi_name_view);
        wifiStatusView = (TextView) findViewById(R.id.wifi_status_view);
        cameraNameView = (TextView) findViewById(R.id.camera_name_view);
        cameraStatusView = (TextView) findViewById(R.id.camera_status_view);
        phoneNumberView = (TextView) findViewById(R.id.phone_number_view);
        timeView = (TextView) findViewById(R.id.time_view);
        versionView = (TextView) findViewById(R.id.app_version_view);
        electricityView = (TextView) findViewById(R.id.electricity_text);
        electricitySeekBar = (SeekBar) findViewById(R.id.electricity_seekBar);
        electricitySeekBar.setMax(100);
        electricityView.setText("刷新中...");
        electricitySeekBar.setEnabled(false);

        storageView = (TextView) findViewById(R.id.storage_text);
        storageSeekBar = (SeekBar) findViewById(R.id.storage_seekBar);
        storageSeekBar.setMax(100);
        storageSeekBar.setEnabled(false);
        timebtn = findViewById(R.id.timeset_button);
        if (ConstantUtil.Level == 0) {
            electricitySeekBar.setProgress(ConstantUtil.Level);
            electricityView.setText("刷新中...");
        } else {
            electricitySeekBar.setProgress(ConstantUtil.Level);
            electricityView.setText(ConstantUtil.Level + "%");
        }

        powerDisplay = ((LinearLayout) findViewById(R.id.powerdisplay));
        wifiDisplay = ((LinearLayout) findViewById(R.id.wifidisplay));
        connectPhone = ((LinearLayout) findViewById(R.id.connect_phone));
        captureDisplay = ((LinearLayout) findViewById(R.id.capturedisplay));

    }

    private void initListener() {
        turnOffBtn.setOnClickListener(onClickListener);
        recolorBtn.setOnClickListener(onClickListener);
        autoCapture.setOnClickListener(onClickListener);
        contractview.setOnClickListener(onClickListener);
        connectCameraBtn.setOnClickListener(onClickListener);
        connectWIFIBtn.setOnClickListener(onClickListener);
        developerBtn.setOnClickListener(onClickListener);
        connectAutoFocusBtn.setOnClickListener(onClickListener);
        connectBlueBtn.setOnClickListener(onClickListener);
        autoUpdateTimeBtn.setOnClickListener(onClickListener);
        updateAppBtn.setOnClickListener(onClickListener);
        aboutBtn.setOnClickListener(onClickListener);
        timebtn.setOnClickListener(onClickListener);

        powerDisplay.setOnClickListener(onClickListener);
        wifiDisplay.setOnClickListener(onClickListener);
        captureDisplay.setOnClickListener(onClickListener);
        connectPhone.setOnClickListener(onClickListener);

        findViewById(R.id.btn_return).setOnClickListener(onClickListener);

        //2016.12.13 : 新增 : 长按关机按钮
        turnOffBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Intent intent = new Intent(SettingActivity.this, DeviceListActivity.class);
                startActivity(intent);

                return true;   //返回true , 告诉系统 , 长按操作已经处理 !
            }
        });

    }

    private void initData() {
        // TODO 获取 当前IP
        String ip = SettingUtils.getIPAddress(this);
        if (ip == null) {
            ipView.setText("无法获取IP地址");
        } else {
            ipView.setText(ip);
        }
        // TODO 获取 当前MAC
        String mac = SettingUtils.getLocalMacAddress(this);
        if (mac == null) {
            macView.setText("无法获取MAC地址");
        } else {
            macView.setText(mac);
        }
        // TODO 获取 当前SSID
        String ssid = SettingUtils.getCurrentSSID(this);
        if (ssid == null||!WifiConnect.isWifiEnabled(SettingActivity.this)) {
            wifiStatusView.setText("未连接");
            wifiNameView.setVisibility(View.GONE);
        } else {
            wifiStatusView.setText("已连接");
            wifiNameView.setVisibility(View.VISIBLE);
            wifiNameView.setText(ssid.substring(1,ssid.length()-1));
        }
        // TODO 获取 当前电量（由广播监听实现）

        // TODO 获取 当前内存卡容量（由广播监听实现）
        /** 获取存储卡路径 */
        //        File sdcardDir = Environment.getExternalStorageDirectory();
        ExternalSD externalSD = new ExternalSD(this);
        File sdcardDir = externalSD.getSDCardDir();
        if (sdcardDir != null && sdcardDir.length() <= 0) {
            ExternalHDD externalHDD = new ExternalHDD(this);
           // sdcardDir = externalHDD.getUSBCardPath();
        }

        File path = Environment.getDataDirectory();

        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long totalBlocks = stat.getBlockCount();
        String employ = SettingUtils.convertFileSize(stat.getFreeBlocksLong() * blockSize);
        String allmemory = SettingUtils.convertFileSize(totalBlocks * blockSize);
        if (employ.contains("GB")) {
            employ = employ.substring(0, employ.lastIndexOf(".") + 2) + "G";
        }
        if (allmemory.contains("GB")) {
            allmemory = allmemory.substring(0, allmemory.lastIndexOf(".") + 2) + "G";
        }
        storageSeekBar.setProgress((int) ((totalBlocks - availableBlocks) * 100 / totalBlocks));
        storageView.setText(employ + "可用（共" + allmemory + "）");
        // TODO 获取 当前连接的相机
        UsbDevice usbDevice = getIntent().getParcelableExtra("current_device");
        if (usbDevice == null) {
            cameraStatusView.setText("已连接");
            cameraNameView.setVisibility(View.GONE);
        } else {
            cameraStatusView.setText("已连接");
            cameraNameView.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraNameView.setText(usbDevice.getProductName());
            } else {
                cameraNameView.setText(usbDevice.getDeviceName());
            }
        }
        // TODO 获取 当前连接上中控端的手机数
        if (clientNumberSocket != null) {
            if (clientNumberSocket.tryGet() != null)
                clientNumberSocket.tryGet().close();
            clientNumberSocket = null;
        }

        //2016.09.05 : 调试时注释掉 : 不停输出日志 ; 调试后解开 ;
        clientNumberByWebSocket();


        //2016.11.15 : 添加获取从build.gradle中定义的动态设置值 : 编译时间,编译的机器,最新的commit版本 等:
        String build_time = getString(R.string.build_time);     //输出2016-11-15 17:01
        String build_host = getString(R.string.build_host);     //输出Hasee-PC@YeeSpec-Wen，这是我的电脑的用户名和PC名
        //        getString(R.string.build_revision); // 输出3dd5823, 这是最后一次commit的sha值

        // TODO 获取 当前版本号
        //        versionView.setText(build_time + " " + build_host + " v" + AndroidUtil.getApplicationVersion(this) + "版");
        versionView.setText(build_time + " v" + AndroidUtil.getApplicationVersion(this) + "版");


    }

    private EditText carrier_move_distance, all_distance_show, one_speed_show, petridish_move_distance, plate_move_distance;

    //对焦设置
    public void showAutoFocusSet() {
        String carrier_step, petridish_step, plate_step, move_speed, all_distance;
        final Dialog autofocusSet = new Dialog(activity, R.style.Dialog_Radio);
        autofocusSet.setContentView(R.layout.autofocus_alerlog);
        autofocusSet.setCancelable(false);
        autofocusSet.setCanceledOnTouchOutside(true);

        carrier_move_distance = (EditText) autofocusSet.findViewById(R.id.carrier_move_distance);
        petridish_move_distance = (EditText) autofocusSet.findViewById(R.id.petridish_move_distance);
        plate_move_distance = (EditText) autofocusSet.findViewById(R.id.plate_move_distance);
        all_distance_show = (EditText) autofocusSet.findViewById(R.id.all_move_distance);
        one_speed_show = (EditText) autofocusSet.findViewById(R.id.one_move_speed);
        Button distance_ditermine = (Button) autofocusSet.findViewById(R.id.autofocus_set_determine);
        Button distance_cancel = (Button) autofocusSet.findViewById(R.id.autofocus_set_cancel);


        File mafDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        String autofocusdistance = FileUtils.getFileString(mafDirectory + "", "autofocusdistance.txt");

        if (autofocusdistance != null && !autofocusdistance.equals("")) {
            String[] autofocuss = autofocusdistance.split(",");
            if (autofocuss.length > 4) {
                carrier_step = autofocuss[0];//载玻片往上偏移步数
                petridish_step = autofocuss[1];//培养皿
                plate_step = autofocuss[2];//96孔板
                move_speed = autofocuss[3];//偏移速度
                all_distance = autofocuss[4];//全程位移
            } else {
                carrier_step = 500 + "";
                petridish_step = 500 + "";
                plate_step = 500 + "";
                move_speed = 3 + "";
                all_distance = 8500 + "";
            }
        } else {
            carrier_step = 500 + "";
            petridish_step = 500 + "";
            plate_step = 500 + "";
            move_speed = 3 + "";
            all_distance = 8500 + "";
        }


        carrier_move_distance.setText(carrier_step.trim());
        petridish_move_distance.setText(petridish_step.trim());
        plate_move_distance.setText(plate_step.trim());
        one_speed_show.setText(move_speed.trim());

        all_distance_show.setText(all_distance.trim());
        distance_ditermine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String carrier_str = carrier_move_distance.getText().toString();
                String petridish_str = petridish_move_distance.getText().toString();
                String plate_str = plate_move_distance.getText().toString();
                String one_speed_str = one_speed_show.getText().toString();
                String all_distance_str = all_distance_show.getText().toString();

                boolean carrier_i = isNumeric(carrier_str);
                boolean petridish_i = isNumeric(petridish_str);
                boolean plate_i = isNumeric(plate_str);
                boolean one_speed_i = isNumeric(one_speed_str);
                boolean all_i = isNumeric(all_distance_str);
                if (!carrier_i) {
                    UIUtil.toast(SettingActivity.this, "载玻片偏移步数必须为正整数!", false);
                    return;
                } else if (!petridish_i) {
                    UIUtil.toast(SettingActivity.this, "培养皿偏移步数必须为正整数!", false);
                    return;
                } else if (!plate_i) {
                    UIUtil.toast(SettingActivity.this, "培养板偏移步数必须为正整数!", false);
                    return;
                } else if (!all_i) {
                    UIUtil.toast(SettingActivity.this, "全程位移步数必须为正整数!", false);
                    return;
                } else if (!one_speed_i) {
                    UIUtil.toast(SettingActivity.this, "偏移速度必须为正整数!", false);
                    return;
                }

                String autofocuss = carrier_str + "," + petridish_str + "," + plate_str + "," + one_speed_str + "," + all_distance_str;

                File afDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                FileUtils.saveFileString(afDirectory + "", autofocuss, "autofocusdistance.txt");

                autofocusSet.dismiss();

            }
        });
        distance_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autofocusSet.dismiss();
            }
        });

        autofocusSet.show();

    }

    public  boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    private long[] mHits = new long[3];
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent;
            switch (v.getId()) {
                case R.id.recolor_button:

                    initRecolorSelectDialog();

                    break;
                case R.id.auto_photo_select:
                    //是否正在自动拍照
                    int autoPhoto_views = BaseApplication.getInstance().getAutoPhoto_views();
                    if (autoPhoto_views == 3) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UIUtil.toast(SettingActivity.this, "正在自动拍照,请先停止自动拍照再设置!!", false);

                            }
                        });
                        return;
                    }
                    ConstantUtil.autoPhotoCount = 0;//清除自动拍照总时长
                    ConstantUtil.stopAutoPhotoStr = "";
                    ConstantUtil.processBarAutoPhoto = 0;
                    initAutoCaptureDialog();
                    break;
                case R.id.camera_button:
                    // TODO 弹出摄像头选择对话框
                    List<UsbDevice> usbDevices = getIntent().getParcelableArrayListExtra("device_list");
                    if (usbDevices != null && usbDevices.size() > 0) {
                        initCameraSelectDialog();
                    } else {
                        //UIUtil.toast(SettingActivity.this, "请先连接摄像头", true);
                    }
                    System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                    mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                    if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                        ConstantUtil.isStatScreenScale = !ConstantUtil.isStatScreenScale;
                        if (ConstantUtil.isStatScreenScale) {
                            UIUtil.toast(SettingActivity.this, "已开启屏幕分割线", false);
                        } else {
                            UIUtil.toast(SettingActivity.this, "已开启屏幕分割线", false);
                        }

                    }
                    break;
                case R.id.wifi_button:
                    // TODO 弹出WIFI选择对话框
                    if (SettingUtils.isWifiOpen(SettingActivity.this)) {
                        //initWIFISelectDialog();
                        Intent intentWifi =  new Intent(Settings.ACTION_WIFI_SETTINGS);//WIFI网络
                        intentWifi.putExtra("extra_prefs_show_button_bar", true);
                        intentWifi.putExtra("extra_prefs_set_back_text","返回");
                        intentWifi.putExtra("extra_prefs_set_next_text","");
                        startActivity(intentWifi);
                        // startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //直接进入手机中的wifi网络设置界面

                    } else {
                        UIUtil.toast(SettingActivity.this, "请先打开WIFI", true);
                    }
                    break;
                case R.id.autofocus_button:
                    // dcCategory = 1;
                    // showDCDoialog();`

                    break;
                case R.id.blueset_button:
                    if (ConstantUtil.rockerState.equals("yes")) {
                        Intent intent1 = new Intent(SettingActivity.this, DeviceActivity.class);
                        startActivity(intent1);
                    } else {
                        UIUtil.toast(SettingActivity.this, "正在开发中.......", true);
                    }
                    break;
                case R.id.synchronous_time_button:
                    // TODO 与网络同步时间
                    UIUtil.toast(SettingActivity.this, "正在开发中……", false);
                    break;
                case R.id.app_update_button:
                    // TODO APP更新
                    SettingUtils.upgrade(SettingActivity.this);

                    break;
                case R.id.about_us_button:
                    // TODO APP介绍
                    intent = new Intent(SettingActivity.this, AboutActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case R.id.developerset_button:
                    // dcCategory =2;
                    //  showDCDoialog();
                    break;
                case R.id.button_turn_off:
                    FileUtils.writeFileToLogFolder("----------------用户点击关机");
                    // TODO APP关机
                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                    builder.setMessage(R.string.really_shutdown).setOnKeyListener(SettingActivity.this).setCancelable(true)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    shutdown();
                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //                            forceExit();
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                    break;
                case R.id.btn_return: {
                    intent = new Intent(SettingActivity.this, MasterActivity.class);
                    startActivity(intent);
                    finish();
                }
                break;
                case R.id.timeset_button:
                    if (ConstantUtil.is_recoding) {
                        UIUtil.toast(SettingActivity.this, "正在录制视频不能设置时间", true);
                        return;
                    }
                    showTimerDialog();
                    break;
                case R.id.powerdisplay:
                    mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                    if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                        dcCategory = 1;
                        showDCDoialog();
                    }
                    break;
                case R.id.wifidisplay:
                    System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                    mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                    if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                        dcCategory = 2;
                        showDCDoialog();
                    }
                    break;
                case R.id.capturedisplay:
                    System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                    mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                    if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                        ConstantUtil.isStatScreenScale = !ConstantUtil.isStatScreenScale;
                        if (ConstantUtil.isStatScreenScale) {
                            UIUtil.toast(SettingActivity.this, "已开启屏幕分割线", false);
                        } else {
                            UIUtil.toast(SettingActivity.this, "已关闭屏幕分割线", false);
                        }
                    }
                    break;
                case R.id.connect_phone:
                    System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                    mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                    if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                        dcCategory = 3;
                        showDCDoialog();
                    }
                    break;
                case R.id.contract_view:

                    showAutoPhotoSateXY();
                    break;

            }
            System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        }
    };

    private Contrack_Set_Adapter contrackAdapter;

    private void showAutoPhotoSateXY() {
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_contract);
        dialog.setCanceledOnTouchOutside(true);
        String[] strs = new String[DataUtil.CONTRACKCOUNT];
        for (int i = 0; i < strs.length; i++) {
            strs[i] = "第 " + (i + 1) + " 组  ";
        }
        contrackAdapter = new Contrack_Set_Adapter(getApplicationContext(), strs, DataUtil.CheckNums);
        ListView lv = ((ListView) dialog.findViewById(R.id.listview_contract));
        CheckBox cc = ((CheckBox) dialog.findViewById(R.id.check_contract));
        lv.setAdapter(contrackAdapter);
        if (DataUtil.getContracts()!=null) {
            cc.setChecked(true);
        } else
            cc.setChecked(false);
        cc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    DataUtil.isStartContract = true;
                } else {
                    DataUtil.isStartContract = false;
                }
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                contrackAdapter.setSelectPosition(position);

            }
        });

        Window dialogWindow = dialog.getWindow();

        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.6); // 高度设置为屏幕的0.6
        p.width = (int) (d.getWidth() * 0.4); // 宽度设置为屏幕的0.65
        dialogWindow.setAttributes(p);


        dialog.show();


    }

    private String initDateTime;// 初始化开始时间
    private long mnow, mNextday;
    private  long moneDay = 24 * 60 * 60 * 1000;

    private void showTimerDialog() {
        if (!ConstantUtil.isAutoPhotoFinish) {
            UIUtil.toast(SettingActivity.this, "正在自动拍照，请停止自动拍照设置时间", false);
            return;
        }
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_timeset);
        dialog.setCanceledOnTouchOutside(true);

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.4); // 高度设置为屏幕的0.6
        p.width = (int) (d.getWidth() * 0.4); // 宽度设置为屏幕的0.65
        dialogWindow.setAttributes(p);
        final EditText nowtime = (EditText) dialog.findViewById(R.id.time_et);
        TextView confirm = (TextView) dialog.findViewById(R.id.confirm_tv);
        TextView cancle = (TextView) dialog.findViewById(R.id.cancle_tv);

        mnow = System.currentTimeMillis();
        mNextday = mnow + moneDay;

        Date date = new Date(mnow);

        Date date3 = new Date(mNextday);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        String data2 = sdf.format(date);
        String data4 = sdf.format(date3);
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
        String data1 = sdf1.format(new Date());
        initDateTime = data2 + " " + data1;
        nowtime.setText(initDateTime);
        nowtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateTimePickDialogUtil dateTimePicKDialog = new DateTimePickDialogUtil(activity, initEndDateTime);
                dateTimePicKDialog.dateTimePicKDialog(nowtime);
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timestr = nowtime.getText().toString();
                String years = "";
                String month = "";
                String day = "";
                String hours = "";
                String minutes = "";
                String second = "30";
                String weekstr = "";
                years = timestr.substring(0, 4);
                month = timestr.substring(5, 7);
                day = timestr.substring(8, 10);
                hours = timestr.substring(12, 14);
                minutes = timestr.substring(15, 17);

                String setTime = years + month + day + "." + hours + minutes + second;

                if (!years.equals("")) {
                    int myears = Integer.valueOf(years);
                    if (myears <= 2000) {
                        UIUtil.toast(SettingActivity.this, "时间不能小于2000年", false);
                        return;
                    }
                }
                if (requesRootPermission(getApplicationContext().getPackageCodePath())) {
                    setTimerZ();
                    setTime(setTime);
                }
                Calendar cal = Calendar.getInstance();
                int week = cal.get(Calendar.DAY_OF_WEEK);
                switch (week - 1) {
                    case 0:
                        weekstr = "07";
                        break;
                    case 1:
                        weekstr = "01";
                        break;
                    case 2:
                        weekstr = "02";
                        break;
                    case 3:
                        weekstr = "03";
                        break;
                    case 4:
                        weekstr = "04";
                        break;
                    case 5:
                        weekstr = "05";
                        break;
                    case 6:
                        weekstr = "06";
                        break;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        setDate();
                        setTime();

                    }
                });

                String sendTimetoUartstr = "4A504C59" + "11" + second + minutes + hours + day + month + years.substring(2, 4) + weekstr;
                FileUtils.writeFileToLogFolder("用户重设时间为：20"+years.substring(2, 4)+"年"+month+"月"+day+"日  "+hours+":"+minutes);
                if (mSettingActivityBinder != null)
                    mSettingActivityBinder.setTime(sendTimetoUartstr);
                BaseApplication.getInstance().setAutoPhoto_views(1);
                dialog.dismiss();
            }
        });
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

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
        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // mAlarmManager.setTimeZone("GMT+8:00");
        //mAlarmManager.setTimeZone(String.valueOf(TimeZone.getTimeZone("GMT+08:00")));
        mAlarmManager.setTimeZone("Asia/Shanghai");
    }

    private int dcCategory = 0;//开发员类别 1为对焦设置 2 为物镜和激发块选择设置

    //例如：20170626.121212
    private boolean setTime(String time) {
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

    //开发员密码认证
    private void showDCDoialog() {
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_deleloper);
        dialog.setCanceledOnTouchOutside(true);
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.4); // 高度设置为屏幕的0.6
        p.width = (int) (d.getWidth() * 0.4); // 宽度设置为屏幕的0.65
        dialogWindow.setAttributes(p);
        final EditText password = (EditText) dialog.findViewById(R.id.password);
        TextView confirm = (TextView) dialog.findViewById(R.id.confirm_tv);
        TextView cancle = (TextView) dialog.findViewById(R.id.cancle_tv);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (password.getText().toString().trim().equals("y")) {
                    if (dcCategory == 2) {
                        showDCSetDoialog();
                    } else if (dcCategory == 1) {
                        showAutoFocusSet();
                    } else if (dcCategory == 3) {
                        showGammaDialog();
                    }

                    dialog.dismiss();
                } else {
                    UIUtil.toast(SettingActivity.this, "你输入的密码不正确", true);
                }
            }
        });
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showGammaDialog() {
        final Dialog autofocusSet = new Dialog(activity, R.style.Dialog_Radio);
        autofocusSet.setContentView(R.layout.connectphonenumber);
        autofocusSet.setCancelable(false);
        autofocusSet.setCanceledOnTouchOutside(true);

        Window dialogWindow = autofocusSet.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.4); // 高度设置为屏幕的0.6
        p.width = (int) (d.getWidth() * 0.5); // 宽度设置为屏幕的0.65
        dialogWindow.setAttributes(p);

        TextView confirm = (TextView) autofocusSet.findViewById(R.id.gamma_confirm);
        final TextView cancle = (TextView) autofocusSet.findViewById(R.id.gamma_cancle);
        final EditText inputNumber = (EditText) autofocusSet.findViewById(R.id.gamma_number);
        File filedir1 = Environment.getExternalStorageDirectory().getAbsoluteFile();
        final String gm = FileUtils.getFileString(filedir1 + "", "developerset_gamma.txt");
        if(gm != null && !gm.trim().equals("")){
            ConstantUtil.M_GAMMA  = Integer.valueOf(gm.toString().trim());
            inputNumber.setText(gm.toString().trim());
        }else {
            inputNumber.setText(100+"".trim());
        }
            confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gamma = inputNumber.getText().toString();
                if (gamma == null || gamma.equals("")) {
                    UIUtil.toast(SettingActivity.this, "伽玛值不能为空!", false);
                }
                if (!isNumeric(gamma)) {
                    UIUtil.toast(SettingActivity.this, "伽玛值必须为正整数!", false);
                    return;
                }
                int mGamma = Integer.valueOf(gamma);
                if (mGamma < 0) {
                    mGamma = 0;
                } else if (mGamma > 255) {
                    mGamma = 255;
                }
                final StringBuffer buffer = new StringBuffer("");
                buffer.append("\n");
                buffer.append(mGamma+"");
                File filedir = Environment.getExternalStorageDirectory().getAbsoluteFile();
                FileUtils.saveFileString(filedir + "", buffer.toString().trim(), "developerset_gamma.txt");
                if (mSettingActivityBinder != null) {
                    mSettingActivityBinder.msetGamma(mGamma);
                }
                autofocusSet.dismiss();
            }
        });
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autofocusSet.dismiss();
            }
        });
        autofocusSet.show();

    }



    private void showDCSetDoialog() {
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_deleloper_setdiolog);
        dialog.setCanceledOnTouchOutside(true);

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.5); // 高度设置为屏幕的0.5
        p.width = (int) (d.getWidth() * 0.5); // 宽度设置为屏幕的0.5
        dialogWindow.setAttributes(p);

        final EditText lights = (EditText) dialog.findViewById(R.id.light_number);
        TextView confirm = (TextView) dialog.findViewById(R.id.confirm_tv);
        TextView cancle = (TextView) dialog.findViewById(R.id.cancle_tv);

        final CheckBox fiveTimes = (CheckBox) dialog.findViewById(R.id.five_times);
        final CheckBox tenTimes = (CheckBox) dialog.findViewById(R.id.ten_times);
        final CheckBox twentyTimes = (CheckBox) dialog.findViewById(R.id.twenty_times);
        final CheckBox fiftyTimes = (CheckBox) dialog.findViewById(R.id.fifty_times);
        final CheckBox checkRocker = (CheckBox) dialog.findViewById(R.id.checkrocker);

        final EditText tranlateX = ((EditText) dialog.findViewById(R.id.tranlate_x));
        final EditText tranlateY = ((EditText) dialog.findViewById(R.id.tranlate_y));

        final EditText username = ((EditText) dialog.findViewById(R.id.user_name));
        final EditText password = ((EditText) dialog.findViewById(R.id.password));
        // String s = readFileData("developerset.txt");
        File filedir = Environment.getExternalStorageDirectory().getAbsoluteFile();
        String s = FileUtils.getFileString(filedir + "", "developerset.txt");
        if (s != null && !s.trim().equals("")) {
            String[] split = s.split("\n");
            if (split.length > 0)
                lights.setText(split[0]);
            if (split.length > 1) {
                String[] mtimes = split[1].trim().split(",");
                if (mtimes.length > 1)
                    for (int i = 0; i < mtimes.length; i++) {

                        int times = Integer.valueOf(mtimes[i]);
                        switch (times) {
                            case 5:
                                fiveTimes.setChecked(true);
                                break;
                            case 10:
                                tenTimes.setChecked(true);
                                break;
                            case 20:
                                twentyTimes.setChecked(true);
                                break;
                            case 50:
                                fiftyTimes.setChecked(true);
                                break;
                        }
                    }
            }
            //判断是否已经增加X Y校准设置
            if (split.length > 2) {
                String[] tranlates = split[2].trim().split(",");
                tranlateX.setText(tranlates[0].toString().trim());
                tranlateY.setText(tranlates[1].toString().trim());
            } else {
                tranlateX.setText(ConstantUtil.tranlateX + "");
                tranlateY.setText(ConstantUtil.tranlateY + "");

            }
            //判断是否增加摇杆选项
            if (split.length > 3) {
                String rockerStr = split[3].trim();
                if (rockerStr.equals("yes")) {
                    checkRocker.setChecked(true);
                } else {
                    checkRocker.setChecked(false);
                }
            } else {
                checkRocker.setChecked(false);
            }
            //增加用户名和密码
            if (split.length > 4) {
                String uandp = split[4].trim();
                String muser = uandp.split("&")[0];
                String mpassword = uandp.split("&")[1];
                username.setText(muser);
                password.setText(mpassword);

            } else {
                username.setText("");
                password.setText("");
            }

        } else {
            lights.setText("1,2,3,4");
            fiveTimes.setChecked(true);
            tenTimes.setChecked(false);
            twentyTimes.setChecked(true);
            fiftyTimes.setChecked(false);
            tranlateX.setText(ConstantUtil.tranlateX + "");
            tranlateY.setText(ConstantUtil.tranlateY + "");
            checkRocker.setChecked(false);
        }

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checknumber = 0;
                String s = lights.getText().toString();
               /* if(!isNumeric(s.trim())){
                    UIUtil.toast(SettingActivity.this, "输入的灯数必须为整数", true);
                    return;
                }*/
                if (s.equals("")) {
                    UIUtil.toast(SettingActivity.this, "输入的灯数不能为空", true);
                    return;
                }

                final StringBuffer buffer = new StringBuffer("");
                buffer.append(s.trim());
                buffer.append("\n");
                if (fiveTimes.isChecked()) {
                    buffer.append("5,");
                    checknumber += 1;
                }
                if (tenTimes.isChecked()) {
                    buffer.append("10,");
                    checknumber += 1;
                }
                if (twentyTimes.isChecked()) {
                    buffer.append("20,");
                    checknumber += 1;
                }
                if (fiftyTimes.isChecked()) {
                    buffer.append("50");
                    checknumber += 1;
                }
                buffer.append("\n");

                if (tranlateX.getText() != null) {
                    buffer.append(tranlateX.getText() + ",");
                    buffer.append(tranlateY.getText() + "");
                }
                buffer.append("\n");
                if (checkRocker.isChecked()) {
                    buffer.append("yes");
                } else {
                    buffer.append("no");
                }
                buffer.append("\n");
                if (checknumber > 3 || checknumber < 2) {
                    UIUtil.toast(SettingActivity.this, "物镜选择必须是2到3之间！", true);
                    return;
                }
                if (username.getText().toString().equals("")||password.getText().toString().equals("")) {
                    UIUtil.toast(SettingActivity.this, "您暂无设置远程登录用户", true);
                    File filedir = Environment.getExternalStorageDirectory().getAbsoluteFile();
                    FileUtils.saveFileString(filedir + "", buffer.toString().trim(), "developerset.txt");
                    dialog.dismiss();
                    return;
                }

                AVUser user = new AVUser();
                //String[] usernames = username.getText().toString().split("#");

                user.logInInBackground(username.getText().toString(), password.getText()
                        .toString(), new LogInCallback<AVUser>() {
                    @Override
                    public void done(AVUser avUser, AVException e) {

                        if (e == null) {

                            String uandp = username.getText()+"&"+password.getText();
                            buffer.append(uandp);
                            buffer.append("\n");
                            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                File filedir = Environment.getExternalStorageDirectory().getAbsoluteFile();

                                FileUtils.saveFileString(filedir + "", buffer.toString().trim(), "developerset.txt");
                            }

                            dialog.dismiss();
                        } else {
                            UIUtil.toast(SettingActivity.this, "登录错误，请确认是否存在该用户或网络链接！", true);
                            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                File filedir = Environment.getExternalStorageDirectory().getAbsoluteFile();

                                FileUtils.saveFileString(filedir + "", buffer.toString().trim(), "developerset.txt");
                            }

                            dialog.dismiss();
                            return;
                        }
                    }
                });







            }
        });
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    // 弹出摄像头选择对话框,这没有做，摄像头切换
    public void initCameraSelectDialog() {
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_camera_select);
        dialog.setCanceledOnTouchOutside(true);
        TextView labelView = (TextView) dialog.findViewById(R.id.label_title);
        labelView.setText("选择摄像头");
        ListView listView = (ListView) dialog.findViewById(R.id.list_content);
        final List<UsbDevice> cameras = new ArrayList<>();
        List<UsbDevice> usbDevices = getIntent().getParcelableArrayListExtra("device_list");
        listView.setDivider(null);
        listView.setDividerHeight(0);
        cameras.addAll(usbDevices != null ? usbDevices : (usbDevices = new ArrayList<UsbDevice>()));
        adapter = new CameraSelectAdapter(this, cameras);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UsbDevice usbDevice = getIntent().getParcelableExtra("current_device");
                if (!usbDevice.getDeviceName().equals(cameras.get(position).getDeviceName())) {
                    //                    openUVCCamera(position, ((BaseApplication) getApplication()).mCameraListener, enableViewListener);
                    if (mSettingActivityBinder != null) {
                        mSettingActivityBinder.changeUVCCamera(cameras.get(position));
                        UIUtil.toast(SettingActivity.this, "切换摄像头成功", false);
                        dialog.dismiss();
                    }
                } else {
                    UIUtil.toast(SettingActivity.this, "已为当前摄像头，不需要切换", false);
                }
            }
        });
        dialog.show();
    }

    // 弹出WIFI选择对话框
    public void initWIFISelectDialog() {
        final Dialog sdialog = new Dialog(this, R.style.Dialog_Radio);
        sdialog.setContentView(R.layout.dialog_device_select);
        sdialog.setCanceledOnTouchOutside(true);
        TextView labelView = (TextView) sdialog.findViewById(R.id.label_title);
        labelView.setText("选择WIFI");
        WifiAdmin wifiAdmin = new WifiAdmin(SettingActivity.this);
        wifiAdmin.startScan();
        final List<ScanResult> wifiScans = wifiAdmin.getWifiList();
        ListView listView = (ListView) sdialog.findViewById(R.id.list_content);
        wfadapter = new WifiSelectAdapter(SettingActivity.this, wifiScans);
        listView.setAdapter(adapter);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String ssid = SettingUtils.getCurrentSSID(SettingActivity.this);
                // Log.w("WifiConnect", "ssid=" + ssid + "  wifiScans.get(position).SSID=" +  wifiScans.get(position).SSID);
                if (ssid != null && wifiScans.get(position).SSID.equals(ssid.substring(1,ssid.length()-1))&&WifiConnect.isWifiEnabled(SettingActivity.this)) {
                    UIUtil.toast(SettingActivity.this, "当前WIFI已连接", true);
                } else {
                    // UIUtil.toast(SettingActivity.this, "正在链接Wifi", true);
                    initWifiConfirmDialog(wifiScans.get(position));
                    sdialog.dismiss();
                }
            }
        });
        sdialog.show();
    }

    private RecolorAdapter recolorAdapter;

    private EditText ultimatelyDateTimer;
    private EditText startDateTime, photoNumber, finishTime,convergenceNumber;
    private Button btnConfirm;
    private String initStartDateTime;// 初始化开始时间
    private String initEndDateTime; //="2016年03月20日 16:25"; // 初始化结束时间
    private String initFinshtTime;

    private String mTime2, mTime3, mTime4;

    private Context context;
    private long now, Nextday;
    private  long oneDay = 24 * 60 * 60 * 1000;

    /**
     * 两个时间之间的秒数
     *
     * @param date1
     * @param date2
     * @return
     */
    public  long getSecondes(String date1, String date2) {
        if (date1 == null || date1.equals(""))
            return 0;
        if (date2 == null || date2.equals(""))
            return 0;

        DateFormat myFormatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        Date date = null;
        Date mydate = null;
        try {
            date = myFormatter.parse(date1);
            mydate = myFormatter.parse(date2);
        } catch (Exception e) {
        }
        long secondes = (date.getTime() - mydate.getTime()) / 1000;
        return secondes;
    }

    //自动拍照停止时间
    private String stopTimerString() {
        String startTimer = startDateTime.getText().toString().trim();
        String stopTimer = finishTime.getText().toString().trim();
        String timerNumber = photoNumber.getText().toString().trim();
        Set<String> Checkboxs = getSharePreferentsCheckbox();

        if (Checkboxs == null || Checkboxs.size() < 1 || startTimer == null || stopTimer == null || timerNumber == null || timerNumber.equals("")) {
            return "正在计算中。。。";
        }
        int checks = 0;
        //如果开启对照组
        if (DataUtil.getContracts()!=null) {
            int[][] contracts = DataUtil.getContracts();
            if (ConstantUtil.isSynthetic) {
                checks = contracts.length * Checkboxs.size() + contracts.length;
            } else {
                checks = contracts.length * Checkboxs.size();
            }
        } else {
            if (ConstantUtil.isSynthetic) {
                checks = Checkboxs.size() + 1;
            } else {
                checks = Checkboxs.size();
            }
        }
        String timerString = stopTimer;
        int onePhotoSize = (1024 * 4 + 60) * checks;//一轮之后使用空间：kb
        int timerSpacing;
        if (timerNumber.equals("") || timerNumber == null) {
            timerSpacing = 0;
        } else {
            timerSpacing = Integer.valueOf(timerNumber);

        }


        //可用内存大小 /一轮使用空间 * 时间间隔= 可用时间段(分
        //如果（结束时间-开始时间）<可用时间段   停止时间 = 结束时间
        //如果 （结束时间-开始时间）> 可用时间段  停止时间 = 开始时间 +可用时间段
        ExternalSD externalSD = new ExternalSD(this);
        File sdcardDir = externalSD.getSDCardDir();
        if (sdcardDir != null && sdcardDir.length() <= 0) {
            ExternalHDD externalHDD = new ExternalHDD(this);
            sdcardDir = externalHDD.getUSBCardPath();
        }

        File path = Environment.getDataDirectory();

        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long emportmemory = blockSize * stat.getFreeBlocksLong();

        int availableTimer = (int) (((emportmemory / 1024 - 500 * 1024) / onePhotoSize) * timerSpacing);
        long secondes = getSecondes(stopTimer, startTimer);

        if (secondes - availableTimer * 60 < 0) {
            if (timerSpacing != 0)
                ConstantUtil.autoPhotoCount = (int) ((secondes / timerSpacing / 60 + 1) * checks);
            ConstantUtil.stopAutoPhotoStr = "停止时间：" + stopTimer;
            SPHelper.getInstance().putString("stoptimershow",ConstantUtil.stopAutoPhotoStr);
            return stopTimer;
        } else {
            DateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
            try {
                Date startt = format.parse(startTimer);
                long seconde = startt.getTime() / 1000 + availableTimer * 60;

                Date date = new Date(seconde * 1000);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                String stoptimer = dateFormat.format(date);

                long secondes2 = getSecondes(stoptimer, startTimer);
                if (timerSpacing != 0)
                    ConstantUtil.autoPhotoCount = (int) ((secondes2 / timerSpacing / 60 + 1) * checks);

                ConstantUtil.stopAutoPhotoStr = "停止时间：" + stoptimer;
                SPHelper.getInstance().putString("stoptimershow",ConstantUtil.stopAutoPhotoStr);
                //Log.e("SettingActivity","stoptimer="+stoptimer);
                return stoptimer + "  !!!";
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return timerString;
    }

    //监听Editext内容变化方法
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
            // Log.d("TAG","afterTextChanged--------------->");
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub
            Log.d("TAG", "beforeTextChanged--------------->");
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            Log.d("TAG", "onTextChanged--------------->");
            String timerString = stopTimerString();
            if (timerString.contains("!")) {
                ultimatelyDateTimer.setTextColor(Color.rgb(255, 32, 32));
            } else {
                ultimatelyDateTimer.setTextColor(Color.rgb(75, 77, 77));
            }
            ultimatelyDateTimer.setText(timerString);

        }
    };

    //2016.09.27 : 新增 : 修改定时拍照设置的方式 :
    // 弹出定时拍照设置对话框 : auto_capture :
    public void initAutoCaptureDialog() {
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_auto_photo_select);
        dialog.setCanceledOnTouchOutside(true);
        TextView labelView = (TextView) dialog.findViewById(R.id.label_title);
        labelView.setText("定时拍照设置");

        //获取当时时间
        now = System.currentTimeMillis();
        Nextday = now + oneDay;

        Date date = new Date(now);

        Date date3 = new Date(Nextday);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        String data2 = sdf.format(date);
        String data4 = sdf.format(date3);
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
        String data1 = sdf1.format(new Date());
        initStartDateTime = data2 + " " + data1;
        initEndDateTime = data2 + " " + data1;
        initFinshtTime = data4 + " " + data1;

        ultimatelyDateTimer = (EditText) dialog.findViewById(R.id.inputDate);
        startDateTime = (EditText) dialog.findViewById(R.id.inputDate2);
        finishTime = (EditText) dialog.findViewById(R.id.inputDate3);
        photoNumber = (EditText) dialog.findViewById(R.id.photo_number);
        convergenceNumber = (EditText) dialog.findViewById(R.id.convergence);
        btnConfirm = (Button) dialog.findViewById(R.id.confirm_button);
        //        btnReturn = (Button) dialog.findViewById(R.id.btn_return);

        // 两个输入框
        ultimatelyDateTimer.setText(initStartDateTime);
        startDateTime.setText(initEndDateTime);
        finishTime.setText(initFinshtTime);
        photoNumber.setText("5");
        convergenceNumber.setText("50");



        //监听两个输入宽
       /* startDateTime.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                DateTimePickDialogUtil dateTimePicKDialog = new DateTimePickDialogUtil(activity, initEndDateTime);
                dateTimePicKDialog.dateTimePicKDialog(endDateTime);

            }
        });
        startDateTime.addTextChangedListener(textWatcher);*/
        finishTime.addTextChangedListener(textWatcher);
        photoNumber.addTextChangedListener(textWatcher);

        finishTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //                DateTimePickDialogUtil dateTimePicKDialog = new DateTimePickDialogUtil(
                //                        AutoPhotoActivity.this, initEndDateTime);
                DateTimePickDialogUtil dateTimePicKDialog = new DateTimePickDialogUtil(activity, initFinshtTime);
                dateTimePicKDialog.dateTimePicKDialog(finishTime);

            }
        });
        initCheckBox(dialog);
        initCheckedIsAutoFocus(dialog);
        String timerString = stopTimerString();//////

        if (timerString.contains("!")) {
            ultimatelyDateTimer.setTextColor(Color.rgb(255, 32, 32));
        } else {
            ultimatelyDateTimer.setTextColor(Color.rgb(75, 77, 77));
        }
        ultimatelyDateTimer.setText(timerString);
        listenerExitText();//设置输入框不能为0
        initCheckedListener(dialog);


        // TODO: 2016/7/10 : 添加对话框手动设置默认参数配置 :
        Button settingDialog = (Button) dialog.findViewById(R.id.setting_dialog);
        settingDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //        Toast.makeText(getApplicationContext(), "弹出对话框 ! ", Toast.LENGTH_SHORT).show();

                final ArrayAdapter<String> multiplesAdapter;
                ArrayAdapter<String> stimulatedLightsAdapter;
                ArrayAdapter<String> tintingsAdapter;

                final Dialog mSelfCheckDialog;


                final ConfigurationParameter parameter = new ConfigurationParameter();

                // 初始化对话框
                mSelfCheckDialog = new Dialog(context, 0);
                mSelfCheckDialog.setContentView(R.layout.dialog_set_parameter);
                mSelfCheckDialog.setCancelable(true);
                mSelfCheckDialog.setCanceledOnTouchOutside(true);
                Button putButton = (Button) mSelfCheckDialog.findViewById(R.id.put_btn);
                Button queryButton = (Button) mSelfCheckDialog.findViewById(R.id.query_btn);
                final TextView queryTV = (TextView) mSelfCheckDialog.findViewById(R.id.query_result);
                Spinner multipleSp = (Spinner) mSelfCheckDialog.findViewById(R.id.multiple_spinner);
                Spinner stimulatedLightSp = (Spinner) mSelfCheckDialog.findViewById(R.id.stimulatedLight_spinner);
                Spinner tintingSp = (Spinner) mSelfCheckDialog.findViewById(R.id.tinting_spinner);
                final EditText sensitivityET = (EditText) mSelfCheckDialog.findViewById(R.id.sensitivity_et);
                final EditText brightnessET = (EditText) mSelfCheckDialog.findViewById(R.id.brightness_et);

                //将可选内容与ArrayAdapter连接起来
                multiplesAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, multiplesArray);
                stimulatedLightsAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, stimulatedLightsArray);
                tintingsAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, tintingsArray);
                //设置下拉列表的风格
                multiplesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                stimulatedLightsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                tintingsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                //将adapter 添加到spinner中
                multipleSp.setAdapter(multiplesAdapter);
                stimulatedLightSp.setAdapter(stimulatedLightsAdapter);
                tintingSp.setAdapter(tintingsAdapter);

                //==================================================
                multipleSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        parameter.setMultiple(multiplesArray[position]);

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        parameter.setMultiple(multiplesArray[0]);

                    }
                });
                stimulatedLightSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        parameter.setStimulatedLight(Integer.valueOf(stimulatedLightsArray[position]));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        parameter.setStimulatedLight(Integer.valueOf(stimulatedLightsArray[0]));
                    }
                });
                tintingSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        parameter.setTinting(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        parameter.setTinting(0);
                    }
                });

                //==================================================
                putButton.setOnClickListener(new View.OnClickListener() {       //添加 数据到数据库 :
                    @Override
                    public void onClick(View v) {
                        if (sensitivityET.getText().toString().trim().length() != 0) {
                            parameter.setSensitivity(Integer.parseInt(sensitivityET.getText().toString()));
                        } else {
                            parameter.setSensitivity(0);
                        }
                        if (brightnessET.getText().toString().trim().length() != 0) {
                            parameter.setBrightness(Integer.parseInt(brightnessET.getText().toString()));
                        } else {
                            parameter.setBrightness(0);
                        }

                        MSQLUtil.mPut(getApplicationContext(), parameter);


                    }
                });
                queryButton.setOnClickListener(new View.OnClickListener() {     //查询数据库的数据 :
                    @Override
                    public void onClick(View v) {

                        if (sensitivityET.getText().toString().trim().length() != 0) {
                            parameter.setSensitivity(Integer.parseInt(sensitivityET.getText().toString()));
                        } else {
                            parameter.setSensitivity(0);
                        }
                        if (brightnessET.getText().toString().trim().length() != 0) {
                            parameter.setBrightness(Integer.parseInt(brightnessET.getText().toString()));
                        } else {
                            parameter.setBrightness(0);
                        }

                        String stringBuf = "查询结果 : \n ";

                        //                        SQLUtils.get(getApplicationContext(), parameter);     //查询特定条件的数据 , 如果不存在 , 就创建一个 ;

                        ArrayList<ConfigurationParameter> allData = (ArrayList<ConfigurationParameter>) MSQLUtil.find();

                        for (int index = 0; index < allData.size(); index++) {
                            //                    ConfigurationParameter configurationParameter = allData.get(index);
                            stringBuf += (allData.get(index).toString() + "\n");
                        }

                        queryTV.setText(stringBuf);
                    }
                });

                mSelfCheckDialog.show();

            }
        });
        dialog.show();

    }

    private void listenerExitText() {
        photoNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                String text = s.toString();
                int len = s.toString().length();
                if (len == 1 && text.equals("0")) {
                    s.clear();
                }

            }
        });

    }

    private void initCheckedIsAutoFocus(final Dialog dialog) {
        mIsAutofocusCheck = ((CheckBox) dialog.findViewById(R.id.is_autofocus));
        mIsSynthetic = ((CheckBox) dialog.findViewById(R.id.is_synthetic));
        isStartContract = ((CheckBox) dialog.findViewById(R.id.startcontract));
        mIsAutofocusCheck.setChecked(false);
        mIsSynthetic.setChecked(false);
        isStartContract.setChecked(false);
        if (ConstantUtil.LIGHTSCOUNT == 1) {
            mIsSynthetic.setVisibility(View.GONE);
        }
        if (ConstantUtil.isAutofocus) {
            mIsAutofocusCheck.setChecked(true);
        }
        if (ConstantUtil.isSynthetic) {
            mIsSynthetic.setChecked(true);
        }
        if(DataUtil.getContracts()!=null){
            isStartContract.setChecked(true);
        }
        final int[][] contracts = DataUtil.getContracts();
        isStartContract.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    if(contracts==null){
                        UIUtil.toast(SettingActivity.this, "请先设置对照组在启动对照!", false);
                        isStartContract.setChecked(false);
                        return;
                    }
                    DataUtil.isStartContract = true;
                } else {
                    DataUtil.isStartContract = false;
                }
            }
        });
        mIsSynthetic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ConstantUtil.isSynthetic = true;
                } else
                    ConstantUtil.isSynthetic = false;
                String timerString = stopTimerString();
                if (timerString.contains("!")) {
                    ultimatelyDateTimer.setTextColor(Color.rgb(255, 32, 32));
                } else {
                    ultimatelyDateTimer.setTextColor(Color.rgb(75, 77, 77));
                }
                ultimatelyDateTimer.setText(timerString);
            }
        });
        mIsAutofocusCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ConstantUtil.isAutofocus = true;
                } else
                    ConstantUtil.isAutofocus = false;

            }
        });
    }

    private void initCheckedListener(final Dialog dialog) {
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<String> sharePreferentsCheckbox = getSharePreferentsCheckbox();
                //一个对照组增加7秒
                int[][] contracts = DataUtil.getContracts();
                if (sharePreferentsCheckbox == null || sharePreferentsCheckbox.size() < 1) {
                    UIUtil.toast(SettingActivity.this, "请选择自动拍摄颜色!", false);
                    return;
                }
                String s = photoNumber.getText().toString();
                String cNumber = convergenceNumber.getText().toString();
                if (!isNumeric(s)) {
                    UIUtil.toast(SettingActivity.this, "时间间隔必须是整数!", false);
                    return;
                }
                if (!isNumeric(cNumber)) {

                    UIUtil.toast(SettingActivity.this, "细胞汇合率必须是整数!", false);
                    return;
                }
                if (s == null || s.equals("")) {
                    UIUtil.toast(SettingActivity.this, "时间间隔不能为空", false);
                    return;
                }
                if (cNumber == null || cNumber.equals("")) {
                    UIUtil.toast(SettingActivity.this, "细胞汇合率不能为空", false);
                    return;
                }
                int number = Integer.valueOf(s);
                int mCNumber = Integer.valueOf(cNumber);
                if(mCNumber<0||mCNumber>100){
                    UIUtil.toast(SettingActivity.this, "细胞汇合率必须是0-100", false);
                    return;
                }

                if (DataUtil.getContracts()!=null) {
                    double minute = (double) (contracts.length * sharePreferentsCheckbox.size() * 8.0 / 60.0) + 1;
                    int result = (int) Math.ceil(minute);
                    if (number < result) {
                        UIUtil.toast(SettingActivity.this, "自动拍照时间间隔必须大于或等于" + result + "分钟", false);
                        return;
                    }
                } else {
                    if (sharePreferentsCheckbox.size() > 1) {
                        if (number < 2) {
                            UIUtil.toast(SettingActivity.this, "多个激发块自动拍照时间间隔必须大于1分钟", false);
                            return;
                        }
                    }
                }


                mTime2 = startDateTime.getText().toString();
                mTime3 = photoNumber.getText().toString();
                mTime4 = finishTime.getText().toString();
                Date DTime2 = null;
                Date DTime3 = null;
                Date DTime4 = null;
                DateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                try {
                    DTime2 = dateFormat.parse(mTime2);
                    DTime3 = dateFormat.parse(initEndDateTime);
                    DTime4 = dateFormat.parse(mTime4);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Long LTime2 = DTime2.getTime();
                Long LTime3 = DTime3.getTime();
                Long LTime4 = DTime4.getTime();
                if (LTime3 - LTime2 > 0) {
                    UIUtil.toast(SettingActivity.this, "输入时间小于当前时间，请重新输入！", false);
                    // Toast.makeText(getApplicationContext(), "输入时间小于当前时间，请重新输入！", Toast.LENGTH_LONG).show();
                    return;
                } else if (LTime2 - LTime4 >= 0) {
                    UIUtil.toast(SettingActivity.this, "输入时间小于或者等于开始时间，请重新输入！", false);
                    //Toast.makeText(getApplicationContext(), "输入时间小于或者等于开始时间，请重新输入！", Toast.LENGTH_LONG).show();
                    return;
                }
                ConvergenceNumber  = mCNumber+"";
                SPHelper.getInstance().putString("convegence",ConvergenceNumber);
                BaseApplication.getInstance().setAutoPhoto_views(2);
                BaseApplication.getInstance().setAutoPhoto_startTime(mTime2);
                BaseApplication.getInstance().setAutoPhoto_tTime(mTime3);
                BaseApplication.getInstance().setAutoPhoto_finishTime(mTime4);
                dialog.dismiss();

                // UIUtil.toast(SettingActivity.this, "点拍照，进入自动拍照!", false);
                // Toast.makeText(getApplicationContext(), "点拍照，进入自动拍照!!!!!!!!!!!!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(activity, MasterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });


    }

    private void retureRecolor_pos(String colorstr) {

        InputStream is = null;
        try {
            is = SettingActivity.this.getAssets().open("colors.txt");
            int postion = readTextFromSDcard(is, colorstr);
            BaseApplication.getInstance().setRecolor_pos(postion);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

            try {//关闭资源输入输出流 :
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }

        }

    }

    private int readTextFromSDcard(InputStream is, String colorstr) {


        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;
        reader = new InputStreamReader(is);
        bufferedReader = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer("");
        String str;
        int i = 0;
        try {
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
                buffer.append("\n");
                String[] strings = str.split(",");

                if (strings.length > 4) {
                    if (colorstr.equals(strings[4]))
                        break;
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭资源输入输出流 :
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    //关闭资源输入输出流 :
                    if (reader != null)
                        reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return i;
    }

    //2016.09.27 : 新增 : 修改着色选择的方式 :
    // 弹出着色选择对话框 : recolor :
    public void initRecolorSelectDialog() {
        if (!ConstantUtil.isAutoPhotoFinish) {
            UIUtil.toast(SettingActivity.this, "正在自动拍照，请停止自动拍照再着色", false);
            return;
        }
        String currentReColorString = BaseApplication.getInstance().getCurrentReColorString();
        retureRecolor_pos(currentReColorString);//目前染色位置
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_recolor_select);
        dialog.setCanceledOnTouchOutside(true);
        TextView labelView = (TextView) dialog.findViewById(R.id.label_title);
        labelView.setText("灯光着色选择");

        GridView gridView = (GridView) dialog.findViewById(R.id.grid_view);

        if (recolorAdapter == null) {
            recolorAdapter = new RecolorAdapter(getApplicationContext());
        } else if (0 == recolorAdapter.getCount()) {
            recolorAdapter = new RecolorAdapter(getApplicationContext());
        }

        gridView.setAdapter(recolorAdapter);
        recolorAdapter.notifyDataSetChanged();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Object rgb = get(getApplicationContext(), "recolor", MasterIntentService.RECOLOR_WHITE);
                //int recolor = (rgb == null ? MasterIntentService.RECOLOR_WHITE : (int) rgb);
                int colorSel = recolorAdapter.getColor(position);
                String colorString = recolorAdapter.getColorString(position);

                //Object recolorPos = get(getApplicationContext(), "recolor-pos", 0);
                //int recolorPositon = (recolorPos == null ? 0 : (int) recolorPos);
                int recolorPositon = BaseApplication.getInstance().getRecolor_pos();

                //2016.11.25 : 修改判断条件 :
                if (position == recolorPositon) {
                    UIUtil.toast(getApplicationContext(), "该颜色当前已选，请选择其他颜色", false);
                    return;
                } else {
                    //SPUtils.put(getApplicationContext(), "recolor", colorSel);
                    BaseApplication.getInstance().setRecolor(colorSel);
                    //SPUtils.put(getApplicationContext(), "recolor-pos", position);
                    BaseApplication.getInstance().setRecolor_pos(position);
                    //2016.06.12 新增 : 用于设置激发块灯光 :
                    //TODO: 2016/6/12

                    //2016.07.18 : 新增 : 用于保存用户相同倍数和激发块下的 着色值 :
                    //获取当前选择的物镜倍数 :
                    //Object objMultiple = get(getApplicationContext(), "contrast", 10);
                    //int mul = (objMultiple == null ? 10 : (int) objMultiple);
                    int mul = BaseApplication.getInstance().getContrast();
                    //获取当前选择的激发块 :
                    //Object objSaturation = get(getApplicationContext(), "saturation", 0);
                    //int saturation = (objSaturation == null ? 0 : (int) objSaturation);
                    int saturation = BaseApplication.getInstance().getSaturation();


                    //2016.07.18 新增 : 用sharepreference保存和获取用户的着色设置 :
                    String userRecolorString = "";
                    int defaultRecolor = MasterIntentService.RECOLOR_WHITE;        //默认着色 :

                    String multipleString = MasterIntentService.MULTIPLE_STRING_5X;
                    switch (mul) {
                        //5 x 倍物镜 :
                        case MasterIntentService.MULTIPLE_5X:
                            multipleString = MasterIntentService.MULTIPLE_STRING_5X;
                            break;
                        //10 x 倍物镜 :
                        case MasterIntentService.MULTIPLE_10X:
                            multipleString = MasterIntentService.MULTIPLE_STRING_10X;
                            break;
                        //20 x 倍物镜 :
                        case MasterIntentService.MULTIPLE_20X:
                            multipleString = MasterIntentService.MULTIPLE_STRING_20X;
                            break;
                        //40 x 倍物镜 :
                        case MasterIntentService.MULTIPLE_40X:
                            multipleString = MasterIntentService.MULTIPLE_STRING_40X;
                            break;
                        //50 x 倍物镜 :
                        case MasterIntentService.MULTIPLE_50X:
                            multipleString = MasterIntentService.MULTIPLE_STRING_50X;
                            break;
                    }
                    String saturationString = MasterIntentService.LIGHT_STRING_WHITE;
                    switch (saturation) {
                        //饱和灯光 颜色 : 白色
                        case MasterIntentService.LIGHT_SATURATION_WHITE:
                            saturationString = MasterIntentService.LIGHT_STRING_WHITE;
                            defaultRecolor = MasterIntentService.RECOLOR_WHITE;      //白着色 ;
                            break;
                        //饱和灯光 颜色 : 蓝色
                        case MasterIntentService.LIGHT_SATURATION_BLUE:
                            saturationString = MasterIntentService.LIGHT_STRING_BLUE;
                            defaultRecolor = MasterIntentService.RECOLOR_GREEN;      //绿着色 ;
                            break;
                        //饱和灯光 颜色 : 黑色
                        case MasterIntentService.LIGHT_SATURATION_BLACK:
                            saturationString = MasterIntentService.LIGHT_STRING_BLACK;
                            defaultRecolor = MasterIntentService.RECOLOR_WHITE;      //白着色 ;
                            break;
                        //饱和灯光 颜色 : 绿色
                        case MasterIntentService.LIGHT_SATURATION_GREEN:
                            saturationString = MasterIntentService.LIGHT_STRING_GREEN;
                            defaultRecolor = MasterIntentService.RECOLOR_RED;      //可选红/黄/橙 , 默认红着色 ;
                            break;
                        //2016.06.08 : 新增 :
                        //饱和灯光 颜色 : 紫色
                        case MasterIntentService.LIGHT_SATURATION_PURPLE:
                            saturationString = MasterIntentService.LIGHT_STRING_PURPLE;
                            defaultRecolor = MasterIntentService.RECOLOR_BLUE;      //蓝着色 ;
                            break;
                    }

                    userRecolorString = multipleString + saturationString;

                    //设置recolor着色值 :
                    SPUtils.put(getApplicationContext(), userRecolorString, colorSel);
                    //2016.11.25 : 添加选择的染料名称记录 :
                    // SPUtils.put(getApplicationContext(), "currentReColorString", colorString);
                    BaseApplication.getInstance().setCurrentReColorString(colorString);
                    if (mSettingActivityBinder != null) {
                        mSettingActivityBinder.setRecolorString(colorString);
                    }
                }
                recolorAdapter.notifyDataSetChanged();
                dialog.dismiss();
                if (mSettingActivityBinder != null) {
                    mSettingActivityBinder.setPutConfigurationParameter();
                }
                Intent intent = new Intent(SettingActivity.this, MasterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();

            }
        });

        dialog.show();

    }

    private void shutdown() {

        new ShutdownThread(this).start();
    }

    @Override
    public void onError(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showErrorDialog(msg);
            }
        });
    }

    @Override
    public void onError(Exception exc) {
        final String msg = exc.getClass().getSimpleName() + ": " + exc.getMessage();
        onError(msg);
    }

    @Override
    public void onNotRoot() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showNotRootedDialog();
            }
        });
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        return false;
    }

    private void showErrorDialog(String msg) {
        AlertDialog.Builder builder = buildErrorDialog(msg);
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void showNotRootedDialog() {
        final Uri uri = Uri.parse(getString(R.string.rooting_url));
        AlertDialog.Builder builder = buildErrorDialog(getString(R.string.not_rooted));
        builder.setNegativeButton(R.string.what, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                forceExit();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private AlertDialog.Builder buildErrorDialog(String msg) {
        return new AlertDialog.Builder(this).setMessage(msg).setOnKeyListener(this).setCancelable(true)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        forceExit();
                    }
                });
    }

    private void forceExit() {
        finish();
        Utils.killMyProcess();
    }




    /**
     * wifi弹出的adapter
     */
    /*private  class WifiSelectAdapter extends ArrayAdapter<ScanResult> {

        private LayoutInflater mInflater;

        public WifiSelectAdapter(Context context, List<ScanResult> objects) {
            super(context, -1, objects);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_wifi_content, parent, false);
                viewHolder.wifiNameView = (TextView) convertView.findViewById(R.id.wifi_name_view);
                viewHolder.lockView = (ImageView) convertView.findViewById(R.id.label_lock);
                viewHolder.levelView = (ImageView) convertView.findViewById(R.id.label_level);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            String ssid = SettingUtils.getCurrentSSID(SettingActivity.this);
            if (ssid != null && getItem(position).SSID.equals(ssid)) {
                viewHolder.wifiNameView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else {
                viewHolder.wifiNameView.setTextColor(getResources().getColor(R.color.LIGHTBLACK));
            }
            viewHolder.wifiNameView.setText(getItem(position).SSID);
            if (getItem(position).capabilities.contains("WPA") || getItem(position).capabilities.contains("WEP")) {
                viewHolder.lockView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.lockView.setVisibility(View.GONE);
            }
            int nSigLevel = WifiManager.calculateSignalLevel(getItem(position).level, 100);
            if (nSigLevel > 0 && nSigLevel <= 50) {
                viewHolder.levelView.setImageResource(R.mipmap.wifi_1);
            } else if (nSigLevel > 50 && nSigLevel <= 70) {
                viewHolder.levelView.setImageResource(R.mipmap.wifi_2);
            } else if (nSigLevel > 70 && nSigLevel <= 100) {
                viewHolder.levelView.setImageResource(R.mipmap.wifi_3);
            }
            return convertView;
        }

        private class ViewHolder {
            TextView wifiNameView;
            ImageView lockView;
            ImageView levelView;
        }
    }*/
    /**
     * wifi弹窗的显示
     *
     * @param result
     */
    private WifiConnector connector;

    private void initWifiConfirmDialog(final ScanResult result) {
        FileUtils.writeFileToLogFolder("用户重新设置链接的wifi:"+result.SSID);
        final Dialog dialog = new Dialog(this, R.style.Dialog_Radio);
        dialog.setContentView(R.layout.dialog_wifi_confirm);
        dialog.setCanceledOnTouchOutside(true);
        final TextView ssidlView = (TextView) dialog.findViewById(R.id.ssid_view);
        final TextView title = (TextView) dialog.findViewById(R.id.label_wifi_title);
        ssidlView.setText(result.SSID);

        final EditText passwordView = (EditText) dialog.findViewById(R.id.password_view);
        final Button confirmBtn = (Button) dialog.findViewById(R.id.confirm_button);
        final View content = dialog.findViewById(R.id.content);
        final View loading = dialog.findViewById(R.id.loading);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        content.setVisibility(View.GONE);
                        loading.setVisibility(View.VISIBLE);
                    }
                });
                if (passwordView.getText() != null && passwordView.getText().toString().length() > 0) {


                    WifiManager wifiManager = (WifiManager) SettingActivity.this.getSystemService(Context.WIFI_SERVICE);
                    WifiConnect wifiConnect = new WifiConnect(SettingActivity.this,wifiManager);

                  /*  connector = new WifiConnector(SettingActivity.this, new WifiConnector.WifiConnectListener() {
                        @Override
                        public void OnWifiConnectCompleted(boolean isConnected) {
                            UIUtil.toast(SettingActivity.this, "WIFI连接" + (isConnected ? "成功" : "失败，请检查输入的密码是否正确"), false);

                            if (!isConnected) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        content.setVisibility(View.VISIBLE);
                                        loading.setVisibility(View.GONE);
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dialog.dismiss();
                                    }
                                });
                            }
                        }
                    });*/
                    WifiConnector.SecurityMode connectType = null;
                    //
                    WifiConnect.WifiCipherType  mconnectType = null;
                    if (result.capabilities.contains("WPA2")) {
                        connectType = WifiConnector.SecurityMode.WPA2;
                        // mconnectType = WifiConnect.WifiCipherType.WIFICIPHER_WPA;


                    } else if (result.capabilities.contains("WPA")) {
                        mconnectType = WifiConnect.WifiCipherType.WIFICIPHER_WPA;
                        connectType = WifiConnector.SecurityMode.WPA;

                    } else if (result.capabilities.contains("WEP")) {
                        mconnectType = WifiConnect.WifiCipherType.WIFICIPHER_WEP;
                        connectType = WifiConnector.SecurityMode.WEP;
                    } else if (result.capabilities.contains("OPEN")) {
                        mconnectType = WifiConnect.WifiCipherType.WIFICIPHER_NOPASS;
                        connectType = WifiConnector.SecurityMode.OPEN;
                    }
                    // UIUtil.toast(SettingActivity.this,"type="+mconnectType,true);

                    wifiConnect.mConnect(result.SSID, passwordView.getText().toString(), mconnectType, new WifiConnect.WifiConnectListener() {
                        @Override
                        public void OnWifiConnectCompleted(boolean isConnected) {
                            if(isConnected){

                                UIUtil.toast(SettingActivity.this, "链接Wifi成功！", false);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wifiStatusView.setText("已链接");
                                        wifiNameView.setVisibility(View.VISIBLE);
                                        wifiNameView.setText(result.SSID);
                                        content.setVisibility(View.VISIBLE);
                                        loading.setVisibility(View.GONE);
                                        dialog.dismiss();

                                    }
                                });
                            }else {

                                UIUtil.toast(SettingActivity.this, "链接Wifi失败！", false);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wifiStatusView.setText("未链接");
                                        wifiNameView.setVisibility(View.GONE);
                                        dialog.dismiss();
                                    }
                                });
                            }
                        }
                    });
                    /*if(wifiConnect.Connect(result.SSID, passwordView.getText().toString(),mconnectType))
                    {
                        UIUtil.toast(SettingActivity.this, "链接Wifi成功！", false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                wifiStatusView.setText("已链接");
                                wifiNameView.setVisibility(View.VISIBLE);
                                wifiNameView.setText(result.SSID);
                                content.setVisibility(View.VISIBLE);
                                loading.setVisibility(View.GONE);
                                dialog.dismiss();

                            }
                        });
                    }else {
                        wifiStatusView.setText("未链接");
                        wifiNameView.setVisibility(View.GONE);
                        UIUtil.toast(SettingActivity.this, "链接Wifi失败！", false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                            }
                        });
                    }*/
                    //  connector.connect(result.SSID, passwordView.getText().toString(), connectType);
                } else {
                    UIUtil.toast(SettingActivity.this, "请检查输入的密码是否正确", false);
                }
            }
        });
        dialog.show();
    }


    private com.koushikdutta.async.future.Future<WebSocket> clientNumberSocket;
    private   AsyncHttpClient.WebSocketConnectCallback connectCallback;
    public void clientNumberByWebSocket() {
        connectCallback = new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {

                if (ex != null) {

                    ex.printStackTrace();
                    clientNumberSocket = null;
                    clientNumberByWebSocket();
                    return;
                }

                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        if (DEBUG)
                            Logger.e(TAG, bb.peekString());
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                JSONObject object = JSON.parseObject(s);

                                phoneNumberView.setText(String.valueOf(object.getInteger("count")));
                            }
                        });
                    }
                });

                webSocket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {

                        if (ex != null) {
                            ex.printStackTrace();
                            return;
                        }
                    }
                });

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            ex.printStackTrace();
                            clientNumberSocket = null;
                            clientNumberByWebSocket();
                            return;
                        }
                    }
                });
            }
        };
        clientNumberSocket = AsyncHttpClient.getDefaultInstance().websocket(CustomWebSocketServer.PROTOCOL + CustomWebSocketServer.URI6 + ClientNumberChannel.CHANNEL_NAME, null, connectCallback);
    }

    @Override
    public void onResume() {

//        Intent start = new Intent(this, PowerService.class);
//        startService(start);
        super.onResume();
    }

    @Override
    public void onPause() {

//        Intent start = new Intent(this, PowerService.class);
//        stopService(start);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (connector != null) {
            connector.disconnect();
        }
        checkboxs.clear();
        checkboxs =null;
//        Intent start = new Intent(this, PowerService.class);
//        stopService(start);
        if (clientNumberSocket != null) {
            if (clientNumberSocket.tryGet() != null)
                clientNumberSocket.tryGet().close();
            clientNumberSocket = null;
        }
        if(connectCallback!=null){
            connectCallback = null;
        }
        adapter = null;
        recolorAdapter = null;
        wfadapter =null;
        //        unregisterReceiver(batteryLevelReceiver);

        //2016.09.02 : 解除绑定 的后台主服务 :
        unbindService(conn);
        unregisterReceiver(batteryLevelReceiver);

        super.onDestroy();
    }

    // TODO: 2016/7/10  : 新增 :
    private String[] multiplesArray = {"5 X", "10 X", "20 X", "50 X"};
    private String[] stimulatedLightsArray = {"蓝光", "绿光", "紫光", "白光"};
    private String[] tintingsArray = {"蓝色", "绿色", "红色", "黄色", "橙色", "紫色"};


}
