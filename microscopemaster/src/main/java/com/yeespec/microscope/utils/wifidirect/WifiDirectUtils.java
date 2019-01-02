package com.yeespec.microscope.utils.wifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.yeespec.microscope.utils.ConstantUtil;
import com.yeespec.microscope.utils.SocketNetUtils;
import com.yeespec.microscope.utils.UIUtil;
import com.yeespec.microscope.utils.wifi.WiFiUtil;
import com.yeespec.microscope.utils.wifi.WifiConnect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static android.content.Context.WIFI_P2P_SERVICE;

/**
 * Created by Administrator on 2018/1/19.
 */

public class WifiDirectUtils {
    private IntentFilter mFilter;
    private Activity context;
    public static List<HashMap<String, String>> peersshow = new ArrayList();

    private MyAdapter mAdapter;
    public List peers = new ArrayList();
    private BroadcastReceiver mReceiver;
    private WifiP2pInfo info;
    public static boolean isconnect = true;


    private ScheduledExecutorService connectDelay = Executors.newScheduledThreadPool(3);
    public static ScheduledExecutorService DiscoverDelay = Executors.newScheduledThreadPool(2);
    private final int READ_DATA = 1;
    public static boolean wlanConnectSoccel = false;

    private int port = 8888;
    private String host;
    private Socket socket;
    private DataThread dataThread;
    private WiFiUtil wiFiUtil;
    private  WifiManager wifiManager;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    public  static   boolean ConnectWifi = false;//是否正在链接wifi
    public static   boolean canConnect  = true;
    /* private DISCOVER_LISTNESS discover_listness;
     public interface DISCOVER_LISTNESS {

     };*/
    public WifiDirectUtils(Activity context) {

        this.context = context;
        wiFiUtil = WiFiUtil.getInstance(context);

    }

    public void startWifiDirect() {


        //打开wifi
         wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        initIntentFilter();
        initReceiver();
        DiscoverDelay.schedule(new Runnable() {
            @Override
            public void run() {
                DiscoverPeers();
            }
        }, 3000, TimeUnit.MILLISECONDS);

    }

    //搜索
    public void DiscoverPeers() {
        Log.i("xyz123", "DiscoverPeers");
        isconnect = true;
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //UIUtil.toast(context,"搜索成功",true);
            }

            @Override
            public void onFailure(int reason) {
                // UIUtil.toast(context,"搜索失败",true);
            }
        });
        DiscoverDelay.schedule(new Runnable() {
            @Override
            public void run() {
                if (peersshow.size() < 1) {
                    DiscoverPeers();
                } /*else {

                    CreateConnect(peersshow.get(0).get("address"),
                            peersshow.get(0).get("name"));
                }*/
            }
        }, 15000, TimeUnit.MILLISECONDS);
    }

    //停止搜索
    public void StopDiscoverPeers() {

        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {


            }
        });
    }

    //停止链接
    public void StopConnect() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    private void sendString(final String str) {
        connectDelay.schedule(new Runnable() {
            @Override
            public void run() {
                if(!socket.isConnected()){
                    Log.i("xyz","!socket.isConnected()");
                    try {
                        socket.close();
                        socket =null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                if (socket == null) {
                    try {
                        host = info.groupOwnerAddress.getHostAddress();
                        socket = new Socket(host, port);
                        socket.setSoTimeout(120 * 10000);
                    } catch (IOException e) {

                    }
                }
                Log.i("xyz", "sendString=" + str);
                try {
                    BufferedReader in;
                    BufferedReader input;
                    PrintWriter out;
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream());
                    String line = str;
                    Log.i("xyz", "line=" + line);
                    if (!"bye".equals(line)) {
                        System.out.println("line:" + line);
                        out.println(line);
                        out.flush();
                        String echo = in.readLine();
                        connecWifi(echo);

                        Log.i("xyz", "echo=" + echo);

                    }
                } catch (Exception e) {
                }
            }
        }, 500, TimeUnit.MILLISECONDS);

    }

    public void initIntentFilter() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mFilter.setPriority(1000);
    }

    public void initReceiver() {
        Log.i("xyz123", "initReceiver");
        mManager = (WifiP2pManager) context.getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, Looper.myLooper(), null);

        WifiP2pManager.PeerListListener mPeerListListerner = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peersList) {
                Log.i("xyz123", "mPeerListListerner is on");

                peers.clear();
                peersshow.clear();
                Collection<WifiP2pDevice> aList = peersList.getDeviceList();
                peers.addAll(aList);

                for (int i = 0; i < aList.size(); i++) {
                    WifiP2pDevice a = (WifiP2pDevice) peers.get(i);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("name", a.deviceName);
                    map.put("address", a.deviceAddress);
                    peersshow.add(map);
                }

                Log.i("xyz123", "peersshow.size()=" + peersshow.size() + " isconnect=" + isconnect);
                if(peersshow.size()>0&&isconnect) {

                    if(canConnect) {
                        UIUtil.toast(context, "链接到=" + peersshow.get(0).get("name"), true);
                        if (isconnect) {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    CreateConnect(peersshow.get(0).get("address"),
                                            peersshow.get(0).get("name"));
                                }
                            });
                        }
                        DiscoverDelay.schedule(new Runnable() {
                            @Override
                            public void run() {
                                if(!ConnectWifi)
                                canConnect = true;
                            }
                        },35000,TimeUnit.MILLISECONDS);

                        canConnect = false;
                    }


                }
                mAdapter = new MyAdapter(peersshow);

                mAdapter.SetOnItemClickListener(new MyAdapter.OnItemClickListener() {
                    @Override
                    public void OnItemClick(View view, int position) {
                        // CreateConnect(peersshow.get(position).get("address"),
                        //       peersshow.get(position).get("name"));

                    }

                    @Override
                    public void OnItemLongClick(View view, int position) {

                    }
                });
            }
        };

        WifiP2pManager.ConnectionInfoListener mInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo minfo) {

                Log.i("xyz123", "InfoAvailable is on");
                info = minfo;
                TextView view = new TextView(context);
                if (info.groupFormed && info.isGroupOwner) {
                    UIUtil.toast(context, "isGroupOwner", true);
                    dataThread = new DataThread(context,mManager,mChannel);
                    dataThread.start();
                    isconnect = false;

                    canConnect =false;


                } else if (info.groupFormed) {
                    UIUtil.toast(context, "groupFormed", true);
                    isconnect = false;

                    canConnect =false;

                    //  sendString("已经链接上服务端");
                    connectDelay.schedule(new Runnable() {
                        @Override
                        public void run() {

                            if (socket == null) {
                                try {
                                    host = info.groupOwnerAddress.getHostAddress();
                                    socket = new Socket(host, port);
                                    socket.setSoTimeout(120 * 10000);
                                } catch (IOException e) {

                                }
                            }
                            Log.i("xyz", "sendString=" + "已经链接上服务端");
                            try {
                                BufferedReader in;
                                BufferedReader input;
                                PrintWriter out;
                                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                out = new PrintWriter(socket.getOutputStream());
                                String line = "已经链接上服务端";
                                Log.i("xyz", "line=" + line);
                                if (!"bye".equals(line)) {
                                    System.out.println("line:" + line);
                                    out.println(line);
                                    out.flush();
                                    String echo = in.readLine();
                                    connecWifi(echo);

                                    Log.i("xyz", "echo=" + echo);

                                }
                            } catch (Exception e) {
                            }
                        }
                    }, 500, TimeUnit.MILLISECONDS);
                }
            }
        };

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, context, mPeerListListerner, mInfoListener);
        context.registerReceiver(mReceiver, mFilter);
    }


    private void CreateConnect(final String address, final String name) {



        WifiP2pDevice device;
        WifiP2pConfig config = new WifiP2pConfig();
        Log.i("xyz", name);

        config.deviceAddress = address;
        /*mac地址*/

        config.wps.setup = WpsInfo.PBC;
        Log.i("address", "MAC IS " + address);
        if (address.equals("9a:ff:d0:23:85:97")) {
            config.groupOwnerIntent = 0;
            Log.i("address", "lingyige shisun");
        }
        if (address.equals("36:80:b3:e8:69:a6")) {
            config.groupOwnerIntent = 15;
            Log.i("address", "lingyigeshiwo");

        }

        Log.i("address", "lingyige youxianji" + String.valueOf(config.groupOwnerIntent));

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {


            }
        });

    }

    public void unregistReciver() {
        StopDiscoverPeers();
        StopConnect();
        if (mReceiver != null) {
            context.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if(wifiManager!=null){
            wifiManager = null;
            mManager = null;
            mChannel = null;
        }
        if(socket!=null){
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    private final String MESSAGE_HEAD = "message_head";
    private int connectWifiDelay = 80;

    public void connecWifi(String line) {
        StopDiscoverPeers();
        ConnectWifi = true;
        if (!TextUtils.isEmpty(line)) {
            String[] split = line.split("&");
            Log.w("xyz", "head=" + split[0]);
            if (split[0].equals(MESSAGE_HEAD)) {

                final String uuid = split[1].trim();
                final String password = split[2].trim();
                final String ip = split[3];
                final String type = split[4]; // 0:SECURITY_NONE  1：SECURITY_WEP  2：SECURITY_PSK  3：SECURITY_EAP
                int t = Integer.valueOf(type);

                Log.w("xyz", "t=" + t);
                Log.w("xyz", "uuid=" + uuid);
                Log.w("xyz", "password=" + password);
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final WifiConnect wifiConnect = new WifiConnect(context, wifiManager);
                wifiConnect.OpenWifi();
                WifiConnect.WifiCipherType mconnectType = null;
                int state = 0;
                switch (t) {
                    case 0:
                        state = wiFiUtil.addWiFiNetwork(uuid, password, WiFiUtil.Data.WIFI_CIPHER_WPA2);
                        break;
                    case 1:
                        state = wiFiUtil.addWiFiNetwork(uuid, password, WiFiUtil.Data.WIFI_CIPHER_WPA);
                        break;
                    case 2:
                        state = wiFiUtil.addWiFiNetwork(uuid, password, WiFiUtil.Data.WIFI_CIPHER_WEP);
                        break;
                    case 3:
                        state = wiFiUtil.addWiFiNetwork(uuid, "", WiFiUtil.Data.WIFI_CIPHER_NOPASS);
                        break;
                }
                while (connectWifiDelay > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (wifiManager.isWifiEnabled()&&SocketNetUtils.getIPAddress(context) != null) {
                        Log.w("xyz", "wifi已经链接到路由器");
                        WifiDirectUtils.wlanConnectSoccel = true;

                        sendString("登录成功&" + SocketNetUtils.getIPAddress(context));
                        return;
                    }
                    if(connectWifiDelay==45){
                        wifiManager.setWifiEnabled(false);
                    }
                    if(connectWifiDelay==42){
                        wifiManager.setWifiEnabled(true);
                    }

                    connectWifiDelay--;
                }
                connectWifiDelay=80;
                DiscoverPeers();
                WifiDirectUtils.wlanConnectSoccel = false;
                Log.w("xyz", "wifi链接到路由器失败");
                sendString("登录失败，请确账号或密码是否正确");

                canConnect = true;
                ConnectWifi = false;
            }
        }
    }
}
