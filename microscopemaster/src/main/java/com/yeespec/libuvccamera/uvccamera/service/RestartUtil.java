package com.yeespec.libuvccamera.uvccamera.service;


import android.content.Context;
import android.content.Intent;
import android.os.Process;


import com.orhanobut.hawk.Hawk;
import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.utils.FileUtils;
import java.util.concurrent.TimeUnit;

import static com.yeespec.libuvccamera.uvccamera.service.UVCService.RESTART_SERVICE_SCHEDULED;


public class RestartUtil {

    public void exceptionKillMethod(Context context,int processId) {
        android.os.Process.killProcess(processId);
        Intent LaunchIntent =context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        LaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(LaunchIntent);
        Process.killProcess(Process.myPid());
        System.gc();
        System.exit(0);
    }
    public  long lastClickTime;
    public  boolean isFastDoubleClick(int delaytimer) {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if ( 0 < timeD && timeD < delaytimer) {
            return true;
        }
        lastClickTime = time;
        return false;
    }
    public String killProcess() {
        Boolean exception_count = Hawk.get("exception_count", false);
        FileUtils.writeFileToLogFolder("killProcess，========"+exception_count);

        if(exception_count) {
            Hawk.put("exception_count", false);
            Hawk.put("remote_exception", true);
            RESTART_SERVICE_SCHEDULED.schedule(new Runnable() {
                @Override
                public void run() {

                    FileUtils.writeFileToLogFolder("killProcess==========");
                    BaseApplication.getInstance().destroy();
//                Intent LaunchIntent = BaseApplication.getContext().getPackageManager().getLaunchIntentForPackage(BaseApplication.getContext().getPackageName());
//                LaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                BaseApplication.getContext().startActivity(LaunchIntent);
//                ActivityManager am = (ActivityManager)BaseApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
//                List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
//                for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
//                    Log.w("mytestprocess", "processname=" + procInfo.processName);
//                    Log.w("mytestprocess", "processid=" + procInfo.pid);
//                    Process.killProcess(procInfo.pid);
//                    am.restartPackage(procInfo.processName);
//                }
//                System.gc();
//                System.exit(0);
                }
            }, 1500, TimeUnit.MILLISECONDS);
        }
        return null;
    }
}
