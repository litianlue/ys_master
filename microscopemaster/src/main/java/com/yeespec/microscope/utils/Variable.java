package com.yeespec.microscope.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Mr.Wen on 2016/11/30.
 */

public class Variable {

   // public static String[] RS232_Params_names = {"baudRate", "stopBits", "dataBits", "parity", "flowControl", "sendAutoInterval"};
    public static Context gContext = null;
    private static Variable instance;
    private static SharedPreferences sharedPref = null;
    //private static SharedPreferences.Editor sharedPrefEditor = null;
    private int baudRate = 115200;
    private int dataBits = 8;
    private int flowControl = 0;
    private int parity = 1;
    /*private boolean recHex = false;
    private boolean recPause = false;
    private boolean screenFreedom = false;
    private boolean sendAreaShow = false;
    private boolean sendAuto = false;
    private int sendAutoInterval = 1000;
    private boolean sendHex = false;
    private String[] sendMemories = new String[10];
    private boolean sendMemory = false;
    private String[] sendMemoryMemos = new String[10];*/
    private int stopBits = 1;

    public Variable(Context paramContext) {
        gContext = paramContext;
    }

    public static Variable getInstance(Context context) {
        if (instance == null) {
            instance = new Variable(context);
            sharedPref = gContext.getSharedPreferences("ComHelper", 0);
        }
        return instance;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public int getFlowControl() {
        return flowControl;
    }

    public int getParity() {
        return parity;
    }
/*
    public int getSendAutoInterval() {
        return sendAutoInterval;
    }*/
/*
    public String[] getSendMemories() {
        return sendMemories;
    }

    public String[] getSendMemoryMemos() {
        return sendMemoryMemos;
    }*/

    public int getStopBits() {
        return stopBits;
    }

    /*public boolean isRecHex() {
        return recHex;
    }

    public boolean isRecPause() {
        return recPause;
    }

    public boolean isScreenFreedom() {
        return screenFreedom;
    }

    public boolean isSendAreaShow() {
        return sendAreaShow;
    }

    public boolean isSendAuto() {
        return sendAuto;
    }*/

   /* public boolean isSendHex() {
        return sendHex;
    }

    public boolean isSendMemory() {
        return sendMemory;
    }*/

    public void setBaudRate(int paramInt) {
        baudRate = paramInt;
    }

    /* public void setCookieVariables(String name, Object obj) {
         sharedPrefEditor = sharedPref.edit();
         if ((obj instanceof Integer))
             sharedPrefEditor.putInt(name, ((Integer) obj).intValue());
         else if ((obj instanceof String))
             sharedPrefEditor.putString(name, (String) obj);
         else if ((obj instanceof Boolean))
             sharedPrefEditor.putBoolean(name, ((Boolean) obj).booleanValue());
         sharedPrefEditor.commit();
     }
 */
   /* public void setCookieVariables(String[] name, Object[] obj) {
        sharedPrefEditor = sharedPref.edit();
        if (obj[0] instanceof Integer) {
            for (int i = 0; i >= name.length; i++) {
                sharedPrefEditor.putInt(name[i], ((Integer) obj[i]).intValue());
            }
        } else if (obj[0] instanceof String) {
            for (int i = 0; i < name.length; i++) {
                sharedPrefEditor.putString(name[i], (String) obj[i]);
            }
        } else if (obj[0] instanceof Boolean) {
            for (int i = 0; i < name.length; i++) {
                sharedPrefEditor.putBoolean(name[i], ((Boolean) obj[i]).booleanValue());
            }
        }
        sharedPrefEditor.commit();
    }
*/
    public void setDataBits(int paramInt) {
        dataBits = paramInt;
    }

    public void setFlowControl(int paramInt) {
        flowControl = paramInt;
    }

    public void setParity(int paramInt) {
        parity = paramInt;
    }

   /* public void setRecHex(boolean paramBoolean) {
        recHex = paramBoolean;
    }

    public void setRecPause(boolean paramBoolean) {
        recPause = paramBoolean;
    }*/
/*
    public void setScreenFreedom(boolean paramBoolean) {
        screenFreedom = paramBoolean;
    }

    public void setSendAreaShow(boolean paramBoolean) {
        sendAreaShow = paramBoolean;
    }

    public void setSendAuto(boolean paramBoolean) {
        sendAuto = paramBoolean;
    }*/

   /* public void setSendAutoInterval(int paramInt) {
        sendAutoInterval = paramInt;
    }

    public void setSendHex(boolean paramBoolean) {
        sendHex = paramBoolean;
    }*/
/*

    public void setSendMemories(String[] paramArrayOfString) {
        sendMemories = paramArrayOfString;
    }

    public void setSendMemory(boolean paramBoolean) {
        sendMemory = paramBoolean;
    }

    public void setSendMemoryMemos(String[] paramArrayOfString) {
        sendMemoryMemos = paramArrayOfString;
    }
*/

    public void setStopBits(int paramInt) {
        stopBits = paramInt;
    }

}
