package com.yeespec.microscope.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

import com.yeespec.microscope.master.activity.MasterActivity;
import com.yeespec.microscope.master.application.BaseApplication;

/**
 * Created by Administrator on 2016/3/25.
 */

/**
 * Thread的run方法是不抛出任何检查型异常(checked exception)的,但是它自身却可能因为一个异常而被终止，
 * 导致这个线程的终结。最麻烦的是，在线程中抛出的异常即使使用try...catch也无法截获，因此可能导致一些问题出现，
 * 比如异常的时候无法回收一些系统资源，或者没有关闭当前的连接等等。
 *
 * uncaughtException(Thread a, Throwable e)可以拿到Thread，所以在uncaughtException释放相关资源是最好的办法。
 *
 */
public class UnCeHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler mDefaultHandler;
    public static final String TAG = "CatchExcep";
    BaseApplication application;

    public UnCeHandler(BaseApplication application) {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler(); //获取系统默认的UncaughtException处理器
        this.application = application;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if(!handleException(ex) && mDefaultHandler != null){
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        }else {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(application.getApplicationContext(), MasterActivity.class);
            PendingIntent restartIntent = PendingIntent.getActivity(application.getApplicationContext(),
                    0, intent, Intent.FILL_IN_ACTION);
            AlarmManager mgr = (AlarmManager)application.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,
                    restartIntent); // 1秒钟后重启应用
            application.finishActivity();
        }

    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用Toast来显示异常信息
        new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(application.getApplicationContext(), "程序即将跳转到主页面.",
                        Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();
        return true;
    }
}
