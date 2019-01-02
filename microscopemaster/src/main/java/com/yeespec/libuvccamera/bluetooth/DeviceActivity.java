package com.yeespec.libuvccamera.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yeespec.R;
import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.SPUtils;
import com.yeespec.microscope.utils.UIUtil;
import com.yeespec.microscope.utils.bluetooth.BlueUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Mr.Wen on 2017/1/3.
 * 发现的设备列表
 */
public class DeviceActivity extends Activity implements OnClickListener {
    private ListView mListView;
    //数据
    private ArrayList<DeviceBean> mDatas;
    private Button mBtnSearch, mBtnService;
    private ChatListAdapter mAdapter;
    //蓝牙适配器
    private BluetoothAdapter mBtAdapter;

    private TextView connect_name;
    private TextView disConnecBlue;
    private View displayConnewBlueName;
    // 蓝牙客户端socket
    private BluetoothSocket mSocket;
    // 设备
    private BluetoothDevice mDevice;
    private ClientThread mClientThread;
    private ReadThread mReadThread;

    private String blueName;
    private boolean isConnectBlue=false;
    private TextView reture_btn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlueUtil.RE_CONNECT_BT = true;
        setContentView(R.layout.activity_devices);
        initDatas();
        removePairDevice();
        initViews();
        registerBroadcast();
        init();
    }
    //得到配对的设备列表，清除已配对的设备
    public void removePairDevice(){
        if(mBtAdapter!=null){
            Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
            for(BluetoothDevice device : bondedDevices ){
                unpairDevice(device);
            }
        }

    }
    //反射来调用BluetoothDevice.removeBond取消设备的配对
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {

        }
    }
    private void initDatas() {
        mDatas = new ArrayList<DeviceBean>();
        mAdapter = new ChatListAdapter(this, mDatas);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 列出所有的蓝牙设备
     */
    private void init() {
        // Log.i("tag", "mBtAdapter==" + mBtAdapter);
        //根据适配器得到所有的设备信息
        Set<BluetoothDevice> deviceSet = mBtAdapter.getBondedDevices();
        Log.i("tag", "deviceSet==" + deviceSet.size());
        if (deviceSet.size() > 0) {
            for (BluetoothDevice device : deviceSet) {
                mDatas.add(new DeviceBean(device.getName() + "\n" + device.getAddress(), true));
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(mDatas.size() - 1);
            }
        } else {
           /* mDatas.add(new DeviceBean("没有配对的设备", true));
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(mDatas.size() - 1);*/
        }
    }

    /**
     * 注册广播
     */
    private void registerBroadcast() {
        //设备被发现广播
        IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);//搜索发现设备
        discoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//状态改变
        discoveryFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//行动扫描模式改变了
        discoveryFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//动作状态发生了变化
        this.registerReceiver(mReceiver, discoveryFilter);

        // 设备发现完成
        IntentFilter foundFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, foundFilter);



    }

    /**
     * 初始化视图
     */

    private void initViews() {

        displayConnewBlueName = findViewById(R.id.display_connect);
        connect_name = ((TextView) findViewById(R.id.connect_blue_name));
        disConnecBlue =(TextView) findViewById(R.id.disconnectblue);
        if(ConstantUtil.CONNECT_BLUE_NAME==null||ConstantUtil.CONNECT_BLUE_NAME.equals("")){
            displayConnewBlueName.setVisibility(View.GONE);
        }else {
            displayConnewBlueName.setVisibility(View.GONE);
          /*  displayConnewBlueName.setVisibility(View.VISIBLE);
            connect_name.setText(ConstantUtil.CONNECT_BLUE_NAME);
            disConnecBlue.setOnClickListener(this);*/
        }

        reture_btn = ((TextView) findViewById(R.id.btn_return));
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setFastScrollEnabled(true);


        mListView.setOnItemClickListener(mDeviceClickListener);

        mBtnSearch = (Button) findViewById(R.id.start_seach);
        mBtnSearch.setOnClickListener(mSearchListener);
        reture_btn.setOnClickListener(this);

        mBtnService = (Button) findViewById(R.id.start_service);
        mBtnService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                BluetoothActivity.mType = BluetoothActivity.Type.SERVICE;
                BluetoothActivity.mTabHost.setCurrentTab(1);
            }
        });

    }


    /**
     * 搜索监听
     */
    private OnClickListener mSearchListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
                mBtnSearch.setText("重新搜索");
            } else {
                mDatas.clear();
                mAdapter.notifyDataSetChanged();

                init();

				/* 开始搜索 */
                mBtAdapter.startDiscovery();
                mBtnSearch.setText("停止搜索");
            }
        }
    };

    /**
     * 点击设备监听
     */
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            DeviceBean bean = mDatas.get(position);

            String info = bean.message;
            String address = info.substring(info.length() - 17);
            blueName = info.substring(0, info.length() - 17);


            BluetoothActivity.BlueToothAddress = address;

            AlertDialog.Builder stopDialog = new AlertDialog.Builder(DeviceActivity.this);
            stopDialog.setTitle("连接");//标题
            stopDialog.setMessage(bean.message);

            stopDialog.setPositiveButton("连接", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    mBtnSearch.setText("重新搜索");

                    BluetoothActivity.mType = BluetoothActivity.Type.CILENT;
                    //BluetoothActivity.mTabHost.setCurrentTab(1);
                    if (BluetoothActivity.mType == BluetoothActivity.Type.CILENT) {
                        final String address = BluetoothActivity.BlueToothAddress;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Toast.makeText(DeviceActivity.this, "正在连接蓝牙", Toast.LENGTH_SHORT).show();

                                mDevice = mBtAdapter.getRemoteDevice(address);

                                mClientThread = new ClientThread();
                                mClientThread.start();
                            }
                        });

                        /*if (!"".equals(address)) {
                            mDevice = mBtAdapter.getRemoteDevice(address);
                            mClientThread = new ClientThread();
                            mClientThread.start();
                            BluetoothActivity.isOpen = true;
                        } else {
                            Toast.makeText(DeviceActivity.this, "address is null !", Toast.LENGTH_SHORT).show();
                        }*/
                    }
                    dialog.cancel();
                }
            });
            stopDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    BluetoothActivity.BlueToothAddress = null;
                    dialog.cancel();
                }
            });
            stopDialog.show();
        }
    };

    /**
     * 发现设备广播
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BlueToothTestActivity", "BroadcastReceiver");
            String action = intent.getAction();
            BluetoothDevice device;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 获得设备信息
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // 如果绑定的状态不一样
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mDatas.add(new DeviceBean(device.getName() + "\n" + device.getAddress(), false));
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(mDatas.size() - 1);
                }
                // 如果搜索完成了
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                if (mListView.getCount() == 0) {
                    mDatas.add(new DeviceBean("没有发现蓝牙设备", false));
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(mDatas.size() - 1);
                }
                mBtnSearch.setText("重新搜索");
            }


            //状态改变时
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING://正在配对
                        Log.d("BlueToothTestActivity", "正在配对......");

                        break;

                    case BluetoothDevice.BOND_BONDED://配对结束
                        Log.d("BlueToothTestActivity", "完成配对");
                        shutdownClient();
                        Intent intent1 = new Intent(ConstantUtil.DIS_CONNECT_BLUE_ACTION);
                        intent1.putExtra("isconnect","connectblue");
                        intent1.putExtra("bluaddress",BluetoothActivity.BlueToothAddress);
                        context.sendBroadcast(intent1);
                        displayConnewBlueName.setVisibility(View.VISIBLE);
                        break;
                    case BluetoothDevice.BOND_NONE://取消配对/未配对
                        Log.d("BlueToothTestActivity", "取消配对");

                    default:
                        break;
                }
            }


        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
        BlueUtil.RE_CONNECT_BT = false;
        finish();
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.disconnectblue){

            isConnectBlue =false;
            disConnecBlue.setText("已断开连接");
            displayConnewBlueName.setVisibility(View.GONE);
            Intent intent = new Intent(ConstantUtil.DIS_CONNECT_BLUE_ACTION);
            intent.putExtra("isconnect","stop");
            sendBroadcast(intent);
        }
        if(v.getId()==R.id.btn_return){
            BlueUtil.RE_CONNECT_BT = false;
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private static final int STATUS_CONNECT = 0x11;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String info = (String) msg.obj;
            switch (msg.what) {
                case STATUS_CONNECT:
                    Toast.makeText(DeviceActivity.this, info, Toast.LENGTH_SHORT).show();
                    mDatas.add(new DeviceBean(info, false));
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(mDatas.size() - 1);
                    break;
                case 2:
                    displayConnewBlueName.setVisibility(View.VISIBLE);
                    connect_name.setText(blueName);
                    isConnectBlue=true;
                    ConstantUtil.CONNECT_BLUE_NAME=blueName;
                    disConnecBlue.setText("点击断开连接");
                    disConnecBlue.setOnClickListener(DeviceActivity.this);
                    shutdownClient();  //  b g i888hh9h9hh
                    Intent intent1 = new Intent(ConstantUtil.DIS_CONNECT_BLUE_ACTION);
                    intent1.putExtra("isconnect","con   nectblue");
                    intent1.putExtra("bluaddress",BluetoothActivity.BlueToothAddress);
                    sendBroadcast(intent1);
                    SPUtils.put(DeviceActivity.this,"blueaddress",BluetoothActivity.BlueToothAddress);
                    mDatas.add(new DeviceBean(info, false));
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(mDatas.size() - 1);
                    Toast.makeText(DeviceActivity.this, "请重启应用使用", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    // displayConnewBlueName.setVisibility(View.GONE);
                    break;
            }

           /* if (msg.what == 1) {
                mDatas.add(new DeviceBean(info, true));
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(mDatas.size() - 1);
            } else {

            }
            if(info.equals("已经连接上服务端！可以发送信息。")){


            }*/

        }

    };
    // 客户端线程
    private class ClientThread extends Thread {
        public void run() {
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Message msg = new Message();
                msg.obj = "请稍候，正在连接蓝牙:" + BluetoothActivity.BlueToothAddress;
                msg.what = STATUS_CONNECT;
                mHandler.sendMessage(msg);

                mSocket.connect();

                msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = 2;
                mHandler.sendMessage(msg);
                // 启动接受数据
                mReadThread = new ReadThread();
                mReadThread.start();
            } catch (IOException e) {
                Message msg = new Message();
                msg.obj = "连接蓝牙异常！断开连接重新试一试。";
                msg.what = 3;
                mHandler.sendMessage(msg);
            }
        }
    };
    /* ͣ停止客户端连接 */
    private void shutdownClient() {
        new Thread() {

            public void run() {
                mBtAdapter.cancelDiscovery();
                if (mReadThread != null) {
                    mReadThread.interrupt();
                    mReadThread = null;
                }
                if (mClientThread != null) {
                    mClientThread.interrupt();
                    mClientThread = null;
                }

                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mSocket = null;
                }
            };
        }.start();
    }
    // 读取数据
    private class ReadThread extends Thread {
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream is = null;
            try {
                if(mSocket==null)
                    return;
                is = mSocket.getInputStream();
                while (true) {
                    if ((bytes = is.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Message msg = new Message();
                        msg.obj = s;
                        msg.what = 1;
                        mHandler.sendMessage(msg);
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
}