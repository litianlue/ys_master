package com.yeespec.microscope.master.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.TwoLineListItem;

import com.yeespec.R;
import com.yeespec.libuvccamera.usart.usbserial.driver.UsbSerialDriver;
import com.yeespec.libuvccamera.usart.usbserial.driver.UsbSerialPort;
import com.yeespec.libuvccamera.usart.usbserial.driver.UsbSerialProber;
import com.yeespec.libuvccamera.usart.usbserial.util.HexDump;
import com.yeespec.libuvccamera.usart.usbserial.util.SerialInputOutputManager;
import com.yeespec.microscope.utils.Comm;
import com.yeespec.microscope.utils.Exit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Mr.Wen on 2016/11/30.
 */

public class UsbSerialActivity extends Activity {
    //static final int MEMORY_REQUEST = 1002;
    public static boolean isRunBackground = true;
    private Button btn_recClear = null;
    private Button btn_send = null;
    private Button btn_sendClear = null;
    private StringBuilder builderRec = new StringBuilder();
    private byte[] bytArryRec = null;
   // private int bytArryRecMax = 10240;
    private int bytArryRecPos = 0;
    private CheckBox check_recHex = null;
    private CheckBox check_sendHex = null;
    private EditText edit_recArea = null;
    private EditText edit_sendArea = null;
    private Activity gActivity = null;
    private ImageView img_menu = null;

    private LinearLayout layout_sendArea = null;
    private LinearLayout layout_sendAreaBar = null;

    //private ProgressDialog proDia = null;

    private ToggleButton toggle_auto = null;
    private ToggleButton toggle_recPause = null;
    private ToggleButton toggle_sendMemory = null;
    private TextView txt_recCount = null;
    private TextView txt_title = null;
    private TextView txt_sendAreaBar = null;
    private TextView txt_sendAreaTip = null;
    private TextView txt_sendCount = null;
    private ScrollView view_scrollView = null;

    private boolean recHexFlag = false;
    private boolean sendHexFlag = false;
    private boolean stopRecFlag = false;

    private long totalRecByte = 0;
    private long singleRecByte = 0;

    private void showSendAreaOrBar() {
        if ((layout_sendAreaBar.getVisibility() == View.GONE) && (layout_sendArea.getVisibility() == View.VISIBLE)) {
            layout_sendAreaBar.setVisibility(View.VISIBLE);
            layout_sendArea.setVisibility(View.GONE);
            return;
        }
        layout_sendArea.setVisibility(View.VISIBLE);
        layout_sendAreaBar.setVisibility(View.GONE);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // TODO: 2016/12/1 反编译出错 :
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.layout_main_new);
        //         getWindow().setSoftInputMode(2);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.TYPE_CHANGED);
        gActivity = this;

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        view_scrollView = (ScrollView) findViewById(R.id.view_scrollView);
        txt_title = ((TextView) findViewById(R.id.txt_title));
        txt_recCount = ((TextView) findViewById(R.id.txt_recCount));
        txt_sendCount = ((TextView) findViewById(R.id.txt_sendCount));
        check_recHex = ((CheckBox) findViewById(R.id.check_recHex));
        check_sendHex = ((CheckBox) findViewById(R.id.check_sendHex));
        toggle_recPause = ((ToggleButton) findViewById(R.id.toggle_recPause));
        toggle_sendMemory = ((ToggleButton) findViewById(R.id.toggle_sendMemory));
        toggle_auto = ((ToggleButton) findViewById(R.id.toggle_auto));
        btn_send = ((Button) findViewById(R.id.btn_send));
        btn_recClear = ((Button) findViewById(R.id.btn_recClear));
        btn_recClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                txt_recCount.setText(getString(R.string.zero));
                totalRecByte = 0;
                singleRecByte = 0;
                builderRec.delete(0, builderRec.length());
                edit_recArea.setText(builderRec.toString());
                bytArryRecPos = 0;
                edit_recArea.setSelection(edit_recArea.length());
            }
        });
        btn_sendClear = ((Button) findViewById(R.id.btn_sendClear));
        btn_sendClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                txt_sendCount.setText(getString(R.string.zero));
                edit_sendArea.setText(null);

                doMotorControl();
            }
        });
        layout_sendAreaBar = ((LinearLayout) findViewById(R.id.layout_sendAreaBar));
        layout_sendArea = ((LinearLayout) findViewById(R.id.layout_sendArea));
        txt_sendAreaBar = ((TextView) findViewById(R.id.txt_sendAreaBar));
        txt_sendAreaTip = ((TextView) findViewById(R.id.txt_sendAreaTip));
        txt_sendAreaBar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showSendAreaOrBar();
            }
        });
        txt_sendAreaTip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showSendAreaOrBar();
            }
        });
        edit_sendArea = ((EditText) findViewById(R.id.edit_sendArea));
        edit_recArea = ((EditText) findViewById(R.id.edit_recArea));
        edit_recArea.setKeyListener(null);

        edit_recArea.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                if ((edit_recArea.getText().toString() != null) && (edit_recArea.getText().toString().length() > 0)) {
                    // TODO: 2016/12/6 添加长按保存数据代码 :
                }
                return false;
            }
        });
        edit_recArea.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if ((motionEvent.getAction() == 0) && (Comm.isDoubleClick(gActivity, 500, false)))
                    showSendAreaOrBar();
                return false;
            }
        });

        check_recHex.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (recHexFlag)
                    recHexFlag = false;
                else
                    recHexFlag = true;

                if (bytArryRec != null) {
                    String strTmp = Comm.byteArray2String(bytArryRec, 0, bytArryRecPos, isChecked);
                    builderRec.delete(0, builderRec.length());
                    builderRec.append(strTmp);
                    edit_recArea.setText(strTmp);
                    edit_recArea.setSelection(edit_recArea.length());
                }

            }
        });
        check_sendHex.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                sendHexFlag = isChecked;
              /*  if (sendHexFlag)
                    sendHexFlag = false;
                else
                    sendHexFlag = true;*/
                // TODO: 2016/12/6 添加代码 :
            }
        });
        toggle_recPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                stopRecFlag = isChecked;
               /* if (stopRecFlag)
                    stopRecFlag = false;
                else
                    stopRecFlag = true;*/
                // TODO: 2016/12/6 添加代码 :

            }
        });
        toggle_sendMemory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // TODO: 2016/12/6 添加代码 :
            }
        });
        toggle_auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) {
                    //自动发送代码 :
                    //创建定时器
                    autoSendTimer = new Timer();
                    //启动定时器 :
                    autoSendTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn_send.performClick(); //使用代码主动去调用控件的点击事件(模拟人手去触摸控件)
                                }
                            });
                        }
                    }, 0, 500);    //定时500ms发送一次 ;
                } else {
                    if (autoSendTimer != null) {
                        //物镜切换控制耐久测试 :
                        autoSendTimer.cancel();
                        autoSendTimer = null;
                    }
                }


                // TODO: 2016/12/6 添加代码 :
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                EXECUTOR_SERVICE.execute(new Runnable() {
                    @Override
                    public void run() {
                        int dataLength = edit_sendArea.length();
                       // int successDataLength = 0;

                        if (dataLength != 0) {
                            if (sendHexFlag) {
                                //Hex发送 :
                                writeBuffer = HexDump.hexStringToByteArray(edit_sendArea.getText().toString());
                            } else {
                                //字符直接发送 :
                                writeBuffer = new byte[dataLength];
                                for (int index = 0; index < dataLength; index++) {
                                    writeBuffer[index] = ((byte) edit_sendArea.getText().charAt(index));
                                }
                            }

                          //  Log.w("UsbSerialActivity", "WriteData Length is " + writeBuffer.length);

                            if (mSerialIoManager != null) {
                                mSerialIoManager.writeAsync(writeBuffer);
                            }

                            /*
                            try {
                                successDataLength = uartInterface.WriteData(writeBuffer, dataLength);
                                if (successDataLength != dataLength) {
                                    Toast.makeText(getApplicationContext(), "WriteData Error", Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            */
                        }
                    }
                });

                // TODO: 2016/12/6 添加发送代码 :
            }
        });

        img_menu = ((ImageView) findViewById(R.id.img_menu));
        img_menu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doMenuSetting();
            }
        });


        //        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);    //根据物理方向传感器确定方向 ;
        //        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);   //限制此页面数竖屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);   //限制此页面横屏显示，

        Exit.getInstance(gActivity).addActivity(this);

        mAdapter = new ArrayAdapter<UsbSerialPort>(this, android.R.layout.simple_expandable_list_item_2, mEntries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TwoLineListItem row;
                if (convertView == null) {
                    final LayoutInflater inflater =
                            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    row = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = (TwoLineListItem) convertView;
                }

                final UsbSerialPort port = mEntries.get(position);
                final UsbSerialDriver driver = port.getDriver();
                final UsbDevice device = driver.getDevice();

                final String title = String.format("Vendor %s Product %s",
                        HexDump.toHexString((short) device.getVendorId()),
                        HexDump.toHexString((short) device.getProductId()));
                row.getText1().setText(title);

                final String subtitle = driver.getClass().getSimpleName();
                row.getText2().setText(subtitle);

                return row;
            }
        };

        //        btn_send.performClick(); //使用代码主动去调用控件的点击事件(模拟人手去触摸控件)
        //        edit_recArea.setSelection(edit_recArea.length()); //让光标移动到最后 !
    }


    protected void onDestroy() {
        super.onDestroy();
    }



    protected void onResume() {
        super.onResume();
        initializeDriver();
        mHandler.sendEmptyMessage(MESSAGE_REFRESH);

    }

    private void initializeDriver() {
        Log.d(TAG, "initializeDriver, port=" + sPort);
        if (sPort == null) {
            txt_title.setText("【 接收显示区 】");
            txt_title.append("No serial device.");
        } else {

            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                txt_title.setText("【 接收显示区 】");
                txt_title.append("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                //                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE, UsbSerialPort.FLOWCONTROL_NONE);

                showStatus(edit_recArea, "CD  - Carrier Detect", sPort.getCD());
                showStatus(edit_recArea, "CTS - Clear To Send", sPort.getCTS());
                showStatus(edit_recArea, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(edit_recArea, "DTR - Data Terminal Ready", sPort.getDTR());
                showStatus(edit_recArea, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(edit_recArea, "RI  - Ring Indicator", sPort.getRI());
                showStatus(edit_recArea, "RTS - Request To Send", sPort.getRTS());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                txt_title.setText("【 接收显示区 】");
                txt_title.append("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                sPort = null;
                return;
            }
            txt_title.setText("【 接收显示区 】");
            txt_title.append("Serial device: " + sPort.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    protected void onStop() {
        super.onStop();
    }


    //====================================================
    //2016.12.06 : 新增 :
    private final String TAG = "UsbSerialActivity";
    private static UsbSerialPort sPort = null;
    private SerialInputOutputManager mSerialIoManager;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    //创建一个可重用固定线程数的线程池
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);

    //创建一个可重用固定线程数的延时线程池
    public static final ScheduledExecutorService EXECUTOR_SERVICE_SCHEDULED = Executors.newScheduledThreadPool(3);

    public byte[] writeBuffer = new byte[512];

    private Timer autoSendTimer;

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     */
    public static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, UsbSerialActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateReceivedData(data);
                }
            });
        }
    };


    void showStatus(TextView theTextView, String theLabel, boolean theValue) {
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
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

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                e.printStackTrace();
                // Ignore.
            }
            sPort = null;
        }
        //        finish();
        //        mHandler.removeMessages(MESSAGE_REFRESH);
    }


    public final int DATA_FRAMES_LENGTH = 22;           //数据帧包含的字节长度 ;
    public int mCurrentRecByteCount = 0;                //当前接收到数据帧的第n字节;
    public int mRecByteCountSecure = 0;                 //当前接收到数据帧的第n字节(用于容错计数);
    public boolean mDataFramesHeadFlag = false;      //当正确接收到数据帧帧头标识;
    public boolean mDataFramesSecureFlag = false;      //当正确接收到数据帧帧头标识(用于容错计数);

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
    public int mTransientLensMotorStatus = 0;      //物镜电机2 位置状态 (瞬时值)
    public int mTransientFocusMotorStatus = 0;      //对焦电机3 位置状态 (瞬时值)
    public float mTransientVoltageValue = 0;         //AD电压值  (瞬时值)
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
    public int mLensMotorStatus = 0;      //物镜电机2 位置状态
    public int mFocusMotorStatus = 0;      //对焦电机3 位置状态
    public float mVoltageValue = 0;          //AD电压值
    public long mChecksum = 0;             //校验和  


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
                        "\nFocusMotorStatus = " + mFocusMotorStatus;

        for (int i = 0; i < array.length; i++) {
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
                                        if ((array[i] & 0x01) == 0x01)    //LED1:bit0  1：打开; 0：关闭
                                            LED1_ON_FLAG_T = true;
                                        else
                                            LED1_ON_FLAG_T = false;
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
                                    mChecksum = 0;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 5:     //灯光 电机1 位置状态
                                    if (array[i] != -1)        //判断是否为有效数据
                                        mTransientLightMotorStatus = array[i];
                                    else
                                        mTransientLightMotorStatus = 0;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    break;
                                case 6:     //对焦 电机2位置 bit[31:24]
                                    mTransientFocusMotorStatus = 0;
                                    if (array[i] != -1) {        //判断是否为有效数据
                                        //                                    mTransientFocusMotorStatus += (array[i] << 24);
                                        //                                    mTransientFocusMotorStatus &= 0x00FFFFFF;
                                        //                                    mTransientFocusMotorStatus |= (((array[i] & 0xFF) << 24) & 0xFF000000);
                                        //                                    mTransientFocusMotorStatus += ((((long) (array[i] & 0xFF)) << 24) & 0xFF000000);
                                        //                                    mTransientFocusMotorStatus += (((long) (array[i] & 0xFF)) << 24);
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
                                        //                                    mTransientFocusMotorStatus += (array[i] << 16);
                                        //                                    mTransientFocusMotorStatus &= 0xFF00FFFF;
                                        //                                    mTransientFocusMotorStatus |= (((array[i] & 0xFF) << 16) & 0x00FF0000);
                                        //                                    mTransientFocusMotorStatus += ((((long) (array[i] & 0xFF)) << 16) & 0x00FF0000);
                                        //                                    mTransientFocusMotorStatus += (((long) (array[i] & 0xFF)) << 16);
                                        //                                        mTransientFocusMotorStatus += ((long) (((int) (array[i])) << 16));
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
                                        //                                    mTransientFocusMotorStatus += (array[i] << 8);
                                        //                                    mTransientFocusMotorStatus &= 0xFFFF00FF;
                                        //                                    mTransientFocusMotorStatus |= (((array[i] & 0xFF) << 8) & 0x0000FF00);
                                        //                                    mTransientFocusMotorStatus += ((((long) (array[i] & 0xFF)) << 8) & 0x0000FF00);
                                        //                                    mTransientFocusMotorStatus += (((long) (array[i] & 0xFF)) << 8);
                                        //                                        mTransientFocusMotorStatus += ((long) (((int) (array[i])) << 8));
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
                                        //                                    mTransientFocusMotorStatus += (array[i] << 0);
                                        //                                    mTransientFocusMotorStatus &= 0xFFFFFF00;
                                        //                                    mTransientFocusMotorStatus |= (((array[i] & 0xFF) << 0) & 0x000000FF);
                                        //                                    mTransientFocusMotorStatus += ((((long) (array[i] & 0xFF)) << 0) & 0x000000FF);
                                        //                                    mTransientFocusMotorStatus += (((long) (array[i] & 0xFF)) << 0);
                                        //                                        mTransientFocusMotorStatus += ((long) (((int) (array[i])) << 0));
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
                                        //                                    mTransientLensMotorStatus += (array[i] << 8);
                                        //                                    mTransientLensMotorStatus &= 0x00FF;
                                        //                                    mTransientLensMotorStatus |= (((array[i] & 0xFF) << 8) & 0xFF00);
                                        //                                    mTransientLensMotorStatus += ((((long) (array[i] & 0xFF)) << 8) & 0xFF00);
                                        //                                    mTransientLensMotorStatus += (((long) (array[i] & 0xFF)) << 8);
                                        //                                    mTransientLensMotorStatus += ((long) (((int) array[i])) << 8);
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
                                        //                                    mTransientLensMotorStatus += (array[i] << 0);
                                        //                                    mTransientLensMotorStatus &= 0xFF00;
                                        //                                    mTransientLensMotorStatus |= (((array[i] & 0xFF) << 0) & 0x00FF);
                                        //                                    mTransientLensMotorStatus += ((((long) (array[i] & 0xFF)) << 0) & 0x00FF);
                                        //                                    mTransientLensMotorStatus += (((long) (array[i] & 0xFF)) << 0);
                                        //                                    mTransientLensMotorStatus += ((long) (((int) array[i]) << 0));
                                        mTransientLensMotorStatus += ((array[i] << 0) & 0x00000000000000FF);
                                        //                                        Log.w(TAG, "B bit[7:0] = " + array[i] + " , " + (array[i] & 0xFF) + " , " + ((array[i] << 0) & 0x00000000000000FF) + " , " + (((array[i] & 0xFF) << 0) & 0x000000FF) + " , " + (array[i] << 0) + " , " + ((array[i] << 0) & 0x000000FF));
                                        //                                    mTransientLensMotorStatus &= 0xFFFF;
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
                                        mTransientVoltageValue += (array[i] / 10);
                                    else
                                        mTransientVoltageValue += 0;
                                    mChecksum += (array[i] & 0x00000000000000FF);
                                    //                                    Log.w(TAG, "  case 14 ! === ");
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
                                    //                                    Log.w(TAG, "  case 18 ! === ");
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
                                        Log.e(TAG, "mChecksum = " + mChecksum + " , mTransientChecksum = " + mTransientChecksum);
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
                                        Log.e(TAG, "array[i] != 0x0d = " + array[i]);
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
                                        Log.e(TAG, "array[i] != 0x0a = " + array[i]);
                                    }
                                    mChecksum = 0;
                                    //                                    Log.w(TAG, "  case 21 ! === ");
                                case 22:
                                    if (mDataFramesFinishFlag & mCheckSumFlag) {
                                        Log.w(TAG, " successful finish ! === ");

                                        LED1_ON_FLAG = LED1_ON_FLAG_T;
                                        LED2_ON_FLAG = LED2_ON_FLAG_T;
                                        LED3_ON_FLAG = LED3_ON_FLAG_T;
                                        LED4_ON_FLAG = LED4_ON_FLAG_T;

                                        LIGHTSWITCH1_ON_FLAG = LIGHTSWITCH1_ON_FLAG_T;
                                        LIGHTSWITCH2_ON_FLAG = LIGHTSWITCH2_ON_FLAG_T;
                                        LIGHTSWITCH3_ON_FLAG = LIGHTSWITCH3_ON_FLAG_T;
                                        LIGHTSWITCH4_ON_FLAG = LIGHTSWITCH4_ON_FLAG_T;
                                        LIGHTSWITCH5_ON_FLAG = LIGHTSWITCH5_ON_FLAG_T;
                                        LIGHTSWITCH6_ON_FLAG = LIGHTSWITCH6_ON_FLAG_T;

                                        mVoltageValue = mTransientVoltageValue;
                                        mLightMotorStatus = mTransientLightMotorStatus;
                                        mFocusMotorStatus = mTransientFocusMotorStatus;
                                        mLensMotorStatus = mTransientLensMotorStatus;
                                    } else {
                                        Log.e(TAG, "mDataFramesFinishFlag = " + mDataFramesFinishFlag + " , mCheckSumFlag = " + mCheckSumFlag);
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

    private void updateReceivedData(byte[] data) {
        final String message;
        final String midMessage;
        if (!stopRecFlag) {

            totalRecByte += data.length;
            singleRecByte += data.length;
            if (recHexFlag) {
                //                midMessage = HexDump.dumpHexString(data);
                midMessage = dataFramesToValue(data);

                if (midMessage != "" & midMessage.length() != 0) {
                    message = "Read " + data.length + " bytes: \n"
                            + midMessage + "\n\n";
                    if (singleRecByte > 5000) {    //超过5000字节时自动清空显示
                        singleRecByte = 0;
                        edit_recArea.setText("");
                    }
                    txt_recCount.setText("" + totalRecByte);
                    edit_recArea.append(message);
                    edit_recArea.setSelection(edit_recArea.length()); //让光标移动到最后 !
                    view_scrollView.smoothScrollTo(0, edit_recArea.getBottom());
                }

            } else {
                //            midMessage = new String(data, 0, data.length);
                message = new String(data, 0, data.length);
                if (message != " " | message.length() != 0) {
                    //        mDumpTextView.append(message);
                    //        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
                    if (singleRecByte > 5000) {     //超过5000字节时自动清空显示
                        singleRecByte = 0;
                        edit_recArea.setText("");
                    }
                    txt_recCount.setText("" + totalRecByte);
                    edit_recArea.append(message);
                    edit_recArea.setSelection(edit_recArea.length()); //让光标移动到最后 !
                    view_scrollView.smoothScrollTo(0, edit_recArea.getBottom());
                }
            }
        }
    }

    private void doMenuSetting() {
        final View localView = gActivity.getLayoutInflater().inflate(R.layout.dialog_menu_setting_new, null);

        TextView txt_SPortStatus = ((TextView) localView.findViewById(R.id.txt_DeviceStatus));
        //        EditText edit_baudRate = ((EditText) localView.findViewById(R.id.edit_baudRate));
        final Spinner spinner_devices = ((Spinner) localView.findViewById(R.id.spinner_devices));
        final Spinner spinner_baudRate = ((Spinner) localView.findViewById(R.id.spinner_baudRate));
        final Spinner spinner_stopBits = ((Spinner) localView.findViewById(R.id.spinner_stopBits));
        final Spinner spinner_dataBits = ((Spinner) localView.findViewById(R.id.spinner_dataBits));
        final Spinner spinner_parity = ((Spinner) localView.findViewById(R.id.spinner_parity));
        final Spinner spinner_flowControl = ((Spinner) localView.findViewById(R.id.spinner_flowControl));
        final EditText edit_sendAutoInterval = ((EditText) localView.findViewById(R.id.edit_sendAutoInterval));

        txt_SPortStatus.setText(sPort != null ? gActivity.getString(R.string.info_ft23xOk) : gActivity.getString(R.string.info_ft23xNo));

        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_devices.setAdapter(mAdapter);

        spinner_devices.setSelection(0);
        spinner_baudRate.setSelection(5);   //默认选择9600 波特率
        spinner_dataBits.setSelection(0);
        spinner_stopBits.setSelection(0);
        spinner_parity.setSelection(0);
        spinner_flowControl.setSelection(0);
        edit_sendAutoInterval.setText("500");

        Comm.showDiaog(new AlertDialog.Builder(gActivity).setIcon(R.drawable._48_setting).setTitle(R.string.info_setting).setView(localView).setPositiveButton(R.string.isOk, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                //                String baudRate = ((EditText) localView.findViewById(edit_baudRate)).getText().toString().trim();
                String sendAutoInterval = edit_sendAutoInterval.getText().toString().trim();
                //                if ((baudRate.matches("[0-9]{3,6}")) && (sendAutoInterval.matches("[0-9]{3,6}"))) {
                if ((sendAutoInterval.matches("[0-9]{3,6}"))) {
                    //                    Integer[] arrayOfInteger = new Integer[Integer.parseInt(baudRate)];
                    Integer[] arrayOfInteger = new Integer[6];
                    //                    arrayOfInteger[0] = Integer.valueOf(Integer.parseInt(baudRate));
                    arrayOfInteger[0] = Integer.valueOf(((String) spinner_baudRate.getSelectedItem()).trim());
                    arrayOfInteger[1] = Integer.valueOf(8 - (int) spinner_dataBits.getSelectedItemId());
                    arrayOfInteger[2] = Integer.valueOf((int) spinner_stopBits.getSelectedItemId() + 1);
                    arrayOfInteger[3] = Integer.valueOf((int) spinner_parity.getSelectedItemId());
                    arrayOfInteger[4] = Integer.valueOf((int) spinner_flowControl.getSelectedItemId());
                    arrayOfInteger[5] = Integer.valueOf(Integer.parseInt(sendAutoInterval));

                    if (sPort != null) {
                        try {
                            sPort.close();
                            sPort = null;
                            sPort = mEntries.get((int) spinner_devices.getSelectedItemId());
                            if (sPort != null) {
                                UsbDeviceConnection connection = mUsbManager.openDevice(sPort.getDriver().getDevice());
                                if (connection != null) {
                                    //
                                    sPort.open(connection);

                                    sPort.setParameters(arrayOfInteger[0].intValue(),
                                            arrayOfInteger[1].intValue(),
                                            arrayOfInteger[2].intValue(),
                                            arrayOfInteger[3].intValue(),
                                            arrayOfInteger[4].intValue());

                                    showStatus(edit_recArea, "CD  - Carrier Detect", sPort.getCD());
                                    showStatus(edit_recArea, "CTS - Clear To Send", sPort.getCTS());
                                    showStatus(edit_recArea, "DSR - Data Set Ready", sPort.getDSR());
                                    showStatus(edit_recArea, "DTR - Data Terminal Ready", sPort.getDTR());
                                    showStatus(edit_recArea, "DSR - Data Set Ready", sPort.getDSR());
                                    showStatus(edit_recArea, "RI  - Ring Indicator", sPort.getRI());
                                    showStatus(edit_recArea, "RTS - Request To Send", sPort.getRTS());

                                    Toast.makeText(gActivity, gActivity.getString(R.string.info_setDone), Toast.LENGTH_SHORT).show();

                                    onDeviceStateChange();

                                } else
                                    Toast.makeText(gActivity, "连接失败 !", Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(gActivity, "找不到设备 !", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else
                        Toast.makeText(gActivity, "找不到设备 !", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(gActivity, gActivity.getString(R.string.err_numberFormat), Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        }).setNegativeButton(R.string.isCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create());

    }

    public static int focusProcess = 0;
    public static int lightProcess = 0;
    public static int lensProcess = 0;

    public static int motor_speed = 0;
    public static int motor_step = 0;
    public static boolean motor_direction = true;
    public static boolean motor_model = true;


    private void doMotorControl() {
        final View localView = gActivity.getLayoutInflater().inflate(R.layout.dialog_menu_motor_control, null);

        final Button btn_stop_focus = ((Button) localView.findViewById(R.id.btn_stop_focus));
        final Button btn_stop_light = ((Button) localView.findViewById(R.id.btn_stop_light));
        final Button btn_stop_lens = ((Button) localView.findViewById(R.id.btn_stop_lens));

        final SeekBar seekbar_motor_focus = ((SeekBar) localView.findViewById(R.id.seekbar_motor_focus));
        final SeekBar seekbar_motor_light = ((SeekBar) localView.findViewById(R.id.seekbar_motor_light));
        final SeekBar seekbar_motor_lens = ((SeekBar) localView.findViewById(R.id.seekbar_motor_lens));

        final TextView txt_progress_focus = ((TextView) localView.findViewById(R.id.txt_progress_focus));
        final TextView txt_progress_light = ((TextView) localView.findViewById(R.id.txt_progress_light));
        final TextView txt_progress_lens = ((TextView) localView.findViewById(R.id.txt_progress_lens));

        final SeekBar seekbar_motor_speed = ((SeekBar) localView.findViewById(R.id.seekbar_motor_speed));
        final SeekBar seekbar_motor_step = ((SeekBar) localView.findViewById(R.id.seekbar_motor_step));

        final TextView txt_progress_speed = ((TextView) localView.findViewById(R.id.txt_progress_speed));
        final TextView txt_progress_step = ((TextView) localView.findViewById(R.id.txt_progress_step));

        final ToggleButton toggle_direction = ((ToggleButton) localView.findViewById(R.id.toggle_direction));
        final ToggleButton toggle_model = ((ToggleButton) localView.findViewById(R.id.toggle_model));

        final Button btn_send_debug = ((Button) localView.findViewById(R.id.btn_send_debug));

        seekbar_motor_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_progress_speed.setText(progress + " / " + seekbar_motor_speed.getMax());
                motor_speed = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbar_motor_step.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_progress_step.setText(progress + " / " + seekbar_motor_step.getMax());
                motor_step = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        toggle_direction.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                motor_direction = isChecked;
                if (isChecked) {    //正转

                } else {        //反转
                }

            }
        });

        toggle_model.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                motor_model = isChecked;
                if (isChecked) {    //一直转

                } else {        //按步转
                }
            }
        });

        btn_send_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selection = "02";    //02电机

                String direction = "55";    //电机运作方向; 默认正转0x55 , 反转0xAA
                String step = "5555";    //电机运转的步数 : 小端方式 , 高位存在地址高位
                String speed = "08";    //电机运转的步数 : 小端方式 , 高位存在地址高位
                String stepModle = "AA";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA
                if (motor_direction)
                    direction = "55";
                else
                    direction = "AA";

                if (motor_model)
                    stepModle = "AA";
                else
                    stepModle = "55";

                step = integerTo2ByteHexString(motor_step & 0xffff);
                speed = integerToHexString(motor_speed & 0xff);

                String commandString = "4A504C59" + selection + direction + step + stepModle + speed + "0000" + "0D0A";
                sendControlCommand(commandString);
                requestCallBackStatus();
            }
        });


        btn_stop_focus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selection = "02";    //02电机
                String commandString = "4A504C59" + "08" + selection + "000000000000" + "0D0A";
                sendControlCommand(commandString);
                requestCallBackStatus();

                //                String step = integerTo2ByteHexString(65280 & 0xffff);     //0x64
                //                String commandString = "4A504C59" + "08" + "04" + "0000" + step + "0000" + "0D0A";
                //                sendControlCommand(commandString);

            }
        });
        btn_stop_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selection = "01";    //01电机
                //加回车换行符 : 0x0D0A
                String commandString = "4A504C59" + "08" + selection + "000000000000" + "0D0A";
                sendControlCommand(commandString);
                requestCallBackStatus();

                //                String step = integerTo2ByteHexString(200 & 0xffff);    //0xC8
                //                //                String commandString = "4A504C59" + "04" + "04" + "0000" + step + "0000" + "0D0A";
                //                String commandString = "4A504C59" + "02" + "03" + "00000000" + step + "0D0A";
                //                sendControlCommand(commandString);
            }
        });
        btn_stop_lens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selection = "05";    //05电机
                String commandString = "4A504C59" + "08" + selection + "000000000000" + "0D0A";
                sendControlCommand(commandString);
                requestCallBackStatus();

                //                String step = integerTo2ByteHexString(65535 & 0xffff);    //0x012C
                //                String commandString = "4A504C59" + "04" + "02" + "0000" + step + "0000" + "0D0A";
                //                sendControlCommand(commandString);
            }
        });

        //对焦电机
        seekbar_motor_focus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_progress_focus.setText(progress + " / " + seekbar_motor_focus.getMax());
                String selection = "02";    //02电机
                String direction = "55";    //电机运作方向; 默认正在0x55 , 反转0xAA
                String step = "5555";    //电机运转的步数 : 小端方式 , 高位存在地址高位
                String stepModle = "AA";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA
                if (progress > focusProcess) {
                    direction = "55";
                    //                    step = Integer.toHexString((progress - focusProcess) & 0xffff);
                    step = integerTo2ByteHexString((progress - focusProcess) & 0xffff);
                } else if (progress < focusProcess) {
                    direction = "AA";
                    //                    step = Integer.toHexString((focusProcess - progress) & 0xffff);
                    step = integerTo2ByteHexString((focusProcess - progress) & 0xffff);
                }
                focusProcess = progress;

                String commandString = "4A504C59" + selection + direction + step + stepModle + "080500" + "0D0A";
                sendControlCommand(commandString);
                requestCallBackStatus();

                //                step = integerTo2ByteHexString(progress & 0xffff);
                //                String commandString = "4A504C59" + "04" + "04" + "0000" + step + "0000" + "0D0A";
                //                sendControlCommand(commandString);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //灯光电机
        seekbar_motor_light.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_progress_light.setText(progress + " / " + seekbar_motor_light.getMax());
                String selection = "01";    //01电机
                String direction = "55";    //电机运作方向; 默认正在0x55 , 反转0xAA
                String step = "5555";    //电机运转的步数 : 小端方式 , 高位存在地址高位
                String stepModle = "11";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA ,位置模式0x11
                if (progress > lightProcess) {
                    direction = "55";
                    //                    step = Integer.toHexString((progress - lightProcess) & 0xffff);
                } else if (progress < lightProcess) {
                    direction = "AA";
                    //                    step = Integer.toHexString((lightProcess - progress) & 0xffff);
                }
                //                step = Integer.toHexString(progress & 0xffff);
                step = integerTo2ByteHexString(progress & 0xffff);
                lightProcess = progress;
                //加回车换行符 : 0x0D0A
                String commandString = "4A504C59" + selection + direction + step + stepModle + "080500" + "0D0A";
                //                String commandString = "4A504C59" + selection + direction + step + stepModle + "080500" + "0D0A";
                sendControlCommand(commandString);
                saturationLightControl(progress);
                requestCallBackStatus();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //物镜电机
        seekbar_motor_lens.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_progress_lens.setText(progress + " / " + seekbar_motor_lens.getMax());
                String selection = "05";    //05电机
                String direction = "55";    //电机运作方向; 默认正在0x55 , 反转0xAA
                String step = "5555";    //电机运转的步数 : 小端方式 , 高位存在地址高位
                String stepModle = "AA";    //电机运转的模式 ; 一直转0x55 , 按步数转0xAA
                if (progress > lensProcess) {
                    direction = "55";
                    //                    step = Integer.toHexString((progress - lensProcess) & 0xffff);
                    step = integerTo2ByteHexString((progress - lensProcess) & 0xffff);
                } else if (progress < lensProcess) {
                    direction = "AA";
                    //                    step = Integer.toHexString((lensProcess - progress) & 0xffff);
                    step = integerTo2ByteHexString((lensProcess - progress) & 0xffff);
                }
                lensProcess = progress;

                String commandString = "4A504C59" + selection + direction + step + stepModle + "080500" + "0D0A";
                sendControlCommand(commandString);
                requestCallBackStatus();

                //                step = integerTo2ByteHexString(progress & 0xffff);
                //                String commandString = "4A504C59" + "04" + "04" + "00000000" + step + "0D0A";
                //                sendControlCommand(commandString);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Comm.showDiaog(new AlertDialog.Builder(gActivity).setIcon(R.drawable._48_setting).setTitle(R.string.info_cammand).setView(localView).setPositiveButton(R.string.isOk, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        }).setNegativeButton(R.string.isCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create());
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

    public void saturationLightControl(int lightID) {       //激发灯光开关控制 :
        byte lightId = 1;
        String lightIDString = "00";
        if (lightID != 0)
            lightIDString = integerToHexString((lightId << (lightID - 1)) & 0xff);

        //加回车换行符 : 0x0D0A
        String commandString = "4A504C59" + "07" + lightIDString + "000000000000" + "0D0A";
        sendControlCommand(commandString);
    }


    public void requestCallBackStatus() {
        String commandString = "4A504C59" + "10" + "00000000000000" + "0D0A";
        sendControlCommand(commandString);
        sendControlCommand(commandString);
        sendControlCommand(commandString);  //查询三次 ;
    }


    public void sendControlCommand(final String commandString) {
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                //Hex发送 :
                byte[] writeBuffer = HexDump.hexStringToByteArray(commandString);

                if (mSerialIoManager != null) {
                    mSerialIoManager.writeAsync(writeBuffer);
                }
            }
        });
    }

    //=========================================
    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 5000;

    private List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();

    private ArrayAdapter<UsbSerialPort> mAdapter;

    private UsbManager mUsbManager;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    refreshDeviceList();
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    private void refreshDeviceList() {

        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            @Override
            protected List<UsbSerialPort> doInBackground(Void... params) {
                Log.d(TAG, "Refreshing device list ...");
                SystemClock.sleep(1000);

                final List<UsbSerialDriver> drivers =
                        UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    Log.d(TAG, String.format("+ %s: %s port%s",
                            driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
                    result.addAll(ports);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<UsbSerialPort> result) {
                mEntries.clear();
                mEntries.addAll(result);
                mAdapter.notifyDataSetChanged();

                //                mProgressBarTitle.setText(
                //                        String.format("%s device(s) found", Integer.valueOf(mEntries.size())));
                //                hideProgressBar();

                Log.d(TAG, "Done refreshing, " + mEntries.size() + " entries found.");
            }

        }.execute((Void) null);
    }


}



