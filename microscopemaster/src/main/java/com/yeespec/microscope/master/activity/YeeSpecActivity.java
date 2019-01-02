package com.yeespec.microscope.master.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.yeespec.R;
import com.yeespec.microscope.master.BaseActivity;
import com.yeespec.microscope.utils.ConstantUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mr.Wen on 2017/3/30.
 */

public class YeeSpecActivity extends BaseActivity {
    private Timer timer;
    private TimerTask timerTask;
    private int constantCount = 0;//重新加载次数
    private  boolean fisttest=true;
    private  boolean twotest=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        fisttest = isBackground(this);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                timer.cancel();
                Intent intent = new Intent(YeeSpecActivity.this, MasterActivity.class);
                startActivity(intent);

                twotest = isBackground(YeeSpecActivity.this);
                if(!fisttest&&!twotest){
                    ConstantUtil.isfirst_run = true;
                }
                finish();
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 3000, 3000);
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
//��������������������������������������������������������������������������������������    ~i-�i�F-�n��FF:~F-�n-y�:-�~F�ni:Fy�i-���o$o$n~�yi��y�θi~~��-���o$n~�yi��y�θ-חF���F��o$n~�yi��y�θ�:��V���F�o$o$n~�y-�n��FF:~F-�p�o$o$n~�y�i�i�Vָ_nFy�o$n~�y�i�i�Vָ_nFy_i:Ћo$o$�^^o$^�yFiF�� y�bF�ח�B� �T�TB�o$^�o$o$~V�--�i:: ��-��FcF��:�-��\o$~y�iF_nFynFy�o$~y�iF_nFy_i:�nFy_i:Ћo$�	�Fyy�Fo$~y�F-F����ח�yFiFN�V���F:i�F��:i�-F�iFM\o$o$:V~Fy�ח�yFiFN:i�F��:i�-F�iFM�o$o$:F�חF�F�Np��i��V�i-��}:~�i:CM�o$o$o$nFy_i:���F�_nFy_i:�NM\o$�	�Fyy�Fo$~V�-���yV�NM\o$nFy�-i�-F�NM�o$�F��F���F��F�N ��-���C:` i:Fy�-���-�i::M�o$:iy�-��N�F�M�o$��:CNM�o$/o$/�o$nFy��F�_nFyNM�o$nFy�:-CF�V�FNnFy_i:�`�BBB`�BBBM�o$/o$/o$