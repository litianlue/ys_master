package com.yeespec.microscope.master.receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.yeespec.microscope.master.activity.SplashActivity;

import java.util.List;

/**
 * Created by yuchunrong on 2017-10-10.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String action_boot="android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(final Context context, Intent intent) {

       /* if (intent.getAction().equals(action_boot)){
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    //if(isBackground(context)){

                        Intent ootStartIntent=new Intent(context,SplashActivity.class);
                        ootStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(ootStartIntent);
                  //  }
                }
            }, 1*1000);
          *//*  Intent ootStartIntent=new Intent(context,SplashActivity.class);
            ootStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(ootStartIntent);*//*
        }*/
    }
    private   boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {

                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    Log.i(context.getPackageName(), "处于后台"
                            + appProcess.processName);
                    return true;
                } else {
                    Log.i(context.getPackageName(), "处于前台"
                            + appProcess.processName);
                    return false;
                }
            }
        }
        return false;
    }
}
