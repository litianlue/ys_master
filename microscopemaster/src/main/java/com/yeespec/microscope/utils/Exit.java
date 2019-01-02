package com.yeespec.microscope.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;

import com.yeespec.R;
import com.yeespec.microscope.master.activity.UsbSerialActivity;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mr.Wen on 2016/11/30.
 */

public class Exit extends Application {
    private static Exit instance;
    private List<Activity> activityList = new LinkedList();
    private Context gContext = null;

    private Exit(Context paramContext) {
        this.gContext = paramContext;
    }

    public static Exit getInstance(Context paramContext) {
        if (instance == null)
            instance = new Exit(paramContext);
        return instance;
    }

    public void addActivity(Activity paramActivity) {
        this.activityList.add(paramActivity);
    }

    public void close() {
        getInstance(this.gContext).exit();
    }

    public void close(Context paramContext) {
        Comm.showDiaog(new AlertDialog.Builder(paramContext).setIcon(R.drawable._48_exit).setTitle(R.string.isExit).setNegativeButton(R.string.isCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                paramAnonymousDialogInterface.dismiss();
            }
        }).setPositiveButton(R.string.isOk, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                paramAnonymousDialogInterface.dismiss();
                Exit.getInstance(Exit.this.gContext).exit();
            }
        }).create());
    }

    public void exit() {
//        Iterator localIterator = this.activityList.iterator();
//        while (true) {
//            if (!localIterator.hasNext())
//                return;
//            Activity localActivity = (Activity) localIterator.next();
//            if (localActivity.getLocalClassName().equals("Main"))
//                Main.isRunBackground = false;
//            localActivity.finish();
//        }
        while (activityList.iterator().hasNext()) {
            Activity activity = (Activity) activityList.iterator().next();
            if (activity.getLocalClassName().equals("Main")) {
                UsbSerialActivity.isRunBackground = false;
            }
            activity.finish();
        }
    }

   /* public Activity getLastActivity() {
        if ((this.activityList == null) || (this.activityList.size() == 0))
            return null;
        return (Activity) this.activityList.get(this.activityList.size() - 1);
    }
*/
    /*public boolean isRun(boolean paramBoolean) {
        if (Comm.isDoubleClick(this.gContext, 1000, paramBoolean))
            getInstance(this.gContext).close();
        return false;
    }*/
}
