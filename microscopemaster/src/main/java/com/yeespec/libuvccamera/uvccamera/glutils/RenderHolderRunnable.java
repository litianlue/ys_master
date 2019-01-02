package com.yeespec.libuvccamera.uvccamera.glutils;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: RenderHolderRunnable.java
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

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import com.yeespec.libuvccamera.usb.UVCCamera;
import com.yeespec.libuvccamera.uvccamera.service.IUVCServiceOnFrameAvailable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Hold shared texture that has camera frame and draw them to registered surface if needs<br>
 * Using RenderHandler is little bit slow and it is better to draw in this class directly.
 * 持有共享的纹理，有相机框架，并提请他们注册的表面，如果需要的话
 * 使用renderhandler是有点慢，最好是把这类直接。
 */
public class RenderHolderRunnable implements Runnable{
    private static final boolean DEBUG = false;
    private static final String TAG = "RenderHolderRunnable";

    public interface RenderHolderCallback {
        public void onCreate(Surface surface);

        public void onDestroy();
    }

    private final Object mSync = new Object();
    private final RenderHolderCallback mCallback;
    private final SparseArray<RenderHandler> mClients = new SparseArray<RenderHandler>();
    private final SparseArray<IUVCServiceOnFrameAvailable> mOnFrameAvailables = new SparseArray<IUVCServiceOnFrameAvailable>();
    private volatile boolean isRunning;
    private volatile boolean requestDraw;
    private volatile boolean requestResize;
    private File mCaptureFile;
    private EGLBase mMasterEgl;
    private EGLBase.EglSurface mDummySurface;
    private int mTexId;     //已经绑定的将要显示的纹理ID ;
    private SurfaceTexture mMasterTexture;
    //2016.07.13 : 更新绘制surface , mTexMatrix为矩阵位图数据 ;
    final float[] mTexMatrix = new float[16];
    private Surface mSurface;
    private int mFrameWidth, mFrameHeight;
    private int mRequestWidth, mRequestHeight;

    public RenderHolderRunnable(final RenderHolderCallback callback) {
        this(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, callback);
    }

    public RenderHolderRunnable(final int width, final int height, final RenderHolderCallback callback) {
        if (DEBUG)
            Log.v(TAG, "Constructor");
        mCallback = callback;
        mFrameWidth = width;
        mFrameHeight = height;
        final Thread thread = new Thread(this, TAG);
        thread.start();     //启动RenderHolderRunnable线程本身 ;
        new Thread(mCaptureTask, "CaptureTask").start();
        synchronized (mSync) {
            if (!isRunning) {
                try {
                    mSync.wait();
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Surface getSurface() {
        if (DEBUG)
            Log.v(TAG, "getSurface:surface=" + mSurface);
        return mSurface;
    }

    //CameraClient对象的addSurface()最终final调用的addSurface();
    public void addSurface(final int id, final Surface surface, final boolean isRecordable, final IUVCServiceOnFrameAvailable onFrameAvailableListener) {
        if (DEBUG)
            Log.v(TAG, "addSurface:id=" + id + ",surface=" + surface);
        Log.i("test", "RenderHolderRunnable addSurface:id=" + id + ",surface=" + surface + " =========8888========= ");
        checkSurface();
        synchronized (mSync) {
            RenderHandler handler = mClients.get(id);
            if (handler == null) {
                handler = RenderHandler.createHandler();
                mClients.append(id, handler);
                if (onFrameAvailableListener != null)
                    mOnFrameAvailables.append(id, onFrameAvailableListener);    //IUVCServiceOnFrameAvailable的onFrameAvailable()传递到这里
                handler.setEglContext(mMasterEgl.getContext(), mTexId, surface, true);
                requestDraw = false;
                if (DEBUG)
                    Log.v(TAG, "success to add surface:id=" + id);
                Log.i("test", "RenderHolderRunnable success to add surface:id=" + id + " =========8888========= ");
            } else {
                Log.w(TAG, "specific surface id already exist");
            }
            mSync.notifyAll();
        }
    }

    public void removeSurface(final int id) {
        if (DEBUG)
            Log.v(TAG, "removeSurface:id=" + id);
        RenderHandler handler = null;
        synchronized (mSync) {
            mOnFrameAvailables.remove(id);
            handler = mClients.get(id);
            if (handler != null) {
                requestDraw = false;
                mClients.remove(id);
                handler.release();
                handler = null;
                if (DEBUG)
                    Log.v(TAG, "success to remove surface:id=" + id);
            } else {
                Log.w(TAG, "specific surface id not found");
            }
            mSync.notifyAll();
        }
        checkSurface();
    }

    public void removeAll() {
        if (DEBUG)
            Log.v(TAG, "removeAll:");
        requestDraw = false;
        synchronized (mSync) {
            final int n = mClients.size();
            for (int i = 0; i < n; i++) {
                mClients.valueAt(i).release();
            }
            mClients.clear();
            mOnFrameAvailables.clear();
            mSync.notifyAll();
        }
    }

    public void resize(final int width, final int height) {
        synchronized (mSync) {
            mRequestWidth = width;
            mRequestHeight = height;
            requestResize = true;
            mSync.notifyAll();
        }
    }

    public void captureStill(final String path) {
        if (DEBUG)
            Log.v(TAG, "captureStill:" + path);
        final File file = new File(path);
        if (DEBUG)
            Log.v(TAG, "captureStill:canWrite");
        synchronized (mSync) {
            mCaptureFile = file;
            mSync.notifyAll();
        }
    }

    public void release() {
        if (DEBUG)
            Log.v(TAG, "release:");
        removeAll();
        synchronized (mSync) {
            isRunning = false;
            mSync.notifyAll();
        }
    }

    private final OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
            synchronized (mSync) {

                requestDraw = isRunning;    //2016.09.09 : 此处赋值为true ;
                mSync.notifyAll();
            }
        }
    };

    private void checkSurface() {
        if (DEBUG)
            Log.v(TAG, "checkSurface");
        synchronized (mSync) {
            final int n = mClients.size();
            for (int i = 0; i < n; i++) {
                final RenderHandler rh = mClients.valueAt(i);
                if (rh == null || !rh.isValid()) {
                    final int id = mClients.keyAt(i);
                    if (DEBUG)
                        Log.i(TAG, "checkSurface:found invalid surface:id=" + id);
                    if (rh != null)
                        rh.release();
                    mClients.remove(id);
                    mOnFrameAvailables.remove(id);
                }
            }
            mSync.notifyAll();
        }
    }

    private void draw() {
        try {
            mDummySurface.makeCurrent();
            /**
             * 调用videoTexture .updateTexImage()方法 :
             * 大意是，从的图像流中更新纹理图像到最近的帧中。
             * 这个函数仅仅当拥有这个纹理的Opengl ES上下文当前正处在绘制线程时被调用。
             * 它将隐式的绑定到这个扩展的GL_TEXTURE_EXTERNAL_OES 目标纹理
             * （为什么上面的片段着色代码中要扩展这个OES，可能就是应为这个吧）。
             */
            // TODO: 2016/9/9 : 这里 这里 ! 更新图像数据 !
            mMasterTexture.updateTexImage();

            /**
             * videoTexture .getTransformMatrix(videoTextureTransform) 方法 :
             * 当对纹理用amplerExternalOES采样器采样时，
             * 应该首先使用getTransformMatrix(float[])查询得到的矩阵来变换纹理坐标，
             * 每次调用updateTexImage()的时候，可能会导致变换矩阵发生变化，
             * 因此在纹理图像更新时需要重新查询，该矩阵将传统的2D OpenGL ES纹理坐标列向量(s,t,0,1)，
             * 其中s，t∈[0,1]，变换为纹理中对应的采样位置。
             * 该变换补偿了图像流中任何可能导致与传统OpenGL ES纹理有差异的属性。
             * 例如，从图像的左下角开始采样，可以通过使用查询得到的矩阵来变换列向量(0,0,0,1)，
             * 而从右上角采样可以通过变换(1,1,0,1)来得到。
             */
            //2016.07.13 : 更新绘制surface , mTexMatrix为矩阵位图数据 ; 此处获取矩阵位图数据保存到mTexMatrix ;
            mMasterTexture.getTransformMatrix(mTexMatrix);
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "draw:thread id =" + Thread.currentThread().getId(), e);
            return;
        }
        synchronized (mCaptureTask) {
            mCaptureTask.notify();
        }
        synchronized (mSync) {
            final int n = mClients.size();
            for (int i = 0; i < n; i++) {
                //2016.07.13 :　此处调用RenderHandler的draw()来更新绘制Surface , mTexMatrix为矩阵位图数据 ;
                mClients.valueAt(i).draw(mTexId, mTexMatrix);
            }
            final int m = mOnFrameAvailables.size();

            for (int i = 0; i < m; i++) {
                try {

                    mOnFrameAvailables.valueAt(i).onFrameAvailable();//IUVCServiceOnFrameAvailable的onFrameAvailable()在这里回调 ;

                } catch (final RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        //重绘的背景颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);     //把一幅图像渲染为场景的背景
        GLES20.glFlush();
    }


    private final Runnable mCaptureTask = new Runnable() {
        EGLBase egl;
        EGLBase.EglSurface captureSurface;
        GLDrawer2D drawer;

        private final void init() {
            if (DEBUG)
                Log.v(TAG, "captureTask:init");
            egl = new EGLBase(mMasterEgl.getContext(), false, false);
            captureSurface = egl.createOffscreen(mFrameWidth, mFrameHeight);
            drawer = new GLDrawer2D();
            drawer.getMvpMatrxi()[5] *= -1.0f;    // flip up-side down
        }

        private final void release() {
            if (DEBUG)
                Log.v(TAG, "captureTask:release");
            captureSurface.release();
            captureSurface = null;
            drawer.release();
            drawer = null;
            egl.release();
            egl = null;
        }

        @Override
        public void run() {
            if (DEBUG)
                Log.v(TAG, "captureTask:start");
            synchronized (mSync) {
                if (!isRunning) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            init();
            int width = mFrameWidth;
            int height = mFrameHeight;
            ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            File captureFile = null;
            if (DEBUG)
                Log.v(TAG, "captureTask:loop");
            for (; isRunning; ) {
                if (captureFile == null)
                    synchronized (mSync) {
                        if (mCaptureFile == null) {
                            try {
                                mSync.wait();
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        if (mCaptureFile != null) {
                            captureFile = mCaptureFile;
                            mCaptureFile = null;
                        }
                    }
                else {
                    synchronized (mCaptureTask) {
                        try {
                            mCaptureTask.wait();
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    if (isRunning && (captureFile != null)) {
                        if ((width != mFrameWidth) || (height != mFrameHeight)) {
                            width = mFrameWidth;
                            height = mFrameHeight;
                            if (DEBUG)
                                Log.v(TAG, String.format("resize capture size(%d,%d)", width, height));
                                Log.i("test_RHRunable", String.format("resize capture size(%d,%d)", width, height));

                            buf = ByteBuffer.allocateDirect(width * height * 4);
                            buf.order(ByteOrder.LITTLE_ENDIAN);
                            if (captureSurface != null) {
                                captureSurface.release();
                                captureSurface = null;
                            }
                            captureSurface = egl.createOffscreen(width, height);
                        }
                        captureSurface.makeCurrent();
                        //2016.07.13 : 此处调用更新绘制surface , mTexMatrix为矩阵位图数据 ;
                        drawer.draw(mTexId, mTexMatrix);
                        captureSurface.swap();
                        buf.clear();
                        //glReadPixels()从帧缓冲区读取一个矩形像素数组,并把数据保存在内存中;
                        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
                        // if you save every frame as a Bitmap, app may crash by Out of Memory exception...     //如果你把每一帧都保存为位图，应用程序可能会通过内存异常崩溃…
                        if (DEBUG)
                            Log.v(TAG, String.format("save pixels(%dx%d) to png file:", width, height) + captureFile);
                        BufferedOutputStream os = null;
                        try {
                            try {
                                os = new BufferedOutputStream(new FileOutputStream(captureFile));
                                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                buf.clear();
                                bmp.copyPixelsFromBuffer(buf);
                              //
                                bmp.compress(Bitmap.CompressFormat.PNG, 90, os);
                                bmp.recycle();
                            } finally {
                                if (os != null)
                                    os.close();
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                //关闭资源输入输出流 :
                                if (os != null) {
                                    os.close();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {

                            }
                        }
                    }
                    captureFile = null;
                }
            }    // wnd of while (isRunning)
            // release resources
            if (DEBUG)
                Log.v(TAG, "captureTask finishing");
            release();
            if (DEBUG)
                Log.v(TAG, "captureTask finished");
        }

    };

    @Override
    public void run() {
        if (DEBUG)
            Log.v(TAG, "start:threadid=" + Thread.currentThread().getId());
        mMasterEgl = new EGLBase(EGL14.EGL_NO_CONTEXT, false, false);
        mDummySurface = mMasterEgl.createOffscreen(2, 2);
        mDummySurface.makeCurrent();
        mTexId = GLDrawer2D.initTex();
        mMasterTexture = new SurfaceTexture(mTexId);
        mSurface = new Surface(mMasterTexture);
        mMasterTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);  //注册视频帧是否可得到的监听器

        if (mCallback != null) {        //设为空 , 不使用 ;
            mCallback.onCreate(mSurface);
        }
        synchronized (mSync) {
            mMasterTexture.setDefaultBufferSize(mFrameWidth, mFrameHeight);

            isRunning = true;
            mSync.notifyAll();
            for (; isRunning; ) {
//                Log.i("test_RHRunnable", "mRequestWidth = " + mRequestWidth + "mRequestHeight = " + mRequestHeight + "mFrameWidth = " + mFrameWidth + "mFrameHeight = " + mFrameHeight);
                if (requestResize) {
                    requestResize = false;
                    Log.i("test_RHRunnable", "mRequestWidth = " + mRequestWidth + "mRequestHeight = " + mRequestHeight + "mFrameWidth = " + mFrameWidth + "mFrameHeight = " + mFrameHeight);
                    if ((mRequestWidth > 0) && (mRequestHeight > 0)
                            && ((mFrameWidth != mRequestWidth) || (mFrameHeight != mRequestHeight))) {
                        mFrameWidth = mRequestWidth;
                        mFrameHeight = mRequestHeight;
                        mMasterTexture.setDefaultBufferSize(mFrameWidth, mFrameHeight);
                    } else {
                        Log.i("test_RHRunnable", "mRequestWidth = " + mRequestWidth + "mRequestHeight = " + mRequestHeight + "mFrameWidth = " + mFrameWidth + "mFrameHeight = " + mFrameHeight);
                    }

                } else {
//                    Log.i("test_RHRunnable", "requestResize == ");
                }
                if (requestDraw) {
                    requestDraw = false;
                    draw();

                } else {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
        if (DEBUG)
            Log.v(TAG, "finishing");
        if (mCallback != null) {
            mCallback.onDestroy();
        }
        release();
        mSurface = null;
        mMasterTexture.release();
        mMasterTexture = null;
        GLDrawer2D.deleteTex(mTexId);
        mDummySurface.release();
        mDummySurface = null;
        mMasterEgl.release();
        mMasterEgl = null;
        if (DEBUG)
            Log.v(TAG, "finished");
    }
}
