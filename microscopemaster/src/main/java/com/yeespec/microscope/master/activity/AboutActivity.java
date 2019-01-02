package com.yeespec.microscope.master.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.yeespec.R;
import com.yeespec.microscope.master.BaseActivity;
import com.yeespec.microscope.master.application.BaseApplication;

public class AboutActivity extends BaseActivity {

    //    private static final String TAG = "AboutActivity";

    private BaseApplication application;
    private TextView btn_return;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG = "AboutActivity";
        setContentView(R.layout.activity_about);

       /* application = (BaseApplication) getApplication();
        application.init();
        application.addActivity(this);*/

        btn_return = (TextView) findViewById(R.id.btn_back);
        btn_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AboutActivity.this, SettingActivity.class);
                startActivity(intent);
                finish();
            }
        });

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }


}
