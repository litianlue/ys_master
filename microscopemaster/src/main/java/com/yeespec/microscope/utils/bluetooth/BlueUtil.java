package com.yeespec.microscope.utils.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.yeespec.libuvccamera.bluetooth.DeviceBean;
import com.yeespec.libuvccamera.usart.usbserial.util.HexDump;

import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.UIUtil;
import com.yeespec.microscope.utils.detector.Maths;
import com.yeespec.microscope.utils.wifi.WifiConnect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2017/5/18.
 */

public class BlueUtil {
    private String BlueAddress = null;
    private static final String TAG = "BlueUtil";
    public static boolean RE_CONNECT_BT = false;
    private final String CONNECT_NETWORK = "connect_network";
    private static boolean isFinish = false;//完成发送串口指令动作
    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private Context context;
    private int connectsize = 0;
    private int countConnect = 0;
    private ArrayList<DeviceBean> mDatas;
    //蓝牙适配器
    private BluetoothDevice mDevice;
    private BluetoothAdapter mBtAdapter;
    // 蓝牙客户端socket
    private static BluetoothSocket mSocket;
    private ClientThread mClientThread;
    private ReadThread mReadThread;
    private String buletoothName;
    private static final ScheduledExecutorService FINISHSATEXY_EXECUTOR = Executors.newScheduledThreadPool(1);
    private String restartMotor = "16";//复位电机
    private String left_RightMotor = "03";//X轴
    private String top_BottomMotor = "04";//Y轴

    public BlueUtil(Context context, BluetoothSocket mSocket, BluetoothDevice mDevice) {
        this.context = context;
        this.mSocket = mSocket;
        this.mDevice = mDevice;
    }

    public BlueUtil(Context context) {
        this.context = context;
    }

    public static void resateXY() {
        String commandString = "4A504C59" + "16" + "05" + "00" + "000000000000";
        sendMessageHandle(commandString);//复位电机
    }

    private Handler bluehandle = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    break;
                case 2:

                    UIUtil.toast(context, "" + msg.obj, false);
                    Bundle data = msg.getData();
                    ConstantUtil.CONNECT_BLUE_NAME = data.getString("bluename");
                    resateXY();

                    // BlueUtil.moveXYState(955,751);
                    break;
                case 3:

                    String s = msg.obj.toString();
                   /* String x = s.substring(20, 24);
                    String y = s.substring(30, 34);*/
                    String x = s.substring(16, 20);
                    String y = s.substring(24, 28);
                    Log.d("MasterActivity", "s="+s);
                    int xstate = Integer.parseInt(("0x" + x).replaceAll("^0[x|X]", ""), 16);
                    int ystate = Integer.parseInt(("0x" + y).replaceAll("^0[x|X]", ""), 16);
                    DataUtil.stateI = xstate;
                    DataUtil.stateII = ystate;

                    Log.d("MasterActivity", "X轴=" + x+"  xstate="+xstate);
                    Log.d("MasterActivity", "Y轴=" + y+"  ystate="+ystate);
                    break;
                case 4:
                    ConstantUtil.CONNECT_BLUE_NAME = "";

                    UIUtil.toast(context, "" + msg.obj, false);
                    countConnect++;
                    if (countConnect > 5 || RE_CONNECT_BT) {//最多重连3次
                        countConnect = 0;

                        UIUtil.toast(context, "连接失败!请检查蓝牙是否打开", false);
                        return;
                    }
                    //重新连接
                    connectBluetooth(ConstantUtil.CONNECT_BLUE_NAME);

                    break;
                case 5:

                    connectsize++;
                    if (connectsize > 15 || RE_CONNECT_BT) {//最多重连5次
                        connectsize = 0;
                        //UIUtil.toast(context, "请手动打开蓝牙!", false);
                        return;
                    }
                    if(BlueAddress!=null&&!BlueAddress.equals(""))
                        connectBluetooth(BlueAddress);
                    else
                    connectBluetooth("");


                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    String[] strings = readMessage.split("&");
                    if (strings[0].equals(CONNECT_NETWORK)) {
                        String uuid = strings[1];
                        String password = strings[2];
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        WifiConnect wifiConnect = new WifiConnect(context, wifiManager);
                        wifiConnect.OpenWifi();
                        connectWifi(wifiConnect, uuid, password);

                    }
                    UIUtil.toast(context, "readMessage=" + readMessage, false);

                    break;
                case MESSAGE_ENBLE_SERVICE:
                    AcceptThread acceptThread = new AcceptThread();
                    acceptThread.start();
                    // UIUtil.toast(context, "开启蓝牙", false);
                    break;
            }
        }
    };

    private void connectWifi(final WifiConnect wifiConnect, final String uuid, final String password) {
        WifiConnect.WifiCipherType mconnectType = WifiConnect.WifiCipherType.WIFICIPHER_WEP;
        wifiConnect.mConnect(uuid, password, mconnectType, new WifiConnect.WifiConnectListener() {
            @Override
            public void OnWifiConnectCompleted(boolean isConnected) {
                if (isConnected) {

                } else {
                    WifiConnect.WifiCipherType mconnectType = WifiConnect.WifiCipherType.WIFICIPHER_WPA;
                    wifiConnect.mConnect(uuid, password, mconnectType, new WifiConnect.WifiConnectListener() {
                        @Override
                        public void OnWifiConnectCompleted(boolean isConnected) {
                            if (isConnected) {
                                if (connectedThread != null) {
                                    connectedThread.write("wifi链接成功".getBytes());
                                }
                            } else {
                                if (connectedThread != null) {
                                    connectedThread.write("wifi链接失败".getBytes());
                                }
                            }
                        }
                    });
                }
            }
        });

    }

    public void setDiscoverableTimeout(int timeout) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);
            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectBluetooth(String maddress) {
        BlueAddress = maddress;
        mDatas = new ArrayList<DeviceBean>();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter != null) {
            if (!mBtAdapter.isEnabled()) {
                //打开蓝牙
               // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
               // enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               // context.startActivity(enableBtIntent);
                mBtAdapter.enable();

            }
        } else {
            UIUtil.toast(context, "该设备不支持蓝牙", false);
            return;
        }
        //UIUtil.toast(context, "mBtAdapter=" + mBtAdapter, false);
        //根据适配器得到所有的设备信息
        Set<BluetoothDevice> deviceSet = mBtAdapter.getBondedDevices();
        //UIUtil.toast(context, "deviceSet=" + deviceSet.size(), false);
        if (deviceSet.size() == 0) {
            Message msg = new Message();
            msg.what = 5;
            bluehandle.sendMessageDelayed(msg, 1000);
        }
        if (deviceSet.size() > 0) {
            for (BluetoothDevice bluetoothDevice : deviceSet) {
                bluetoothDevice.setPin("0000".getBytes());
                mDatas.add(new DeviceBean(bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress(), true));

                buletoothName = bluetoothDevice.getName().toString();
            }
        } else
            return;
        DeviceBean bean = mDatas.get(0);
        String info = bean.message;
        String address = info.substring(info.length() - 17);
        mBtAdapter.cancelDiscovery();
        //
        if (!"".equals(address)) {
            if (maddress == null || maddress.equals(""))
                mDevice = mBtAdapter.getRemoteDevice(address);
            else
                mDevice = mBtAdapter.getRemoteDevice(maddress);
            mClientThread = new ClientThread();
            mClientThread.start();

        } else {
            UIUtil.toast(context, "address is null !", false);

        }
    }

    // 客户端线程
    public class ClientThread extends Thread {
        public void run() {
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Message msg = new Message();
                msg.obj = "请稍候，正在连接服务器:";
                msg.what = 1;
                bluehandle.sendMessage(msg);

                mSocket.connect();

                msg = new Message();
                msg.obj = "已经连接上蓝牙！";
                DataUtil.isConnectBlue = true;
                msg.what = 2;
                Bundle bundle = new Bundle();
                bundle.putString("bluename", buletoothName);
                msg.setData(bundle);

                bluehandle.sendMessage(msg);
                // 启动接受数据
                mReadThread = new ReadThread();
                mReadThread.start();
            } catch (IOException e) {
                Message msg = new Message();
                msg.obj = "连接蓝牙异常！正在重连。";
                msg.what = 4;
                bluehandle.sendMessageDelayed(msg, 3000);
            }
        }
    }

    ;

    // 发送数据
    public static void sendMessageHandle(String msg) {
        if (mSocket == null) {
            return;
        }
        try {
            byte[] bytes = HexDump.hexStringToByteArray(msg);
            OutputStream os = mSocket.getOutputStream();
            os.write(bytes);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 读取数据
    private class ReadThread extends Thread {
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream is = null;
            StringBuffer bf = new StringBuffer();
            byte[] buf_data = null;
            try {
                is = mSocket.getInputStream();


                while (true) {
                    if ((bytes = is.read(buffer)) > 0) {
                        buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                    }
                    for (int i = 0; i < buf_data.length; i++) {

                        //Log.w("MasterActivity","buf_data[i]="+buf_data[i]);


                        String hexs = Integer.toHexString(buf_data[i] & 0xFF);
                        if (hexs.length() < 2) {
                            bf.append("0" + hexs);
                        } else
                            bf.append(hexs);
                    }
                    String bfstr = bf.toString();

                    if (bfstr.contains("4a504c59") && bfstr.contains("0d0a")) {
                        int head = bfstr.indexOf("4a504c59");
                        int tail = bfstr.indexOf("0d0a");

                        if (bfstr.length() < 2)
                            return;
                        if (head > tail) {
                            bfstr = bfstr.substring(tail + 2, bfstr.length());
                            head = bfstr.indexOf("4a504c59");
                            tail = bfstr.indexOf("0d0a");
                        }

                        if (bfstr.contains("4a504c59") && bfstr.contains("0d0a")) {
                            String s = bfstr.substring(head, tail + 4);
                            isFinish = true;
                            Message msg = new Message();
                            msg.obj = s;
                            msg.what = 3;
                            bluehandle.sendMessage(msg);
                            bf.delete(0, bf.length());

                            if (finishcount == 2) {
                                finishcount = 1;
                                isFinishXY = true;
                            }
                            Log.d("MasterActivity", "finishcount=" + finishcount);
                        }


                    }

                }

            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }

    public static void moveXSate(int sate, boolean ispositive) {
        String sendstr;
        String x;
        //Log.d("MasterActivity", "s=" +x);
        if (ispositive) {
            if (sate > 255) {
                if (sate < 4096) {
                    x = "0" + Integer.toHexString(sate);
                } else {
                    x = Integer.toHexString(sate);
                }
                sendstr = "4A504C59" + "03" + "aa" + x + "aa" + "0a" + "0000";
            } else {
                if (sate < 16)
                    x = "0" + Integer.toHexString(sate);
                else
                    x = Integer.toHexString(sate);
                sendstr = "4A504C59" + "03" + "aa" + "00" + x + "aa" + "0a" + "0000";
            }


        } else {
            if (sate > 255) {
                if (sate < 4096) {
                    x = "0" + Integer.toHexString(sate);
                } else {
                    x = Integer.toHexString(sate);
                }
                sendstr = "4A504C59" + "03" + "55" + x + "aa" + "0a" + "0000";
            } else {
                if (sate < 16)
                    x = "0" + Integer.toHexString(sate);
                else
                    x = Integer.toHexString(sate);
                sendstr = "4A504C59" + "03" + "55" + "00" + x + "aa" + "0a" + "0000";
            }
        }
        sendMessageHandle(sendstr);
    }

    public static boolean isFinishXY = false;//是否完成XY移动；
    private static int finishcount = 1;//1 为原始状态，2为移动Y轴开始状态

    public static void moveXYState(int x, final int y) {
        isFinishXY = false;

        int satex = Math.abs(DataUtil.stateI - x);
        if (DataUtil.stateI > x) {
            moveXSate(satex, false);
            isFinish = false;
            moveYsateMethod(y);
        } else if (DataUtil.stateI < x) {
            moveXSate(satex, true);
            isFinish = false;
            moveYsateMethod(y);
        } else {
            isFinish = true;
            moveYsateMethod(y);
        }

    }

    private static void moveYsateMethod(final int y) {
        executorService.schedule(new Runnable() {
            @Override
            public void run() {

                if (isFinish) {
                    isFinishXY = false;
                    finishcount = 2;
                    int satey = Math.abs(DataUtil.stateII - y);
                    if (DataUtil.stateII > y) {
                        moveYSate(satey, false);
                        // Log.d("MasterActivity", "DataUtil.stateII=" + DataUtil.stateII + " Y=" + y);
                    } else if (DataUtil.stateII < y) {
                        moveYSate(satey, true);
                        //Log.d("MasterActivity", "DataUtil.stateII=" + DataUtil.stateII + " Y=" + y);
                    } else {
                        finishcount = 1;
                        isFinishXY = true;
                    }
                    isFinish = false;
                } else {
                    moveYsateMethod(y);
                }
            }
        }, 200, TimeUnit.MILLISECONDS);
    }

    public static void moveYSate(int sate, boolean ispositive) {
        String sendstr;
        String x;
        if (ispositive) {
            if (sate > 255) {
                if (sate < 4096) {
                    x = "0" + Integer.toHexString(sate);
                } else {
                    x = Integer.toHexString(sate);
                }
                sendstr = "4A504C59" + "04" + "55" + x + "aa" + "0a" + "0000";
            } else {
                if (sate < 16)
                    x = "0" + Integer.toHexString(sate);
                else
                    x = Integer.toHexString(sate);
                sendstr = "4A504C59" + "04" + "55" + "00" + x + "aa" + "0a" + "0000";
            }


        } else {
            if (sate > 255) {
                if (sate < 4096) {
                    x = "0" + Integer.toHexString(sate);
                } else {
                    x = Integer.toHexString(sate);
                }
                sendstr = "4A504C59" + "04" + "aa" + x + "aa" + "0a" + "0000";
            } else {
                if (sate < 16)
                    x = "0" + Integer.toHexString(sate);
                else
                    x = Integer.toHexString(sate);
                sendstr = "4A504C59" + "04" + "aa" + "00" + x + "aa" + "0a" + "0000";
            }
        }
        sendMessageHandle(sendstr);
    }

    public void shutdownClient() {
        new Thread() {
            public void run() {
                if (mClientThread != null) {
                    mClientThread.interrupt();
                    mClientThread = null;
                }
                if (mReadThread != null) {
                    mReadThread.interrupt();
                    mReadThread = null;
                }
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mSocket = null;
                }
                DataUtil.isConnectBlue = false;
            }

            ;
        }.start();
    }

    private BluetoothAdapter bluetoothAdapter;
    private String NAME = "";
    private String MY_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66";
    private final int MESSAGE_READ = 0;
    private final int MESSAGE_ENBLE_SERVICE = 8;
    private ConnectedThread connectedThread;

    //作为服务端（无屏幕显示使用）
    public void startService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        NAME = bluetoothAdapter.getName();
        if (bluetoothAdapter == null) {
            //表明此手机不支持蓝牙
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
      /*  //启动修改蓝牙可见性的Intent
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //设置蓝牙可见性的时间，方法本身规定最多可见300秒
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            context.startActivity(intent);*/
            setDiscoverableTimeout(bluetoothAdapter, 120);
        }
        bluehandle.sendEmptyMessageDelayed(MESSAGE_ENBLE_SERVICE, 5000);

    }

    //通过反射机制开启蓝牙可见
    public void setDiscoverableTimeout(BluetoothAdapter bluetoothAdapter, int timeout) {

        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(bluetoothAdapter, timeout);
            setScanMode.invoke(bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {

            // Use a temporary object that is later assigned to mmServerSocket,

            // because mmServerSocket is final

            BluetoothServerSocket tmp = null;

            try {
                // MY_UUID is the app's UUID string, also used by the client code
                //listenUsingInsecureRfcommWithServiceRecord
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, UUID.fromString(MY_UUID));
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            // Keep listening until exception occurs or a socket is returned

            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {

                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }


        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        try {
            Log.w(TAG, "socket.connect()3");

            //BluetoothDevice remoteDevice = socket.getRemoteDevice();
            if (!socket.isConnected())
                socket.connect();

            Log.w(TAG, "socket.connect()=");
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;

        private final InputStream mmInStream;

        private final OutputStream mmOutStream;


        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;

            InputStream tmpIn = null;

            OutputStream tmpOut = null;


            // Get the input and output streams, using temp objects because

            // member streams are final

            try {

                tmpIn = socket.getInputStream();

                tmpOut = socket.getOutputStream();

            } catch (IOException e) {
            }


            mmInStream = tmpIn;

            mmOutStream = tmpOut;

        }


        public void run() {

            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()


            // Keep listening to the InputStream until an exception occurs

            while (true) {

                try {

                    // Read from the InputStream

                    bytes = mmInStream.read(buffer);

                    Log.w(TAG, "byte=" + bytes);

                    bluehandle.obtainMessage(MESSAGE_READ, bytes, -1, buffer)

                            .sendToTarget();

                } catch (IOException e) {

                    break;

                }

            }

        }



    /* Call this from the main Activity to send data to the remote device */

        public void write(byte[] bytes) {

            try {

                mmOutStream.write(bytes);

            } catch (IOException e) {
            }

        }



    /* Call this from the main Activity to shutdown the connection */

        public void cancel() {

            try {

                mmSocket.close();

            } catch (IOException e) {
            }

        }

    }
}
