package com.yeespec.libuvccamera.uvccamera.glutils;

import android.graphics.Bitmap;

/**
 * Created by Mr.Wen on 2017/4/5.
 */

public class JniUtils {
    static {
        System.loadLibrary("native-lib");
    }


    public static native double getFreShnesFormC(int buffs[],int heigh,int width,int bpp );
    public static native double getBitmapGamma(byte buffs[],int heigh,int width,int ts);
    public static native double getOperationBrignes(Bitmap bitmap);
    public static native double changeBrightness(int gamma, Bitmap bitmap);

    public static native double fillOnePixelBitmap(Bitmap bitmap);
}
