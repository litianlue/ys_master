package com.yeespec.microscope.utils.wifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import com.yeespec.microscope.master.activity.MasterActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by zd on 2016/3/20.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Activity mActivity;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mInfoListener;
    private boolean isconnect = false;
    private ScheduledExecutorService connectDelay = Executors.newScheduledThreadPool(1);
    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Activity activity,
                                       WifiP2pManager.PeerListListener peerListListener,
                                       WifiP2pManager.ConnectionInfoListener infoListener
    ) {
        this.mManager = manager;
        this.mChannel = channel;
        this.mPeerListListener = peerListListener;
        this.mActivity = activity;
        this.mInfoListener = infoListener;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        String action = intent.getAction();

        /*check if the wifi is enable*/
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        }

        /*get the list*/
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            mManager.requestPeers(mChannel, mPeerListListener);
        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            int State = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
            if (State == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                Toast.makeText(mActivity, "搜索开启", Toast.LENGTH_SHORT).show();
            }
            else if (State == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                Toast.makeText(mActivity, "搜索已关闭", Toast.LENGTH_SHORT).show();
            }

        }
        /*Respond to new connection or disconnections*/
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                Toast.makeText(mActivity, "已经链接", Toast.LENGTH_SHORT).show();
                isconnect = true;
                mManager.requestConnectionInfo(mChannel, mInfoListener);
            } else {
                if(isconnect){
                    isconnect = false;
                    connectDelay.schedule(new Runnable() {
                        @Override
                        public void run() {

                           WifiDirectUtils.isconnect = true;
                          /*  mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                }

                                @Override
                                public void onFailure(int reason) {

                                }
                            });*/
                            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onFailure(int reason) {

                                }
                            });
                        }
                    },5000, TimeUnit.MILLISECONDS);
                   // Toast.makeText(mActivity, "重新链接链接", Toast.LENGTH_SHORT).show();
                }
                //Toast.makeText(mActivity, "断开链接", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        /*Respond to this device's wifi state changing*/
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

        }
    }
}
