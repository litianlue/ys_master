package com.yeespec.microscope.utils.wifidirect;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.yeespec.microscope.utils.SocketNetUtils;
import com.yeespec.microscope.utils.UIUtil;
import com.yeespec.microscope.utils.wifi.WifiConnect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A simple server socket that accepts connection and writes some data on
 * the stream.
 */
public class DataServerAsyncTask extends
        AsyncTask<Void, Void, String> {
    private final String MESSAGE_HEAD="message_head";
    private TextView statusText;
    private Context activity;
    private WifiP2pManager wifiP2pManager;
    private int PORT = 8500;
    /**
     * @param statusText
     * @param mManager
     */
    public DataServerAsyncTask(Context activity, View statusText, WifiP2pManager mManager) {
        this.statusText = (TextView) statusText;
        this.activity=activity;
        this.wifiP2pManager = mManager;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            Log.i("xyz", "data doinback");
            ServerSocket serverSocket = new ServerSocket(8888);

            Log.i("xyz","串口创建完成");
            Socket client = serverSocket.accept();
            Log.i("xyz","阻塞已取消");
            InputStream inputstream = client.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i;
            while ((i = inputstream.read()) != -1) {
                baos.write(i);
            }

            String str = baos.toString();
            serverSocket.close();
            return str;

        } catch (IOException e) {
            Log.e("xyz", e.toString());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(String result) {

        Log.i("xyz", "data onpost");

        if(!TextUtils.isEmpty(result)){
            String[] split = result.split("&");
            if(split[0].equals(MESSAGE_HEAD)){
                final String uuid  = split[1];
                final String password = split[2];
                final String ip = split[3];
                String ipAddress = SocketNetUtils.getIPAddress(activity);
                WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
                final WifiConnect wifiConnect = new WifiConnect(activity,wifiManager);
                wifiConnect.OpenWifi();
                WifiConnect.WifiCipherType  mconnectType = WifiConnect.WifiCipherType.WIFICIPHER_WEP;
                wifiConnect.mConnect(uuid, password, mconnectType, new WifiConnect.WifiConnectListener() {
                    @Override
                    public void OnWifiConnectCompleted(boolean isConnected) {
                        if(!isConnected){
                            WifiConnect.WifiCipherType connectType = WifiConnect.WifiCipherType.WIFICIPHER_WPA;
                            wifiConnect.mConnect(uuid, password, connectType, new WifiConnect.WifiConnectListener() {
                                @Override
                                public void OnWifiConnectCompleted(boolean isConnected) {
                                    if(isConnected){
                                        Log.w("xyz", "wifi已经链接到路由器");
                                        WifiDirectUtils.wlanConnectSoccel  = true;
                                        SocketNetUtils.connectServerWithTCPSocket(ip,PORT,"登录成功&"+SocketNetUtils.getIPAddress(activity));
                                    }else {
                                        WifiDirectUtils.wlanConnectSoccel  = false;
                                        Log.w("xyz", "wifi链接到路由器失败");
                                        SocketNetUtils.connectServerWithTCPSocket(ip,PORT,"登录失败，请确账号或密码是否正确");
                                    }
                                }
                            });
                        }else {
                            WifiDirectUtils.wlanConnectSoccel  = true;
                            Log.w("xyz", "wifi已经链接到路由器");
                            SocketNetUtils.connectServerWithTCPSocket(ip,PORT,"登录成功&"+SocketNetUtils.getIPAddress(activity));
                        }
                    }
                });
            }
        }
        //Toast.makeText(activity, " "+result, Toast.LENGTH_SHORT).show();

        if (result != null) {
            statusText.setText("Data-String is " + result);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute() {

    }

}