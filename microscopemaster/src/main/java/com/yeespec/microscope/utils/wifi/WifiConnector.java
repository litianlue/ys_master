package com.yeespec.microscope.utils.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by virgilyan on 15/11/13.
 */
public class WifiConnector {
    private static final int WIFI_CONNECT_TIMEOUT = 20; //连接WIFI的超时时间
    private Context mContext;
    private WifiManager mWifiManager;
    private Lock mLock;
    private Condition mCondition;
    private WiFiConnectReceiver mWifiConnectReceiver;
    private WifiConnectListener mWifiConnectListener;
    private boolean mIsConnected = false;
    private int mNetworkID = -1;

    //网络加密模式
    public static enum SecurityMode {
        OPEN, WEP, WPA, WPA2
    }

    //通知连接结果的监听接口
    public interface WifiConnectListener {
        public void OnWifiConnectCompleted(boolean isConnected);
    }

    public WifiConnector(Context context, WifiConnectListener listener) {
        mContext = context;
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiConnectReceiver = new WiFiConnectReceiver();
        mWifiConnectListener = listener;
    }
    public void disconnect(){
        if(isRegist) {
            mContext.unregisterReceiver(mWifiConnectReceiver);
            isRegist = false;
        }
    }
    private boolean isRegist=false;
    public void connect(final String ssid, final String password, final SecurityMode mode) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                //如果WIFI没有打开，则打开WIFI
                if (!mWifiManager.isWifiEnabled()) {
                    mWifiManager.setWifiEnabled(true);
                }
                isRegist = true;
                //注册连接结果监听对象
                mContext.registerReceiver(mWifiConnectReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                //连接指定SSID
                if (!onConnect(ssid, password, mode)) {
                    mWifiConnectListener.OnWifiConnectCompleted(false);
                } else {
                    mWifiConnectListener.OnWifiConnectCompleted(true);
                }
                //删除注册的监听类对象
                if(isRegist) {
                    mContext.unregisterReceiver(mWifiConnectReceiver);
                    isRegist = false;
                }
              //  mContext.unregisterReceiver(mWifiConnectReceiver);
            }
        }).start();
    }

    protected boolean onConnect(String ssid, String password, SecurityMode mode) {

        //添加新的网络配置
        WifiConfiguration cfg = new WifiConfiguration();
        cfg.SSID = "\"" + ssid + "\"";
        if (password != null && !"".equals(password)) {
            //这里比较关键，如果是WEP加密方式的网络，密码需要放到cfg.wepKeys[0]里面
            if (mode == SecurityMode.WEP) {
                cfg.wepKeys[0] = "\"" + password + "\"";
                cfg.wepTxKeyIndex = 0;

                cfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                cfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            } else if(mode==SecurityMode.WPA){

                cfg.preSharedKey = "\"" + password + "\"";
                cfg.hiddenSSID = true;
                cfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                cfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                cfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                // 此处需要修改否则不能自动重联
                // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                cfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                cfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                cfg.status = WifiConfiguration.Status.ENABLED;
            }else {
                cfg.preSharedKey = "\"" + password + "\"";
            }
        }
        cfg.status = WifiConfiguration.Status.ENABLED;
        //添加网络配置
        mNetworkID = mWifiManager.addNetwork(cfg);

        mLock.lock();
        mIsConnected = false;
        //连接该网络
        if (!mWifiManager.enableNetwork(mNetworkID, true)) {
            mLock.unlock();
            return false;
        }
        try {
            //等待连接结果
            mCondition.await(WIFI_CONNECT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mLock.unlock();
        return mIsConnected;
    }

    //监听系统的WIFI连接消息
    protected class WiFiConnectReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            mLock.lock();
            WifiInfo info = mWifiManager.getConnectionInfo();
            if (info.getNetworkId() == mNetworkID && info.getSupplicantState() == SupplicantState.COMPLETED) {
                mIsConnected = true;
                mCondition.signalAll();
            }
            mLock.unlock();
        }
    }

}
