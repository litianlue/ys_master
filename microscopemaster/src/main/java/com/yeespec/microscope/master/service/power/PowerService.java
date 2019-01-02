package com.yeespec.microscope.master.service.power;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import com.yeespec.R;
import com.yeespec.microscope.master.activity.MasterActivity;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: mraviteja
 * Date: 8/6/14
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class PowerService extends Service {



    private static final String TAG = "PowerService";

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int level = -1;
            if (currentLevel >= 0 && scale > 0) {
                level = (currentLevel * 100) / scale;
            }
            showNotification(context, level);
        }

    };

    public PendingIntent contentIntent;
    public Notification notification = new Notification(R.mipmap.one,
            "Battery Manager Service Started.", System.currentTimeMillis());
    PowerService ps;

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);

        //        return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    private void showForegroundNotification() {
        //2016.09.20 : 添加startForeground将进程设置为前台进程 ;
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(this, MasterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notify = new Notification.Builder(this)
                .setAutoCancel(false)
                .setTicker(getResources().getString(R.string.ticker_notify))
                .setContentTitle(getResources().getString(R.string.title_notify))
                .setContentText(getResources().getString(R.string.text_notify))
//                .setSmallIcon(R.mipmap.ic_microscope_cells_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pIntent)
                .build();

        startForeground(new Random().nextInt(100), notify);
        Log.w("test", TAG + " # showForegroundNotification() ");
    }

    @Override
    public void onCreate() {
//        //2016.09.20 : 添加startForeground将进程设置为前台进程 ;
//        showForegroundNotification();

        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onStart(final Intent intent, int startid) {

    }

    @Override
    public void onDestroy() {
        if(mBatInfoReceiver!=null) {
            unregisterReceiver(this.mBatInfoReceiver);
        }
       stopForeground(true);
    }

    private void showNotification(Context context, int level) {
        String l = Integer.toString(level);
        String buttonID = "cs" + l;
        int resID = getResources().getIdentifier(buttonID, "drawable", "com.example.battery_status");


        notification = new Notification(resID, "Battery Manager Service Started", System.currentTimeMillis());

        contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MasterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        ps = this;
        notification.setLatestEventInfo(this, "Battery Manager", "Battery charge level " + level + "%", contentIntent);

        startForeground(1, notification);

    }


}