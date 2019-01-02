package com.yeespec.microscope.utils.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;


import com.yeespec.microscope.utils.SocketNetUtils;
import com.yeespec.microscope.utils.wifi.WiFiUtil;
import com.yeespec.microscope.utils.wifi.WifiConnect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static com.yeespec.microscope.utils.wifidirect.WifiDirectUtils.ConnectWifi;
import static com.yeespec.microscope.utils.wifidirect.WifiDirectUtils.DiscoverDelay;
import static com.yeespec.microscope.utils.wifidirect.WifiDirectUtils.canConnect;
import static com.yeespec.microscope.utils.wifidirect.WifiDirectUtils.isconnect;
import static com.yeespec.microscope.utils.wifidirect.WifiDirectUtils.peersshow;

/**
 * Created by Administrator on 2018/1/22.
 */

public class DataThread extends Thread {
    private Activity activity;
    private Handler handler;
    private  Socket client;
    private BufferedReader in = null;
    private PrintWriter out = null;
    private final String MESSAGE_HEAD="message_head";
    private int PORT = 8500;
    private WiFiUtil wiFiUtil;
    private int connectWifiDelay =80;
    private WifiP2pManager pManager;
    private WifiP2pManager.Channel mChannel;
    public DataThread(Activity activity, WifiP2pManager mManager, WifiP2pManager.Channel mChannel){
        this.activity = activity;
        wiFiUtil = WiFiUtil.getInstance(activity);
        this.pManager = mManager;
        this.mChannel = mChannel;
    }

    @Override
    public void run() {

        ServerSocket serverSocket = null;
        try {

            serverSocket = new ServerSocket(8888);
            serverSocket.setSoTimeout(120*1000);
            Log.i("xyz","串口创建完成");
            client = serverSocket.accept();

            out = new PrintWriter(client.getOutputStream());
            Log.i("xyz","串口创建完成=");
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            String line = in.readLine();
            Log.w("xyz", "串口创建完成==");
            while(!"bye".equals(line)){

                System.out.println("client:"+line);
              /*  out.println("服务端应答");
                out.flush();*/
                Log.w("xyz", "serverSocket line="+line);
                if(line==null)
                    break;

                connecWifi(line);
                if(client.isConnected())
                line = in.readLine();
            }

            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    public void connecWifi(final String line) {
        ConnectWifi = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
                if(!TextUtils.isEmpty(line)){
                    String[] split = line.split("&");
                    Log.w("xyz", "head="+split[0]);
                    if(split[0].equals(MESSAGE_HEAD)){
                        final String uuid  = split[1];
                        final String password = split[2];
                        final String ip = split[3];
                        final String type = split[4]; // 0:SECURITY_NONE  1：SECURITY_WEP  2：SECURITY_PSK  3：SECURITY_EAP
                        int  t = Integer.valueOf(type);
                        Log.w("xyz", "t="+t);
                        Log.w("xyz", "uuid="+uuid);
                        Log.w("xyz", "password="+password);

                        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
                        final WifiConnect wifiConnect = new WifiConnect(activity,wifiManager);
                        wifiConnect.OpenWifi();
                        int state =0;
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
                        while (connectWifiDelay>0){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (wifiManager.isWifiEnabled()&&SocketNetUtils.getIPAddress(activity) != null) {
                                Log.w("xyz", "wifi已经链接到路由器");
                                WifiDirectUtils.wlanConnectSoccel  = true;
                                SocketNetUtils.connectServerWithTCPSocket(ip,PORT,"登录成功&"+ SocketNetUtils.getIPAddress(activity));
                                //serverSendString("登录成功&"+SocketNetUtils.getIPAddress(activity));
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
                        isconnect = true;
                        peersshow.clear();
                        WifiDirectUtils.wlanConnectSoccel  = false;
                        Log.w("xyz", "wifi链接到路由器失败");
                        //serverSendString("登录失败，请确账号或密码是否正确");
                        SocketNetUtils.connectServerWithTCPSocket(ip,PORT,"登录失败，请确账号或密码是否正确");
                        pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onFailure(int reason) {

                            }
                        });
                        canConnect = true;
                        ConnectWifi = false;
                    }
                }
            }
        }).start();

    }

    public void serverSendString(String str){
        if(!client.isConnected()){
            Log.w("xyz", "!client.isConnected()="+str);
        }
        if(out!=null){
            Log.w("xyz", "serverSendString="+str);
            out.println(str);
            out.flush();
        }
    }
}
