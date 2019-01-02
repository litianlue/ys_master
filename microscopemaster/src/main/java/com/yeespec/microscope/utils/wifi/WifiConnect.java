package com.yeespec.microscope.utils.wifi;

/**
 * Created by Administrator on 2017/12/11.
 */

/*
 *  WifiConnect.java
 *  Author: cscmaker
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static android.content.Context.CONNECTIVITY_SERVICE;


public class WifiConnect {
    private WifiConnectListener mWifiConnectListener;
    //通知连接结果的监听接口
    public interface WifiConnectListener {
        public void OnWifiConnectCompleted(boolean isConnected);
    }

    WifiManager wifiManager;
    private  Context context;
    //定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }

    //构造函数
    public WifiConnect(Context context,WifiManager wifiManager,WifiConnectListener listener) {
        this.wifiManager = wifiManager;
        this.context = context;
        this.mWifiConnectListener = listener;
    }
    public WifiConnect(Context context,WifiManager wifiManager) {
        this.wifiManager = wifiManager;
        this.context = context;

    }
    //打开wifi功能
    public boolean OpenWifi() {
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }

        return bRet;
    }
    //是否已经链接wifi
    public boolean isConnectWifi(){
        boolean bConnect = false;
        ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            bConnect = true;
        }else {
            bConnect = false;
        }
        return  bConnect;
    }
    public  void  mConnect(final String SSID, final String Password, final WifiCipherType Type, final WifiConnectListener listener){

        mWifiConnectListener = listener;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!OpenWifi()) {
                    mWifiConnectListener.OnWifiConnectCompleted(false);
                }
                //开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi
                //状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
                while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                    try {
                        //为了避免程序一直while循环，让它睡个100毫秒在检测……
                        Thread.currentThread();
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                    }
                }

                WifiConfiguration wifiConfig = CreateWifiInfo(SSID, Password, Type);
                //
                if (wifiConfig == null) {
                    mWifiConnectListener.OnWifiConnectCompleted(false);
                }

                WifiConfiguration tempConfig = IsExsits(SSID);

                if (tempConfig != null) {
                    wifiManager.removeNetwork(tempConfig.networkId);
                }

                int netID = wifiManager.addNetwork(wifiConfig);
                boolean bRet = wifiManager.enableNetwork(netID, true);
                WifiInfo info = wifiManager.getConnectionInfo();
                try {
                    Thread.sleep(2000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int delaycount = 0;//
                while (!isWifiEnabled(context)&&delaycount < 8) {
                    try {
                        delaycount++;
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


                Log.w("WifiConnect", "info.getSSID=" + info.getSSID() + " SSID=" + SSID);
                Log.w("WifiConnect", "info.getSupplicantState()=" + info.getSupplicantState() + " SupplicantState.COMPLETED=" + SupplicantState.COMPLETED);
                if (isWifiEnabled(context)) {
                    mWifiConnectListener.OnWifiConnectCompleted(true);
                } else
                    mWifiConnectListener.OnWifiConnectCompleted(false);
            }
        }).start();

    }

    //提供一个外部接口，传入要连接的无线网
    public boolean Connect(String SSID, String Password, WifiCipherType Type) {

        if (!this.OpenWifi()) {
            return false;
        }
        //开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi
        //状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
        while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            try {
                //为了避免程序一直while循环，让它睡个100毫秒在检测……
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }

        WifiConfiguration wifiConfig = this.CreateWifiInfo(SSID, Password, Type);
        //
        if (wifiConfig == null) {
            return false;
        }

        WifiConfiguration tempConfig = this.IsExsits(SSID);

        if (tempConfig != null) {
            wifiManager.removeNetwork(tempConfig.networkId);
        }

        int netID = wifiManager.addNetwork(wifiConfig);
        boolean bRet = wifiManager.enableNetwork(netID, true);
        WifiInfo info = wifiManager.getConnectionInfo();
        try {
            Thread.sleep(2000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int delaycount = 0;//
        while (!isWifiEnabled(context)&&delaycount < 10) {
            try {
                delaycount++;
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        Log.w("WifiConnect", "info.getSSID=" + info.getSSID() + " SSID=" + SSID);
        Log.w("WifiConnect", "info.getSupplicantState()=" + info.getSupplicantState() + " SupplicantState.COMPLETED=" + SupplicantState.COMPLETED);
        if (isWifiEnabled(context)) {
            return true;
        } else
            return false;
    }
    public static boolean isWifiEnabled(Context context) {
        ConnectivityManager mgrConn = (ConnectivityManager) context
                .getSystemService(CONNECTIVITY_SERVICE);
        TelephonyManager mgrTel = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return ((mgrConn.getActiveNetworkInfo() != null && mgrConn
                .getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel
                .getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);
    }


    //查看以前是否也配置过这个网络
    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private WifiConfiguration CreateWifiInfo(String SSID, String Password, WifiCipherType Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WifiCipherType.WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            config.preSharedKey="\""+Password+"\"";
            config.hiddenSSID = true;
            config.status = WifiConfiguration.Status.ENABLED;
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        }
        return config;
    }


    /*连接到热点*/
    public void connectToHotpot(String uuid,String password){
        if(uuid==null)
            return;
        WifiConfiguration wifiConfig=this.setWifiParams(uuid,password);
        int wcgID = wifiManager.addNetwork(wifiConfig);
        boolean flag=wifiManager.enableNetwork(wcgID, true);
        System.out.println("connect success? "+flag);
    }

    /*设置要连接的热点的参数*/
    public WifiConfiguration setWifiParams(String ssid,String password){
        WifiConfiguration apConfig=new WifiConfiguration();
        apConfig.SSID="\""+ssid+"\"";
        apConfig.preSharedKey="\""+password+"\"";
        apConfig.hiddenSSID = true;
        apConfig.status = WifiConfiguration.Status.ENABLED;
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return apConfig;
    }

}
