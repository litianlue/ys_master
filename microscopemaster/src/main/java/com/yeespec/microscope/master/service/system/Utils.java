package com.yeespec.microscope.master.service.system;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    private Utils() {
    }

    public static String readAll(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null.");
        }
        BufferedReader r = null;
        try {
            int size = inputStream.available();
            if (size == 0) {
                return "";
            }
            r = new BufferedReader(new InputStreamReader(inputStream), size);
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        } catch (IOException e) {
            e.printStackTrace();
           // Log.e(TAG, "Error reading from stream.", e);
            return "";
        } finally {
            try {//关闭资源输入输出流 :
                if (r != null) {
                    r.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Dumps the process output, that is input and error stream.
     *
     * @param process
     * @return The error output for further analysis, or an empty string.
     */
    public static String dumpProcessOutput(Process process) {
        if (process == null) {
            throw new IllegalArgumentException("process cannot be null.");
        }
        String stdOut = readAll(process.getInputStream());
        String stdErr = readAll(process.getErrorStream());
        if (stdOut.length() > 0) {
            Log.i(TAG, "Process console output: \n" + stdOut);
        }
        if (stdErr.length() > 0) {
            Log.e(TAG, "Process error output: \n" + stdErr);
        }
        return stdErr;
    }

    public static void killMyProcess() {
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }
}