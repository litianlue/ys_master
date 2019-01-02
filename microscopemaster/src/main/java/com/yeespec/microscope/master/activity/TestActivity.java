package com.yeespec.microscope.master.activity;

import android.content.Intent;
import android.os.Bundle;

import com.yeespec.microscope.master.BaseActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yuchunrong on 2017-09-30.
 */

public class TestActivity extends BaseActivity {
    private Timer timer;
    private TimerTask timerTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(TestActivity.this, YeeSpecActivity.class);
        startActivity(intent);
        finish();
        /*timerTask = new TimerTask() {
            @Override
            public void run() {
                timer.cancel();

            }
        };
        timer = new Timer();
        timer.schedule(timerTask,3000,3000);*/
    }
}
