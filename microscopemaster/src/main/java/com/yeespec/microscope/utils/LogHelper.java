package com.yeespec.microscope.utils;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Mr.Wen on 2016/11/30.
 */

public class LogHelper {

    public static String DIR = "ComHelperLog";
    public static String DIRSECOND = DIR + File.separator + "Save";
    private static LogHelper INSTANCE = null;
    private static String LOGCAT_DIR = null;
    public static String LOGCAT_DIR_SAVE = null;
    public static String TAG = "ComHelperLog";
    private int appPid;
    private LogThread logThread = null;

    private LogHelper(Context paramContext) {
        init(paramContext);
        appPid = android.os.Process.myPid();
    }

    public static LogHelper getInstance(Context paramContext) {
        if (INSTANCE == null)
            INSTANCE = new LogHelper(paramContext);
        return INSTANCE;
    }

    public void init(Context context) {
        if (Environment.getExternalStorageState().equals("mounted")) {
            LOGCAT_DIR = File.separator + DIR;
            LOGCAT_DIR_SAVE = File.separator + DIRSECOND;
        } else {
            LOGCAT_DIR = File.separator + DIR;
            LOGCAT_DIR_SAVE = File.separator + DIRSECOND;
        }
        File file = new File(LOGCAT_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        File fileSave = new File(LOGCAT_DIR_SAVE);
        if (!fileSave.exists()) {
            fileSave.mkdirs();
        }
    }

    public void start() {
        if (logThread == null)
            logThread = new LogThread(String.valueOf(appPid), LOGCAT_DIR);
        logThread.start();
    }

    public void stop() {
        if (logThread != null) {
            logThread.stopLogs();
            logThread = null;
        }
    }

    private class LogThread extends Thread {
        private BufferedReader bufferedReader = null;
        String cmds = null;
        private Process logcatProc;
        private String myPID;
        private FileOutputStream out = null;
        private boolean runningFlag = true;

        public LogThread(String paramString1, String arg3) {
            myPID = paramString1;
            try {
                String str = arg3;
                out = new FileOutputStream(new File(str, new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(System.currentTimeMillis())) + ".log"), true);
                cmds = ("logcat *:e *:w | -s " + LogHelper.TAG + " & grep \"(" + myPID + ")\"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

       /* public LogThread(LogHelper p1, String pid, String dir) {
            myPID = pid;
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                String curDate = format.format(new Date(System.currentTimeMillis()));
                out = new FileOutputStream(new File(dir, ".log"), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cmds = LogHelper.TAG + " & grep \"(" + myPID + ")\"";
        }
*/

        // ERROR //
        public void run() {
            // TODO: 2016/12/1 反编译出错 :
            try {
                logcatProc = Runtime.getRuntime().exec(cmds);
                bufferedReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 0x400);
                String line = null;
                if ((runningFlag) && (line != null)) {
                    if (runningFlag) {
                        if (line.length() != 0) {
                            if ((out != null) && (line.contains(myPID))) {
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                                String curDate = format.format(new Date(System.currentTimeMillis()));
                                out.write(("  " + line + "\n").getBytes());
                            }
                        }

                        if (logcatProc != null) {
                            logcatProc.destroy();
                            logcatProc = null;
                        }
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                                bufferedReader = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException local1) {
                                local1.printStackTrace();
                            }
                            out = null;
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                        bufferedReader = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException local1) {
                        local1.printStackTrace();
                    }
                    out = null;
                }
            }
            // Parsing error may occure here :(
        }

        public void stopLogs() {
            runningFlag = false;
        }


    }

}
