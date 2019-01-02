package com.yeespec.libuvccamera.uvccamera.widget;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: UVCCameraTextureView.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.yeespec.libuvccamera.uvccamera.encoder.MediaEncoderRunnable;
import com.yeespec.microscope.utils.log.Logger;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/**
 * 核心代码 :
 * 原理是使用
 * UVCCamera来控制、管理与外接设备的连接，
 * UVCCameraTextureView控件进行图像的预览，
 * USBMonitor进行驱动的连接和断开
 */

/**
 * change the view size with keeping the specified aspect ratio.
 * if you set this view with in a FrameLayout and set property "android:layout_gravity="center",
 * you can show this view in the center of screen and keep the aspect ratio of content
 * XXX it is better that can set the aspect raton a a xml property
 * <p/>
 * 改变视图大小以保持指定的纵横比。
 * 如果你设置这个视图中FrameLayout和设置属性“Android：layout_gravity =“中心”，
 * 您可以在屏幕中心显示该视图，并保持内容的纵横比
 * XXX是更好的，可以设置方面拉顿一一XML属性
 * <p/>
 * 这个部分需要注意,写在SurfaceHolder的回调方法内,为的是让SurfaceView中的Surface成功建立后,
 * 再将Surface传入C代码中进行处理
 */
public class UVCCameraTextureView extends TextureView    // API >= 14
        implements TextureView.SurfaceTextureListener, CameraViewInterface {

    private static final boolean DEBUG = false;    // TODO set false on production
    private static final String TAG = "UVCCameraTextureView";

    private double mRequestedAspect = -1.0;        // initially use default window size     //最初使用默认窗口大小
    private static int mWidthPixels = 1360;        //保存视窗的宽 默认1360
    private static int mHeightPixels = 1040;        //保存视窗的高 默认1040

    private boolean mHasSurface;
    private Surface mPreviewSurface;
    private Callback mCallback;

    private final Object mCaptureSync = new Object();
    private Bitmap mTempBitmap;
    private boolean mReqesutCaptureStillImage;

    public UVCCameraTextureView(final Context context) {
        this(context, null, 0);
    }

    public UVCCameraTextureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UVCCameraTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        if (DEBUG)
            Log.v(TAG, "Constructor:");
        setSurfaceTextureListener(this);
        //新增 :
        requestLayout();
        invalidate();

    }

  /*  final Bitmap colorBitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);

    public Bitmap changeImageColor(Bitmap bitmap, int newColor) {
        try {
            Canvas canvas = new Canvas(colorBitmap);
            Paint paint = new Paint();
            paint.setColorFilter(new LightingColorFilter(newColor, 0));//通过color控制图片颜色
            canvas.drawBitmap(bitmap, 0, 0, paint);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return colorBitmap;
    }*/

    //获取屏幕的宽度
    public static int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getWidth();
    }

    public static int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return display.getHeight();
    }

    //--------------------------------------------------------------------------------
    //TextureView.SurfaceTextureListener接口的实现 :

    @Override
    public Canvas lockCanvas() {
        return super.lockCanvas();
    }

    /**
     * SurfaceTexture : 能捕获一个图像流的一帧来作为OpenGL 的texture也就是纹理。
     * 这个图片流主要是来自相机的预览或视频的解码。
     */

    @Override
    public void unlockCanvasAndPost(Canvas canvas) {
        lockCanvas();
//        Paint  p = new Paint();
//        Bitmap btm = getBitmap();
//        btm = creatGrayBitmap(btm,20);
//        canvas.drawBitmap(btm,0,0,p);
//        Log.e("UVCCameraTextureView", "length "+Bitmap2Bytes(btm).length );
        super.unlockCanvasAndPost(canvas);
    }

    /**
     * 视频绘制前的配置就发生在这个对象所在类中.
     * 真正的绘制工作则在它的子类中VideoTextureSurfaceRenderer
     */

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        if (DEBUG)
            Log.v(TAG, "onSurfaceTextureAvailable:" + surface);
        //      mRenderHandler = RenderHandler.createHandler(surface);
        mHasSurface = true;

        if (mCallback != null) {
            mCallback.onSurfaceCreated(getSurface());
        }
    }


    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
        //TextureView创建过程中没有进到onSurfaceTextureSizeChanged()这个函数里
        //而SurfaceView在创建过程中，从无到有的时候会进到大小发生变化回调里
        if (DEBUG)
            Log.v(TAG, "onSurfaceTextureSizeChanged:" + surface);
        if (mCallback != null) {
            mCallback.onSurfaceChanged(getSurface(), width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        if (DEBUG)
            Log.v(TAG, "onSurfaceTextureDestroyed:" + surface);
        mHasSurface = false;
        if (mCallback != null) {
            mCallback.onSurfaceDestroy(getSurface());
        }
        if (mPreviewSurface != null) {
            mPreviewSurface.release();

            mPreviewSurface = null;
            System.gc();
        }
        return true;
    }
    public byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
        //onSurfaceTextureUpdated()这个函数每上来一帧数据，这块就进来一次 , 这是跟Surfaceview相比，最伟大的一个地方
        if (DEBUG)
            Log.v(TAG, "onSurfaceTextureUpdated:" + surface);
//
//        Log.e("UVCCameraTextureView", "mTempBit   " + surface);
//        synchronized (mCaptureSync) {
//            if (mReqesutCaptureStillImage) {
//                mReqesutCaptureStillImage = false;
//                if (mTempBitmap == null) {
//
//                    mTempBitmap = getBitmap();
//                    Log.e("UVCCameraTextureView", "mTempBitmap =  " + mTempBitmap.getByteCount());
//                   // mTempBitmap = creatGrayBitmap(mTempBitmap,20);
//                    // TODO modify this to change output image size
//                    //                    mTempBitmap = getBitmap(1600, 1200);//3264,2448
//                } else {
//                   // Log.e("UVCCameraTextureView", "mTempBitmap != null   " + surface);
//                    getBitmap(mTempBitmap);
//                   // mTempBitmap = creatGrayBitmap(mTempBitmap,20);
//                }
//                mCaptureSync.notifyAll();
//            }
//        }
    }
    private Bitmap creatGrayBitmap(Bitmap btm,int threshole){
        int height = btm.getHeight();
        int width = btm.getWidth();
        //创建线性拉升灰度图像
        Bitmap linegray = null;
        linegray = btm.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //得到每点的像素值
                int col = btm.getPixel(i, j);
                int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);


                float tempgray = (float) (0.114 * blue + 0.587 * green + 0.299 * red);
                if(tempgray<threshole) {
                    red = 0;
                    green = 0;
                    blue = 0;
                    // 新的ARGB
                    int newColor = alpha | (red << 16) | (green << 8) | blue;
                    //设置新图像的RGB值
                    linegray.setPixel(i, j, newColor);
                }else
                    linegray.setPixel(i, j, col);
            }
        }
        return linegray;

    }
    /*
    * UI框架开始绘制时，皆是从ViewRoot.java类开始绘制的：
    ViewRoot类简要说明:
        任何显示在设备中的窗口，例如：Activity、Dialog等，都包含一个ViewRoot实例，
        该类主要用来与远端 WindowManagerService交互以及控制(开始/销毁)绘制。
    */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (DEBUG)
            Log.v(TAG, "onMeasure:" + widthMeasureSpec + "," + heightMeasureSpec);
        if (mRequestedAspect > 0) {     //  宽/高
            /*
            MeasureSpec.getSize() :根据提供的测量值(格式)，提取大小值(这个大小也就是我们通常所说的大小)
            * */
//            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
//            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);
//
//            final int horizPadding = getPaddingLeft() + getPaddingRight();
//            final int vertPadding = getPaddingTop() + getPaddingBottom();
//            initialWidth -= horizPadding;
//            initialHeight -= vertPadding;
//
//            //            final double viewAspectRatio = (double) initialWidth / initialHeight;   //视图方向比例 ;
//            //            final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;
//            final double viewAspectRatio = new BigDecimal(initialWidth).divide(new BigDecimal(initialHeight), 5, BigDecimal.ROUND_HALF_UP).doubleValue();   //视图方向比例 ;
//            final double aspectDiff = new BigDecimal(mRequestedAspect).divide(new BigDecimal(viewAspectRatio), 5, BigDecimal.ROUND_HALF_UP).doubleValue() - 1;
//
//            if (Math.abs(aspectDiff) > 0.01) {
//                //2016.09.12 : 修改 : 改为以短边为基准按比例截取长边;
//                if (aspectDiff > 0) {
//                    //                    initialHeight = (int) (initialWidth / mRequestedAspect);
//                    initialWidth = (int) (initialHeight * mRequestedAspect);
//                } else {
//                    initialHeight = (int) (initialWidth / mRequestedAspect);
//                    //                    initialWidth = (int) (initialHeight * mRequestedAspect);
//                }
//                //                if (aspectDiff > 0) {
//                //                    initialHeight = (int) (initialWidth / mRequestedAspect);
//                //                } else {
//                //                    initialWidth = (int) (initialHeight * mRequestedAspect);
//                //                }
//                initialWidth += horizPadding;
//                initialHeight += vertPadding;
//
//            } else {
//                //                Log.i("test_TexView", "Math.abs(aspectDiff) < 0.01" );
//            }
//
//            int screenWidth = getScreenWidth(getContext());
//            int screenHeight = getScreenHeight(getContext());
//
//            //2016.09.12 : 新增 :
//            int resultScreenWidth = 1360;
//            int resultScreenHeight = 1040;
//
//            //                if (mWidthPixels < resultScreenWidth | mHeightPixels < resultScreenHeight) {
//            //                    //                    resultScreenWidth = mWidthPixels;
//            //                    //                    resultScreenHeight = mHeightPixels;
//            //
//            //                    //                    mWidthPixels = resultScreenWidth;
//            //                    //                    mHeightPixels = resultScreenHeight;
//            //
//            //                    resultScreenWidth += horizPadding;
//            //                    resultScreenHeight += vertPadding;
//            //
//            //                }
//
//            if (mWidthPixels > resultScreenWidth & mHeightPixels > resultScreenHeight) {
//                //                    if (screenWidth < screenHeight) {
//                //
//                //                    } else if (screenWidth > screenHeight) {
//                //                    resultScreenWidth = mHeightPixels * resultScreenWidth / resultScreenHeight;
//                //                    resultScreenHeight = mHeightPixels;
//
//                resultScreenHeight = mWidthPixels * resultScreenHeight / resultScreenWidth;
//                resultScreenWidth = mWidthPixels;
//
//                //                    if (resultScreenHeight < 1040 | resultScreenWidth < 1360) {
//                //                        resultScreenWidth = 1360;
//                //                        resultScreenHeight = 1040;
//                //                    } else {
//                resultScreenWidth += horizPadding;
//                resultScreenHeight += vertPadding;
//                //                    }
//
//                //                    resultScreenWidth = screenHeight * resultScreenWidth / resultScreenHeight;
//                //                    resultScreenHeight = screenHeight;
//                //                    }
//            }
//
//            if (resultScreenHeight < 1040 | resultScreenWidth < 1360) {
//                resultScreenWidth = 1360;
//                resultScreenHeight = 1040;
//            }
//
//            /*
//            MeasureSpec.makeMeasureSpec() :创建一个整形值，其高两位代表mode类型，其余30位代表长或宽的实际值。
//            可以是WRAP_CONTENT、MATCH_PARENT或具体大小exactly size ;
//
//            MeasureSpec.EXACTLY(完全)，父元素决定自元素的确切大小，子元素将被限定在给定的边界里而忽略它本身大小；
//            * */
//            widthMeasureSpec = MeasureSpec.makeMeasureSpec(resultScreenWidth, MeasureSpec.EXACTLY);   //Measure Spc 测量SPC
//            heightMeasureSpec = MeasureSpec.makeMeasureSpec(resultScreenHeight, MeasureSpec.EXACTLY);
//
//            //                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth * screenHeight / initialHeight, MeasureSpec.EXACTLY);   //Measure Spc 测量SPC
//            //                heightMeasureSpec = MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY);
//
//            //                widthMeasureSpec = MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY);   //Measure Spc 测量SPC
//            //                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight * screenWidth / initialWidth, MeasureSpec.EXACTLY);
//
//            if (DEBUG)
//                Logger.e(TAG, "width = " + initialHeight + ",height = " + (initialHeight * screenWidth / initialWidth));

            //            Log.i("test_TexView", "screenWidth = " + screenWidth + ",screenHeight = " + screenHeight + ",initialWidth = " + initialWidth + ",initialHeight = " + initialHeight +
            //                    ",widthSpec = " + initialWidth * screenHeight / initialHeight + ",mRequestedAspect = " + mRequestedAspect
            //                    + "\n widthMeasureSpec = " + widthMeasureSpec + ", heightMeasureSpec = " + heightMeasureSpec
            //                    + "\n resultScreenWidth = " + resultScreenWidth + ", resultScreenHeight = " + resultScreenHeight
            //                    + "\n mWidthPixels = " + mWidthPixels + ", mHeightPixels = " + mHeightPixels);
        } else {
            //            Log.i("test_TexView", "mRequestedAspect < 0" );
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    //--------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------
    //CameraViewInterface接口的实现 :

    @Override
    public void setCallback(final Callback callback) {
        mCallback = callback;
    }

    @Override
    public boolean hasSurface() {
        return mHasSurface;
    }

    @Override
    public Surface getSurface() {   //获取生产的 SurfaceTexture,在子类中实现
        if (DEBUG)
            Log.v(TAG, "getSurface:hasSurface=" + mHasSurface);
        if (mPreviewSurface == null) {
            final SurfaceTexture st = getSurfaceTexture();   //获取生产的 SurfaceTexture,在子类中实现
            if (st != null)
                mPreviewSurface = new Surface(st);
        }
        return mPreviewSurface;
    }

    @Override
    public void setVideoEncoder(MediaEncoderRunnable encoder) {

    }

    /**
     * capture preview image as a bitmap
     * this method blocks current thread until bitmap is ready
     * if you call this method at almost same time from different thread,
     * the returned bitmap will be changed while you are processing the bitmap
     * (because we return same instance of bitmap on each call for memory saving)
     * if you need to call this method from multiple thread,
     * you should change this method(copy and return)
     * <p/>
     * 捕获预览图像作为位图
     * 该方法将阻止当前线程，直到位图已准备就绪为止
     * 如果你在几乎相同的时间从不同的线程调用这个方法，
     * 当您正在处理位图时，返回的位图将被更改
     * （因为我们在每个调用内存保存的位图上返回相同的位图实例）
     * 如果您需要从多个线程调用这个方法，
     * 您应该更改此方法（复制和返回）
     */
    @Override
    public Bitmap captureStillImage() {
        synchronized (mCaptureSync) {
            mReqesutCaptureStillImage = true;
            try {
                mCaptureSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            return mTempBitmap;
        }
    }

    //--------------------------------------------------------------------------------
    //CameraViewInterface的父接口AspectRatioViewInterface的实现 :
    @Override
    public void onResume() {
        if (DEBUG)
            Log.v(TAG, "onResume:");
    }

    @Override
    public void onPause() {
        if (DEBUG)
            Log.v(TAG, "onPause:");
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
        if (mTempBitmap != null) {
            mTempBitmap.recycle();
            mTempBitmap = null;
        }
    }

    @Override
    public void setAspectRatio(final double aspectRatio) {      //设置视图方向比例 ;
        if (DEBUG)
            Log.v(TAG, "setAspectRatio:");
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        if (mRequestedAspect != aspectRatio) {       //视图方向比例 ;
            mRequestedAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    public void setAspectRatio(int width, int height) {
        if (DEBUG)
            Log.v(TAG, "setAspectRatio:");
        mWidthPixels = width;
        mHeightPixels = height;
        double aspectRatio = new BigDecimal(width).divide(new BigDecimal(height), 5, BigDecimal.ROUND_HALF_UP).doubleValue();
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        if (mRequestedAspect != aspectRatio) {       //视图方向比例 ;
            mRequestedAspect = aspectRatio;
            requestLayout();
        }
    }
    //--------------------------------------------------------------------------------


}
