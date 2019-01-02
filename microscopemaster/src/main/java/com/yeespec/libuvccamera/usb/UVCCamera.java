package com.yeespec.libuvccamera.usb;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: UVCCamera.java
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

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.yeespec.libuvccamera.usb.USBMonitor.UsbControlBlock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 核心代码 :
 * 原理是使用
 * UVCCamera来控制、管理与外接设备的连接，
 * UVCCameraTextureView控件进行图像的预览，
 * USBMonitor进行驱动的连接和断开
 */

public class UVCCamera {
    private static final boolean DEBUG = false; // TODO set false when releasing
    private static final String TAG = UVCCamera.class.getSimpleName();
    private static final String DEFAULT_USBFS = "/dev/bus/usb";
    // 分辨率1
    //    public static final int DEFAULT_PREVIEW_WIDTH = 640;
    //    public static final int DEFAULT_PREVIEW_HEIGHT = 480;
    // 分辨率2
    //    public static final int DEFAULT_PREVIEW_WIDTH = 1280;
    //    public static final int DEFAULT_PREVIEW_HEIGHT = 1024;
    // 分辨率3
    public static final int DEFAULT_PREVIEW_WIDTH = 1280;       //长宽分辨率 1360*1040 = 1414400
    public static final int DEFAULT_PREVIEW_HEIGHT = 960;
    public static final boolean camraType = true;// true :iso值为真值，不需要转换  true :lite smark
   // public static final int DEFAULT_PREVIEW_WIDTH = 2592;       //长宽分辨率 1360*1040 = 1414400
   // public static final int DEFAULT_PREVIEW_HEIGHT = 1944;

    //   public static final int DEFAULT_PREVIEW_WIDTH = 1280;       //长宽分辨率 1920*972
    //   public static final int DEFAULT_PREVIEW_HEIGHT= 960;   //640  480


    public static final int DEFAULT_PREVIEW_MODE = 0;
    public static final float DEFAULT_BANDWIDTH = 1.0f;
    public static final int FRAME_FORMAT_YUYV = 0;
    public static final int FRAME_FORMAT_MJPEG = 1;         //图像存储格式为 MJPEG

    // public static final int PIXEL_FORMAT_RAW = 0;
    // public static final int PIXEL_FORMAT_YUV = 1;
    // public static final int PIXEL_FORMAT_RGB565 = 2;
    public static final int PIXEL_FORMAT_RGBX = 3;          //用32bit来存储 3位RGB 8888
    //  public static final int PIXEL_FORMAT_YUV420SP = 4;
    //  public static final int PIXEL_FORMAT_NV21 = 5;        // = YVU420SemiPlanar

    //--------------------------------------------------------------------------------
    public static final int CTRL_SCANNING = 0x00000001;      // D0:  Scanning Mode
    public static final int CTRL_AE = 0x00000002;            // D1:  Auto-Exposure Mode
    public static final int CTRL_AE_PRIORITY = 0x00000004;   // D2:  Auto-Exposure Priority
    public static final int CTRL_AE_ABS = 0x00000008;        // D3:  Exposure Time (Absolute)
    public static final int CTRL_AR_REL = 0x00000010;        // D4:  Exposure Time (Relative)
    public static final int CTRL_FOCUS_ABS = 0x00000020;     // D5:  Focus (Absolute)
    public static final int CTRL_FOCUS_REL = 0x00000040;     // D6:  Focus (Relative)
    public static final int CTRL_IRIS_ABS = 0x00000080;      // D7:  Iris (Absolute)
    public static final int CTRL_IRIS_REL = 0x00000100;      // D8:  Iris (Relative)
    public static final int CTRL_ZOOM_ABS = 0x00000200;      // D9:  Zoom (Absolute)
    public static final int CTRL_ZOOM_REL = 0x00000400;      // D10: Zoom (Relative)
    public static final int CTRL_PANTILT_ABS = 0x00000800;   // D11: PanTilt (Absolute)
    public static final int CTRL_PANTILT_REL = 0x00001000;   // D12: PanTilt (Relative)
    public static final int CTRL_ROLL_ABS = 0x00002000;      // D13: Roll (Absolute)
    public static final int CTRL_ROLL_REL = 0x00004000;      // D14: Roll (Relative)
    public static final int CTRL_FOCUS_AUTO = 0x00020000;    // D17: Focus, Auto
    public static final int CTRL_PRIVACY = 0x00040000;       // D18: Privacy
    public static final int CTRL_FOCUS_SIMPLE = 0x00080000;  // D19: Focus, Simple
    public static final int CTRL_WINDOW = 0x00100000;        // D20: Window

    public static final int PU_BRIGHTNESS = 0x80000001;     // D0: Brightness
    public static final int PU_CONTRAST = 0x80000002;       // D1: Contrast
    public static final int PU_HUE = 0x80000004;            // D2: Hue
    public static final int PU_SATURATION = 0x80000008;     // D3: Saturation
    public static final int PU_SHARPNESS = 0x80000010;      // D4: Sharpness
    public static final int PU_GAMMA = 0x80000100;          // D5: Gamma
    public static final int PU_WB_TEMP = 0x80000040;        // D6: White Balance Temperature
    public static final int PU_WB_COMPO = 0x80000080;       // D7: White Balance Component
    public static final int PU_BACKLIGHT = 0x80000100;      // D8: Backlight Compensation
    public static final int PU_GAIN = 0x80000200;           // D9: Gain
    public static final int PU_POWER_LF = 0x80000400;       // D10: Power Line Frequency
    public static final int PU_HUE_AUTO = 0x80000800;       // D11: Hue, Auto
    public static final int PU_WB_TEMP_AUTO = 0x80001000;   // D12: White Balance Temperature, Auto
    public static final int PU_WB_COMPO_AUTO = 0x80002000;  // D13: White Balance Component, Auto
    public static final int PU_DIGITAL_MULT = 0x80004000;   // D14: Digital Multiplier
    public static final int PU_DIGITAL_LIMIT = 0x80008000;  // D15: Digital Multiplier Limit
    public static final int PU_AVIDEO_STD = 0x80010000;     // D16: Analog Video Standard
    public static final int PU_AVIDEO_LOCK = 0x80020000;    // D17: Analog Video Lock Status
    public static final int PU_CONTRAST_AUTO = 0x80040000;  // D18: Contrast, Auto

    // uvc_status_class from libuvc.h
    public static final int STATUS_CLASS_CONTROL = 0x10;
    public static final int STATUS_CLASS_CONTROL_CAMERA = 0x11;
    public static final int STATUS_CLASS_CONTROL_PROCESSING = 0x12;

    // uvc_status_attribute from libuvc.h
    public static final int STATUS_ATTRIBUTE_VALUE_CHANGE = 0x00;
    public static final int STATUS_ATTRIBUTE_INFO_CHANGE = 0x01;
    public static final int STATUS_ATTRIBUTE_FAILURE_CHANGE = 0x02;
    public static final int STATUS_ATTRIBUTE_UNKNOWN = 0xff;

    private static boolean isLoaded;

    static {
        //在使用“.so”库的时候记得添加库才能使用本地语言
        if (!isLoaded) {

            System.loadLibrary("jpeg-turbo1400");
            System.loadLibrary("usb100");       //导入usb100.so库文件,调用jni的c代码:
            System.loadLibrary("uvc");          //导入uvc.so库文件,调用jni的c代码:
            System.loadLibrary("UVCCamera");    //导入UVCCamera.so库文件,调用jni的c代码:

            //jni so位置在 "/data/data/"+getPackageName()+"/lib/目录下"
            // /data/data/com.yeespec.microscope.master/lib/

            //            System.load("/data/data/com.yeespec.microscope.master/lib/libjpeg-turbo1400.so");
            //            System.load("/data/data/com.yeespec.microscope.master/lib/libusb100.so");       //导入usb100.so库文件,调用jni的c代码:
            //            System.load("/data/data/com.yeespec.microscope.master/lib/libuvc.so");          //导入uvc.so库文件,调用jni的c代码:
            //            System.load("/data/data/com.yeespec.microscope.master/lib/libUVCCamera.so");    //导入UVCCamera.so库文件,调用jni的c代码:

            isLoaded = true;
        }
    }


    private UsbControlBlock mCtrlBlock;
    protected long mControlSupports;            // カメラコントロールでサポートしている機能フラグ  //用照相机控制支持的功能标志
    protected long mProcSupports;                // プロセッシングユニットでサポートしている機能フラグ   //在处理单元支持的功能标志
    protected int mCurrentPreviewMode = 0;
    protected int mCurrentPreviewWidth = DEFAULT_PREVIEW_WIDTH, mCurrentPreviewHeight = DEFAULT_PREVIEW_HEIGHT;
    protected String mSupportedSize;
    // these fields from here are accessed from native code and do not change name and remove   //从这里访问这些字段从本机代码访问，不更改名称和删除
    protected long mNativePtr;          // //保存当前连接的相机ID : id_camera ;
    protected int mScanningModeMin, mScanningModeMax, mScanningModeDef;
    protected int mExposureModeMin, mExposureModeMax, mExposureModeDef;
    protected int mExposurePriorityMin, mExposurePriorityMax, mExposurePriorityDef;
    protected int mExposureMin, mExposureMax, mExposureDef;
    protected int mAutoFocusMin, mAutoFocusMax, mAutoFocusDef;
    protected int mFocusMin, mFocusMax, mFocusDef;                  //聚焦Focus
    protected int mFocusRelMin, mFocusRelMax, mFocusRelDef;
    protected int mFocusSimpleMin, mFocusSimpleMax, mFocusSimpleDef;
    protected int mIrisMin, mIrisMax, mIrisDef;
    protected int mIrisRelMin, mIrisRelMax, mIrisRelDef;
    protected int mPanMin, mPanMax, mPanDef;
    protected int mTiltMin, mTiltMax, mTiltDef;
    protected int mRollMin, mRollMax, mRollDef;
    protected int mPanRelMin, mPanRelMax, mPanRelDef;
    protected int mTiltRelMin, mTiltRelMax, mTiltRelDef;
    protected int mRollRelMin, mRollRelMax, mRollRelDef;
    protected int mPrivacyMin, mPrivacyMax, mPrivacyDef;
    protected int mAutoWhiteBlanceMin, mAutoWhiteBlanceMax, mAutoWhiteBlanceDef;
    protected int mAutoWhiteBlanceCompoMin, mAutoWhiteBlanceCompoMax, mAutoWhiteBlanceCompoDef;
    protected int mWhiteBlanceMin, mWhiteBlanceMax, mWhiteBlanceDef;        //白平衡WhiteBlance
    protected int mWhiteBlanceRelMin, mWhiteBlanceRelMax, mWhiteBlanceRelDef;
    protected int mBacklightCompMin, mBacklightCompMax, mBacklightCompDef;
    protected int mBrightnessMin, mBrightnessMax, mBrightnessDef;   //亮度Brightness
    protected int mContrastMin, mContrastMax, mContrastDef;         //对比度Contrast
    protected int mSharpnessMin, mSharpnessMax, mSharpnessDef;      //锐度Sharpness
    protected int mGainMin, mGainMax, mGainDef;                     //增益Gain
    protected int mGammaMin, mGammaMax, mGammaDef;                  //伽马Gamma
    protected int mSaturationMin, mSaturationMax, mSaturationDef;   //饱和度Saturation
    protected int mHueMin, mHueMax, mHueDef;                        //色调Hue
    protected int mZoomMin, mZoomMax, mZoomDef;                     //变焦Zoom
    protected int mZoomRelMin, mZoomRelMax, mZoomRelDef;
    protected int mPowerlineFrequencyMin, mPowerlineFrequencyMax, mPowerlineFrequencyDef;
    protected int mMultiplierMin, mMultiplierMax, mMultiplierDef;
    protected int mMultiplierLimitMin, mMultiplierLimitMax, mMultiplierLimitDef;
    protected int mAnalogVideoStandardMin, mAnalogVideoStandardMax, mAnalogVideoStandardDef;
    protected int mAnalogVideoLockStateMin, mAnalogVideoLockStateMax, mAnalogVideoLockStateDef;
    // until here


    /**
     * the conctructor of this class should be call within the thread that has a looper
     * (UI thread or a thread that called Looper.prepare)
     * 这个类的conctructor构造器应该在线程中有一个活套打
     * （UI线程或线程称为尺蠖。准备）
     */
    public UVCCamera() {
        //jni so位置在 "/data/data/"+getPackageName()+"/lib/目录下"
        // /data/data/com.yeespec.microscope.master/lib/
        //        if (!isLoaded) {
        //            System.loadLibrary("usb100");       //导入usb100.so库文件,调用jni的c代码:
        //            System.loadLibrary("uvc");          //导入uvc.so库文件,调用jni的c代码:
        //            System.loadLibrary("UVCCamera");    //导入UVCCamera.so库文件,调用jni的c代码:
        //
        //            isLoaded = true;
        //        }
        mNativePtr = nativeCreate();    //获取当前连接的相机ID : id_camera ;
        mSupportedSize = null;
    }

    //    //用于检测 so模块是否存在，如果不存在，可以不调用so
    //    private  String checkJNISo(String soName)
    //    {
    //
    //        File filesDir = getFilesDir();
    //        if(filesDir!=null)
    //        {
    //            String dataPath = filesDir.getParentFile().getAbsolutePath();
    //            //jni so位置在 "/data/data/"+getPackageName()+"/lib/目录下"
    //            File f = new File(dataPath+"/lib/",soName);//"libndkstl.so");
    //            if(f!=null && f.exists())
    //            {
    //                return f.getAbsolutePath();
    //            }
    //        }
    //        return null;
    //    }

    /**
     * connect to a UVC camera
     * USB permission is necessary before this method is called
     *
     * @param ctrlBlock
     */
    public void open(final UsbControlBlock ctrlBlock) {
        mCtrlBlock = ctrlBlock;
        nativeConnect(mNativePtr,
                mCtrlBlock.getVenderId(), mCtrlBlock.getProductId(),
                mCtrlBlock.getFileDescriptor(),
                getUSBFSName(mCtrlBlock));
        if (mNativePtr != 0 && TextUtils.isEmpty(mSupportedSize)) {
            mSupportedSize = nativeGetSupportedSize(mNativePtr);
        }
        nativeSetPreviewSize(mNativePtr, DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT, DEFAULT_PREVIEW_MODE, DEFAULT_BANDWIDTH);
    }

    /**
     * set status callback
     *
     * @param callback
     */
    public void setStatusCallback(final IStatusCallback callback) {
        if (mNativePtr != 0) {
            nativeSetStatusCallback(mNativePtr, callback);
        }
    }

    /**
     * set button callback
     *
     * @param callback
     */
    public void setButtonCallback(final IButtonCallback callback) {
        if (mNativePtr != 0) {
            nativeSetButtonCallback(mNativePtr, callback);
        }
    }

    /**
     * close and release UVC camera
     */
    public void close() {
        // stopPreview();
        if (mNativePtr != 0) {
            nativeRelease(mNativePtr);
        }
        mCtrlBlock = null;
        mControlSupports = mProcSupports = 0;
        mCurrentPreviewMode = -1;
    }

    public UsbDevice getDevice() {
        return mCtrlBlock != null ? mCtrlBlock.getDevice() : null;
    }

    public String getDeviceName() {
        return mCtrlBlock != null ? mCtrlBlock.getDeviceName() : null;
    }

    public UsbControlBlock getUsbControlBlock() {
        return mCtrlBlock;
    }

    public synchronized String getSupportedSize() {
        return !TextUtils.isEmpty(mSupportedSize) ? mSupportedSize : (mSupportedSize = nativeGetSupportedSize(mNativePtr));
    }

    public Size getPreviewSize() {
        Size result = null;
        final List<Size> list = getSupportedSizeList();
        for (final Size sz : list) {
            if ((sz.width == mCurrentPreviewWidth)
                    || (sz.height == mCurrentPreviewHeight)) {
                result = sz;
                break;
            }
        }
        return result;
    }

    /**
     * Set preview size and preview mode
     *
     * @param width
     * @param height
     * @param mode   0:yuyv, other:MJPEG
     */
    public void setPreviewSize(final int width, final int height) {
        setPreviewSize(width, height, mCurrentPreviewMode, 0);
    }

    /**
     * Set preview size and preview mode
     *
     * @param width
     * @param height
     * @param mode   0:yuyv, other:MJPEG
     */
    public void setPreviewSize(final int width, final int height, final int mode) {
        setPreviewSize(width, height, mode, 0);
    }

    /**
     * Set preview size and preview mode
     * 设置图像的 预览大小 和 预览模式
     *
     * @param width     图像宽
     * @param height    图像高
     * @param mode      0:yuyv, other:MJPEG     图像数据编码格式 : yuyv或MJPEG
     * @param bandwidth [0.0f,1.0f]             带宽
     */
    public void setPreviewSize(final int width, final int height, final int mode, final float bandwidth) {
        if ((width == 0) || (height == 0))
            throw new IllegalArgumentException("invalid preview size");
        if (mNativePtr != 0) {
            final int result = nativeSetPreviewSize(mNativePtr, width, height, mode, bandwidth);
            if (result != 0)
                throw new IllegalArgumentException("Failed to set preview size");
            mCurrentPreviewMode = mode;
            mCurrentPreviewWidth = width;
            mCurrentPreviewHeight = height;
        }
    }

    public List<Size> getSupportedSizeList() {
        final int type = (mCurrentPreviewMode > 0) ? 6 : 4;
        return getSupportedSize(type, mSupportedSize);
    }

    public static List<Size> getSupportedSize(final int type, final String supportedSize) {
        final List<Size> result = new ArrayList<Size>();
        if (!TextUtils.isEmpty(supportedSize))
            try {
                final JSONObject json = new JSONObject(supportedSize);
                final JSONArray formats = json.getJSONArray("formats");
                final int format_nums = formats.length();
                for (int i = 0; i < format_nums; i++) {
                    final JSONObject format = formats.getJSONObject(i);
                    final int format_type = format.getInt("type");
                    if ((format_type == type) || (type == -1)) {
                        addSize(format, format_type, result);
                    }
                }
            } catch (final JSONException e) {
                e.printStackTrace();
            }
        return result;
    }

    private static final void addSize(final JSONObject format, final int type, final List<Size> size_list) throws JSONException {
        final JSONArray size = format.getJSONArray("size");
        final int size_nums = size.length();
        for (int j = 0; j < size_nums; j++) {
            final String[] sz = size.getString(j).split("x");
            try {
                size_list.add(new Size(type, j, Integer.parseInt(sz[0]), Integer.parseInt(sz[1])));
            } catch (final Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * set preview surface with SurfaceHolder</br>
     * you can use SurfaceHolder came from SurfaceView/GLSurfaceView
     * <p/>
     * 使用SurfaceHolder设置预览的surface
     * 你可以使用SurfaceView 或 glsurfaceview 的 SurfaceHolder来设置
     *
     * @param holder
     */
    public void setPreviewDisplay(final SurfaceHolder holder) {
        nativeSetPreviewDisplay(mNativePtr, holder.getSurface());
    }

    /**
     * set preview surface with SurfaceTexture.
     * this method require API >= 14
     * <p/>
     * 通过纹理表面SurfaceTexture来设置预览的surface ;
     * 这个方法需要 API >= 14
     *
     * @param texture
     */
    public void setPreviewTexture(final SurfaceTexture texture) {    // API >= 11
        final Surface surface = new Surface(texture);    // XXX API >= 14
        nativeSetPreviewDisplay(mNativePtr, surface);
    }

    /**
     * set preview surface with Surface
     * 直接使用surface设置 预览的 surface图像
     *
     * @param Surface
     */
    public void setPreviewDisplay(final Surface surface) {
        nativeSetPreviewDisplay(mNativePtr, surface);
    }

    /**
     * set frame callback
     * 设置框架回调  / 设置图像帧回调
     * 如果你需要帧数据字节缓冲区，您可以使用此回调接口uvccamera # setframecallback
     * (重要的借口)      重要分析步骤 2 :
     *
     * @param callback
     * @param pixelFormat
     */
    public void setFrameCallback(final IFrameCallback callback, final int pixelFormat) {
        if (mNativePtr != 0) {
            nativeSetFrameCallback(mNativePtr, callback, pixelFormat);
        }
    }

    /**
     * start preview
     * 开始预览图像 :
     */
    public void startPreview() {

        if (mCtrlBlock != null) {
            nativeStartPreview(mNativePtr);
        }
    }

    /**
     * stop preview
     * 停止预览图像 :
     */
    public void stopPreview() {
        // Log.e("CameraClient"," stopPreview1 ");
        setFrameCallback(null, 0);
        //2016.06.06 修改 : 修改 , 将图像预览的格式从PIXEL_FORMAT_RAW 改为PIXEL_FORMAT_YUV
        //        setFrameCallback(null, UVCCamera.PIXEL_FORMAT_YUV);
        if (mCtrlBlock != null) {
            //  Log.e("CameraClient"," stopPreview1111");
            nativeStopPreview(mNativePtr);
        }
    }

    /**
     * destroy UVCCamera object
     * 结束并销毁UVCCamera对象 ;
     */
    public void destroy() {
        close();
        if (mNativePtr != 0) {
            nativeDestroy(mNativePtr);
            mNativePtr = 0;
        }
    }

    // wrong result may return when you call this just after camera open.
    // it is better to wait several hundreads millseconds.
    public boolean checkSupportFlag(final long flag) {
        updateCameraParams();
        if ((flag & 0x80000000) == 0x80000000)
            return ((mProcSupports & flag) == (flag & 0x7ffffffF));
        else
            return (mControlSupports & flag) == flag;
    }

    //================================================================================
    //自动聚焦 :

    public synchronized void setAutoFocus(final boolean autoFocus) {
        if (mNativePtr != 0) {
            nativeSetAutoFocus(mNativePtr, autoFocus);
        }
    }

    public synchronized boolean getAutoFocus() {
        boolean result = true;
        if (mNativePtr != 0) {
            result = nativeGetAutoFocus(mNativePtr) > 0;
        }
        return result;
    }
    //================================================================================
    //聚焦

    /**
     * @param focus[%]
     */
    public synchronized void setFocus(final int focus) {
        if (mNativePtr != 0) {
            final float range = Math.abs(mFocusMax - mFocusMin);
            if (range > 0)
                nativeSetFocus(mNativePtr, (int) (focus / 100.f * range) + mFocusMin);
        }
    }

    /**
     * @param focus_abs
     * @return forcus[%]
     */
    public synchronized int getFocus(final int focus_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            nativeUpdateFocusLimit(mNativePtr);
            final float range = Math.abs(mFocusMax - mFocusMin);
            if (range > 0) {
                result = (int) ((focus_abs - mFocusMin) * 100.f / range);
            }
        }
        return result;
    }

    /**
     * @return focus[%]
     */
    public synchronized int getFocus() {
        return getFocus(nativeGetFocus(mNativePtr));
    }

    public synchronized void resetFocus() {
        if (mNativePtr != 0) {
            nativeSetFocus(mNativePtr, mFocusDef);
        }
    }

    //================================================================================
    //自动白平衡

    public synchronized void setAutoWhiteBlance(final boolean autoWhiteBlance) {
        if (mNativePtr != 0) {
            nativeSetAutoWhiteBlance(mNativePtr, autoWhiteBlance);
        }
    }

    public synchronized boolean getAutoWhiteBlance() {
        boolean result = true;
        if (mNativePtr != 0) {
            result = nativeGetAutoWhiteBlance(mNativePtr) > 0;
        }
        return result;
    }

    //================================================================================
    //白平衡

    /**
     * @param whiteBlance[%]
     */
    public synchronized void setWhiteBlance(final int whiteBlance) {
        if (mNativePtr != 0) {
            final float range = Math.abs(mWhiteBlanceMax - mWhiteBlanceMin);
            if (range > 0)
                nativeSetWhiteBlance(mNativePtr, (int) (whiteBlance / 100.f * range) + mWhiteBlanceMin);
        }
    }

    /**
     * @param whiteBlance_abs
     * @return whiteBlance[%]
     */
    public synchronized int getWhiteBlance(final int whiteBlance_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            nativeUpdateWhiteBlanceLimit(mNativePtr);
            final float range = Math.abs(mWhiteBlanceMax - mWhiteBlanceMin);
            if (range > 0) {
                result = (int) ((whiteBlance_abs - mWhiteBlanceMin) * 100.f / range);
            }
        }
        return result;
    }

    /**
     * @return white blance[%]
     */
    public synchronized int getWhiteBlance() {
        return getFocus(nativeGetWhiteBlance(mNativePtr));
    }

    public synchronized void resetWhiteBlance() {
        if (mNativePtr != 0) {
            nativeSetWhiteBlance(mNativePtr, mWhiteBlanceDef);
        }
    }
    //================================================================================
    //亮度

    /**
     * @param brightness[%]
     */
    public synchronized void setBrightness(final int brightness) {
        if (mNativePtr != 0) {
            Log.w(TAG, "mBrightnessMax=" + mBrightnessMax);
            Log.w(TAG, "mBrightnessMin=" + mBrightnessMin);
            final float range = Math.abs(mBrightnessMax - mBrightnessMin);
            if(camraType){
                nativeSetBrightness(mNativePtr, brightness);
            }else {
                if (range > 0) {
                    //  nativeSetBrightness(mNativePtr, (int) (brightness / 100.f * range) + mBrightnessMin);
                    nativeSetBrightness(mNativePtr, brightness);
                    Log.w(TAG, "setBrightness=" + brightness);
                }
            }
        }
    }

    /**
     * @param brightness_abs
     * @return brightness[%]
     */
    public synchronized int getBrightness(final int brightness_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            nativeUpdateBrightnessLimit(mNativePtr);
            final float range = Math.abs(mBrightnessMax - mBrightnessMin);
            if (range > 0) {
                Log.w(TAG, "getBrightness=" + brightness_abs);
                result = brightness_abs;
            }
        }
        return result;
    }

    /**
     * @return brightness[%]
     */
    public synchronized int getBrightness() {
        return getBrightness(nativeGetBrightness(mNativePtr));
    }

    //读取最大亮度值
    public synchronized int getMaxBrightness() {

        if (mNativePtr != 0) {
            return mBrightnessMax;
        }
        return 0;
    }

    //读取最大值
    public static final int BRIGHTNESS_TYPE = 1;//亮度
    public static final int ISO_TYPE = 2;//

    public synchronized int getMaxNuberType(int Type) {

        if (mNativePtr != 0) {
            switch (Type) {
                case BRIGHTNESS_TYPE:
                    return mBrightnessMax;

                case ISO_TYPE:
                    return mGainMax;
            }
        }
        return 0;
    }

    public synchronized void resetBrightness() {
        if (mNativePtr != 0) {
            nativeSetBrightness(mNativePtr, mBrightnessDef);
        }
    }

    //================================================================================
    //对比度 :

    /**
     * @param contrast[%]
     */
    //    public synchronized void setContrast(final int contrast) {
    //        if (mNativePtr != 0) {
    //            nativeUpdateContrastLimit(mNativePtr);
    //            final float range = Math.abs(mContrastMax - mContrastMin);
    //            if (range > 0)
    //                nativeSetContrast(mNativePtr, (int) (contrast / 100.f * range) + mContrastMin);
    //        }
    //    }
    public synchronized void setContrast(final int contrast) {
        if (mNativePtr != 0) {
            nativeSetContrast(mNativePtr, contrast);
        }
    }

    public synchronized void setColor(final int color) {
        if (mNativePtr != 0) {
            nativeSetColor(mNativePtr, color);
        }
    }

    /**
     * @param contrast_abs
     * @return contrast[%]
     */
    public synchronized int getContrast(final int contrast_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            final float range = Math.abs(mContrastMax - mContrastMin);
            if (range > 0) {
                result = (int) ((contrast_abs - mContrastMin) * 100.f / range);
            }
        }
        return result;
    }

    /**
     * @return contrast[%]
     */
    public synchronized int getContrast() {
        return getContrast(nativeGetContrast(mNativePtr));
    }

    public synchronized void resetContrast() {
        if (mNativePtr != 0) {
            nativeSetContrast(mNativePtr, mContrastDef);
        }
    }

    //================================================================================
    //锐度

    /**
     * @param sharpness[%]
     */
    public synchronized void setSharpness(final int sharpness) {
        if (mNativePtr != 0) {
            final float range = Math.abs(mSharpnessMax - mSharpnessMin);
            if (range > 0)
                nativeSetSharpness(mNativePtr, (int) (sharpness / 100.f * range) + mSharpnessMin);
        }
    }

    /**
     * @param sharpness_abs
     * @return sharpness[%]
     */
    public synchronized int getSharpness(final int sharpness_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            nativeUpdateSharpnessLimit(mNativePtr);
            final float range = Math.abs(mSharpnessMax - mSharpnessMin);
            if (range > 0) {
                result = (int) ((sharpness_abs - mSharpnessMin) * 100.f / range);
            }
        }
        return result;
    }

    /**
     * @return sharpness[%]
     */
    public synchronized int getSharpness() {
        return getSharpness(nativeGetSharpness(mNativePtr));
    }

    public synchronized void resetSharpness() {
        if (mNativePtr != 0) {
            nativeSetSharpness(mNativePtr, mSharpnessDef);
        }
    }

    //================================================================================
    //增益

    /**
     * @param gain[%]
     */
    public synchronized void setGain(final int gain) {
        if (mNativePtr != 0) {

            if(camraType){
                nativeSetGain(mNativePtr, gain);
               // Log.w(TAG, "setgain=" + gain);
            }else {
                final float range = Math.abs(mGainMax - mGainMin);
                if (range > 0) {
                    nativeSetGain(mNativePtr, (int) (gain / 100.f * range) + mGainMin);

                   // Log.w(TAG, "setgain=" + ((int) (gain / 100.f * range) + mGainMin));
                }
            }
        }
    }

    /**
     * @param gain_abs
     * @return gain[%]
     */
    public synchronized int getGain(final int gain_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            nativeUpdateGainLimit(mNativePtr);
            if(camraType){
                return  gain_abs;
            }else {
                final float range = Math.abs(mGainMax - mGainMin);
                if (range > 0) {
                    result = (int) ((gain_abs - mGainMin) * 100.f / range);
                    Log.w(TAG, "gain=" + result);
                }
            }
        }
        return result;
    }

    /**
     * @return gain[%]
     */
    public synchronized int getGain() {
        return getGain(nativeGetGain(mNativePtr));
    }

    public synchronized void resetGain() {
        if (mNativePtr != 0) {
            nativeSetGain(mNativePtr, mGainDef);
        }
    }

    //================================================================================
    //伽马

    /**
     * @param gamma[%]
     */
    //    public synchronized void setGamma(final int gamma) {
    //        if (mNativePtr != 0) {
    //            final float range = Math.abs(mGammaMax - mGammaMin);
    //            if (range > 0)
    //                nativeSetGamma(mNativePtr, (int) (gamma / 100.f * range) + mGammaMin);
    //        }
    //    }
    public synchronized void setGamma(final int gamma) {
        if (mNativePtr != 0) {
            nativeSetGamma(mNativePtr, gamma);
        }
    }

    /**
     * @param gamma_abs
     * @return gamma[%]
     */
    public synchronized int getGamma(final int gamma_abs) {
        int result = 0;
        //        if (mNativePtr != 0) {
        //            nativeUpdateGammaLimit(mNativePtr);
        //            final float range = Math.abs(mGammaMax - mGammaMin);
        //            if (range > 0) {
        //                result = (int) ((gamma_abs - mGammaMin) * 100.f / range);
        //            }
        //        }
        result = nativeGetGamma(mNativePtr);
        ///
        return result;
    }

    /**
     * @return gamma[%]
     */
    public synchronized int getGamma() {
        return getGamma(nativeGetGamma(mNativePtr));
    }

    public synchronized void resetGamma() {
        if (mNativePtr != 0) {
            nativeSetGamma(mNativePtr, mGammaDef);
        }
    }

    public synchronized void setGray(final int gray) {
        if (mNativePtr != 0) {
            nativeSetGray(mNativePtr, gray);
        }
    }
    //================================================================================
    //饱和度

    /**
     * @param saturation[%]
     */
    public synchronized void setSaturation(final int saturation) {
        if (mNativePtr != 0) {
            nativeSetSaturation(mNativePtr, saturation);
        }
    }

    //	public synchronized void setSaturation(final int saturation) {
    //    	if (mNativePtr != 0) {
    // 		   final float range = Math.abs(mSaturationMax - mSaturationMin);
    // 		   if (range > 0)
    // 			   nativeSetSaturation(mNativePtr, (int)(saturation / 100.f * range) + mSaturationMin);
    //    	}
    //    }

    /**
     * @param saturation_abs
     * @return saturation[%]
     */
    public synchronized int getSaturation(final int saturation_abs) {
        int result = 0;
        //if (mNativePtr != 0) {
        //   nativeUpdateSaturationLimit(mNativePtr);
        //    final float range = Math.abs(mSaturationMax - mSaturationMin);
        //     if (range > 0) {
        //         result = (int) ((saturation_abs - mSaturationMin) * 100.f / range);
        //     }
        //  }
        result = nativeGetSaturation(mNativePtr);
        return result;
    }

    /**
     * @return saturation[%]
     */
    public synchronized int getSaturation() {
        return getSaturation(nativeGetSaturation(mNativePtr));
    }

    public synchronized void resetSaturation() {
        if (mNativePtr != 0) {
            nativeSetSaturation(mNativePtr, mSaturationDef);
        }
    }
    //================================================================================
    //色调

    /**
     * @param hue[%]
     */
    public synchronized void setHue(final int hue) {
        if (mNativePtr != 0) {
            final float range = Math.abs(mHueMax - mHueMin);
            if (range > 0)
                nativeSetHue(mNativePtr, (int) (hue / 100.f * range) + mHueMin);
        }
    }

    /**
     * @param hue_abs
     * @return hue[%]
     */
    public synchronized int getHue(final int hue_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            nativeUpdateHueLimit(mNativePtr);
            final float range = Math.abs(mHueMax - mHueMin);
            if (range > 0) {
                result = (int) ((hue_abs - mHueMin) * 100.f / range);
            }
        }
        return result;
    }

    /**
     * @return hue[%]
     */
    public synchronized int getHue() {
        return getHue(nativeGetHue(mNativePtr));
    }

    public synchronized void resetHue() {
        if (mNativePtr != 0) {
            nativeSetHue(mNativePtr, mSaturationDef);
        }
    }

    //================================================================================
    //电力线载波频率 :

    public void setPowerlineFrequency(final int frequency) {
        if (mNativePtr != 0)
            nativeSetPowerlineFrequency(mNativePtr, frequency);
    }

    public int getPowerlineFrequency() {
        return nativeGetPowerlineFrequency(mNativePtr);
    }

    //================================================================================
    //变焦 :

    /**
     * this may not work well with some combination of camera and device
     *
     * @param zoom[%]
     */
    public synchronized void setZoom(final int zoom) {
        if (mNativePtr != 0) {
            final float range = Math.abs(mZoomMax - mZoomMin);
            if (range > 0) {
                final int z = (int) (zoom / 100.f * range) + mZoomMin;
                // 			   Log.d(TAG, "setZoom:zoom=" + zoom + " ,value=" + z);
                nativeSetZoom(mNativePtr, z);
            }
        }
    }

    /**
     * @param zoom_abs
     * @return zoom[%]
     */
    public synchronized int getZoom(final int zoom_abs) {
        int result = 0;
        if (mNativePtr != 0) {
            nativeUpdateZoomLimit(mNativePtr);
            final float range = Math.abs(mZoomMax - mZoomMin);
            if (range > 0) {
                result = (int) ((zoom_abs - mZoomMin) * 100.f / range);
            }
        }
        return result;
    }

    /**
     * @return zoom[%]
     */
    public synchronized int getZoom() {
        return getZoom(nativeGetZoom(mNativePtr));
    }

    public synchronized void resetZoom() {
        if (mNativePtr != 0) {
            nativeSetZoom(mNativePtr, mZoomDef);
        }
    }

    //================================================================================
    public synchronized void updateCameraParams() {
        if (mNativePtr != 0) {
            if ((mControlSupports == 0) || (mProcSupports == 0)) {
                // サポートしている機能フラグを取得     //取得支持的功能标志
                if (mControlSupports == 0)
                    mControlSupports = nativeGetCtrlSupports(mNativePtr);
                if (mProcSupports == 0)
                    mProcSupports = nativeGetProcSupports(mNativePtr);
                // 設定値を取得       //获得设定值 :
                if ((mControlSupports != 0) && (mProcSupports != 0)) {
                    nativeUpdateBrightnessLimit(mNativePtr);
                    nativeUpdateContrastLimit(mNativePtr);
                    nativeUpdateSharpnessLimit(mNativePtr);
                    nativeUpdateGainLimit(mNativePtr);
                    nativeUpdateGammaLimit(mNativePtr);
                    nativeUpdateSaturationLimit(mNativePtr);
                    nativeUpdateHueLimit(mNativePtr);
                    nativeUpdateZoomLimit(mNativePtr);
                    nativeUpdateWhiteBlanceLimit(mNativePtr);
                    nativeUpdateFocusLimit(mNativePtr);
                }
                if (DEBUG) {
                    dumpControls(mControlSupports);
                    dumpProc(mProcSupports);
                    Log.v(TAG, String.format("Brightness:min=%d,max=%d,def=%d", mBrightnessMin, mBrightnessMax, mBrightnessDef));
                    Log.v(TAG, String.format("Contrast:min=%d,max=%d,def=%d", mContrastMin, mContrastMax, mContrastDef));
                    Log.v(TAG, String.format("Sharpness:min=%d,max=%d,def=%d", mSharpnessMin, mSharpnessMax, mSharpnessDef));
                    Log.v(TAG, String.format("Gain:min=%d,max=%d,def=%d", mGainMin, mGainMax, mGainDef));
                    Log.v(TAG, String.format("Gamma:min=%d,max=%d,def=%d", mGammaMin, mGammaMax, mGammaDef));
                    Log.v(TAG, String.format("Saturation:min=%d,max=%d,def=%d", mSaturationMin, mSaturationMax, mSaturationDef));
                    Log.v(TAG, String.format("Hue:min=%d,max=%d,def=%d", mHueMin, mHueMax, mHueDef));
                    Log.v(TAG, String.format("Zoom:min=%d,max=%d,def=%d", mZoomMin, mZoomMax, mZoomDef));
                    Log.v(TAG, String.format("WhiteBlance:min=%d,max=%d,def=%d", mWhiteBlanceMin, mWhiteBlanceMax, mWhiteBlanceDef));
                    Log.v(TAG, String.format("Focus:min=%d,max=%d,def=%d", mFocusMin, mFocusMax, mFocusDef));
                }
            }
        } else {
            mControlSupports = mProcSupports = 0;
        }
    }

    private static final String[] SUPPORTS_CTRL = {
            "D0:  Scanning Mode",
            "D1:  Auto-Exposure Mode",
            "D2:  Auto-Exposure Priority",
            "D3:  Exposure Time (Absolute)",
            "D4:  Exposure Time (Relative)",
            "D5:  Focus (Absolute)",
            "D6:  Focus (Relative)",
            "D7:  Iris (Absolute)",
            "D8:  Iris (Relative)",
            "D9:  Zoom (Absolute)",
            "D10: Zoom (Relative)",
            "D11: PanTilt (Absolute)",
            "D12: PanTilt (Relative)",
            "D13: Roll (Absolute)",
            "D14: Roll (Relative)",
            "D15: Reserved",
            "D16: Reserved",
            "D17: Focus, Auto",
            "D18: Privacy",
            "D19: Focus, Simple",
            "D20: Window",
            "D21: Region of Interest",
            "D22: Reserved, set to zero",
            "D23: Reserved, set to zero",
    };

    private static final String[] SUPPORTS_PROC = {
            "D0: Brightness",
            "D1: Contrast",
            "D2: Hue",
            "D3: Saturation",
            "D4: Sharpness",
            "D5: Gamma",
            "D6: White Balance Temperature",
            "D7: White Balance Component",
            "D8: Backlight Compensation",
            "D9: Gain",
            "D10: Power Line Frequency",
            "D11: Hue, Auto",
            "D12: White Balance Temperature, Auto",
            "D13: White Balance Component, Auto",
            "D14: Digital Multiplier",
            "D15: Digital Multiplier Limit",
            "D16: Analog Video Standard",
            "D17: Analog Video Lock Status",
            "D18: Contrast, Auto",
            "D19: Reserved. Set to zero",
            "D20: Reserved. Set to zero",
            "D21: Reserved. Set to zero",
            "D22: Reserved. Set to zero",
            "D23: Reserved. Set to zero",
    };

    private static final void dumpControls(final long controlSupports) {
        Log.i(TAG, String.format("controlSupports=%x", controlSupports));
        for (int i = 0; i < SUPPORTS_CTRL.length; i++) {
            Log.i(TAG, SUPPORTS_CTRL[i] + ((controlSupports & (0x1 << i)) != 0 ? "=enabled" : "=disabled"));
        }
    }

    private static final void dumpProc(final long procSupports) {
        Log.i(TAG, String.format("procSupports=%x", procSupports));
        for (int i = 0; i < SUPPORTS_PROC.length; i++) {
            Log.i(TAG, SUPPORTS_PROC[i] + ((procSupports & (0x1 << i)) != 0 ? "=enabled" : "=disabled"));
        }
    }

    private final String getUSBFSName(final UsbControlBlock ctrlBlock) {
        String result = null;
        final String name = ctrlBlock.getDeviceName();
        final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
        if ((v != null) && (v.length > 2)) {
            final StringBuilder sb = new StringBuilder(v[0]);
            for (int i = 1; i < v.length - 2; i++)
                sb.append("/").append(v[i]);
            result = sb.toString();
        }
        if (TextUtils.isEmpty(result)) {
            Log.w(TAG, "failed to get USBFS path, try to use default path:" + name);
            result = DEFAULT_USBFS;
        }
        return result;
    }

    /**
     * start movie capturing(this should call while previewing)
     *
     * @param surface
     */
    public void startCapture(final Surface surface) {
        if (mCtrlBlock != null && surface != null) {
            nativeSetCaptureDisplay(mNativePtr, surface);
        } else
            throw new NullPointerException("startCapture");
    }

    public void reLeaseCamera() {
        if (mCtrlBlock != null) {
            nativeRelease(mNativePtr);
        }
    }

    /**
     * stop movie capturing
     */
    public void stopCapture() {
        if (mCtrlBlock != null) {
            nativeSetCaptureDisplay(mNativePtr, null);
        }
    }

   /* public double getFreShness(int heigh,int width,int bpp,String buffs){
        double mresult=0;
        if (mCtrlBlock != null) {

            mresult = nativeGetFreShness(heigh, width, 24, buffs);
        }
        return mresult;
    }

    private static final native  double nativeGetFreShness(final int heigh,final int width,final int bpp, final String buffs);
*/

    // #nativeCreate and #nativeDestroy are not static methods. //nativeCreate和nativedestroy不是静态的方法。
    private final native long nativeCreate();

    private final native void nativeDestroy(final long id_camera);

    //**********************************************************************
    private static final native int nativeConnect(final long id_camera, final int venderId, final int productId, final int fileDescriptor, String usbfs);

    private static final native int nativeRelease(final long id_camera);

    //**********************************************************************
    private static final native int nativeSetStatusCallback(final long mNativePtr, final IStatusCallback callback);

    private static final native int nativeSetButtonCallback(final long mNativePtr, final IButtonCallback callback);

    //**********************************************************************
    private static final native int nativeSetPreviewSize(final long id_camera, final int width, final int height, final int mode, final float bandwidth);

    private static final native String nativeGetSupportedSize(final long id_camera);

    private static final native int nativeStartPreview(final long id_camera);

    private static final native int nativeStopPreview(final long id_camera);

    /**
     * set frame callback
     * 设置框架回调  / 设置图像帧回调
     * (重要的借口)
     *
     * @param mNativePtr
     * @param callback
     * @param pixelFormat
     * @return
     */
    private static final native int nativeSetFrameCallback(final long mNativePtr, final IFrameCallback callback, final int pixelFormat);

    //**********************************************************************

    /**
     * 通过这个底层C++的接口来设置 预览surface :
     * (重要的借口)
     *
     * @param id_camera
     * @param surface
     * @return
     */
    private static final native int nativeSetGray(final long id_camera, final int gray);


    private static final native int nativeSetPreviewDisplay(final long id_camera, final Surface surface);

    private static final native int nativeSetCaptureDisplay(final long id_camera, final Surface surface);

    //**********************************************************************
    private static final native long nativeGetCtrlSupports(final long id_camera);

    private static final native long nativeGetProcSupports(final long id_camera);

    //********************************************************************** 本地更新扫描模式限制
    private final native int nativeUpdateScanningModeLimit(final long id_camera);

    private static final native int nativeSetScanningMode(final long id_camera, final int scanning_mode);

    private static final native int nativeGetScanningMode(final long id_camera);

    //********************************************************************** 本地更新曝光模式限制
    private final native int nativeUpdateExposureModeLimit(final long id_camera);

    private static final native int nativeSetExposureMode(final long id_camera, final int exposureMode);

    private static final native int nativeGetExposureMode(final long id_camera);

    //********************************************************************** 本地更新曝光优先级限制
    private final native int nativeUpdateExposurePriorityLimit(final long id_camera);

    private static final native int nativeSetExposurePriority(final long id_camera, final int priority);

    private static final native int nativeGetExposurePriority(final long id_camera);

    //********************************************************************** 本地更新曝光限制
    private final native int nativeUpdateExposureLimit(final long id_camera);

    private static final native int nativeSetExposure(final long id_camera, final int exposure);

    private static final native int nativeGetExposure(final long id_camera);

    //********************************************************************** 本地更新的曝光与限制
    private final native int nativeUpdateExposureRelLimit(final long id_camera);

    private static final native int nativeSetExposureRel(final long id_camera, final int exposure_rel);

    private static final native int nativeGetExposureRel(final long id_camera);

    //********************************************************************** 本地更新自动聚焦限制
    private final native int nativeUpdateAutoFocusLimit(final long id_camera);

    private static final native int nativeSetAutoFocus(final long id_camera, final boolean autofocus);

    private static final native int nativeGetAutoFocus(final long id_camera);

    //********************************************************************** 本地更新焦点限制
    private final native int nativeUpdateFocusLimit(final long id_camera);

    private static final native int nativeSetFocus(final long id_camera, final int focus);

    private static final native int nativeGetFocus(final long id_camera);

    //********************************************************************** 天然更新的重点与限制
    private final native int nativeUpdateFocusRelLimit(final long id_camera);

    private static final native int nativeSetFocusRel(final long id_camera, final int focus_rel);

    private static final native int nativeGetFocusRel(final long id_camera);

    //********************************************************************** 本地更新虹膜限制
    private final native int nativeUpdateIrisLimit(final long id_camera);

    private static final native int nativeSetIris(final long id_camera, final int iris);

    private static final native int nativeGetIris(final long id_camera);

    //********************************************************************** 天然更新的虹膜与限制
    private final native int nativeUpdateIrisRelLimit(final long id_camera);

    private static final native int nativeSetIrisRel(final long id_camera, final int iris_rel);

    private static final native int nativeGetIrisRel(final long id_camera);

    //********************************************************************** 本地更新泛限制
    private final native int nativeUpdatePanLimit(final long id_camera);

    private static final native int nativeSetPan(final long id_camera, final int pan);

    private static final native int nativeGetPan(final long id_camera);

    //********************************************************************** 天然更新Pan Rel Limit
    private final native int nativeUpdatePanRelLimit(final long id_camera);

    private static final native int nativeSetPanRel(final long id_camera, final int pan_rel);

    private static final native int nativeGetPanRel(final long id_camera);

    //********************************************************************** 本地更新倾斜极限
    private final native int nativeUpdateTiltLimit(final long id_camera);

    private static final native int nativeSetTilt(final long id_camera, final int tilt);

    private static final native int nativeGetTilt(final long id_camera);

    //********************************************************************** 天然更新的倾斜与限制
    private final native int nativeUpdateTiltRelLimit(final long id_camera);

    private static final native int nativeSetTiltRel(final long id_camera, final int tilt_rel);

    private static final native int nativeGetTiltRel(final long id_camera);

    //********************************************************************** 本地更新辊限制
    private final native int nativeUpdateRollLimit(final long id_camera);

    private static final native int nativeSetRoll(final long id_camera, final int roll);

    private static final native int nativeGetRoll(final long id_camera);

    //********************************************************************** 天然更新辊相对极限
    private final native int nativeUpdateRollRelLimit(final long id_camera);

    private static final native int nativeSetRollRel(final long id_camera, final int roll_rel);

    private static final native int nativeGetRollRel(final long id_camera);

    //********************************************************************** 天然更新自动White Blance Limit
    private final native int nativeUpdateAutoWhiteBlanceLimit(final long id_camera);

    private static final native int nativeSetAutoWhiteBlance(final long id_camera, final boolean autoWhiteBlance);

    private static final native int nativeGetAutoWhiteBlance(final long id_camera);

    //********************************************************************** 天然更新自动白平衡成分的限制
    private final native int nativeUpdateAutoWhiteBlanceCompoLimit(final long id_camera);

    private static final native int nativeSetAutoWhiteBlanceCompo(final long id_camera, final boolean autoWhiteBlanceCompo);

    private static final native int nativeGetAutoWhiteBlanceCompo(final long id_camera);

    //********************************************************************** 天然更新白平衡极限
    private final native int nativeUpdateWhiteBlanceLimit(final long id_camera);

    private static final native int nativeSetWhiteBlance(final long id_camera, final int whiteBlance);

    private static final native int nativeGetWhiteBlance(final long id_camera);

    //********************************************************************** 天然更新白平衡成分的限制
    private final native int nativeUpdateWhiteBlanceCompoLimit(final long id_camera);

    private static final native int nativeSetWhiteBlanceCompo(final long id_camera, final int whiteBlance_compo);

    private static final native int nativeGetWhiteBlanceCompo(final long id_camera);

    //********************************************************************** 天然更新背光补偿极限
    private final native int nativeUpdateBacklightCompLimit(final long id_camera);

    private static final native int nativeSetBacklightComp(final long id_camera, final int backlight_comp);

    private static final native int nativeGetBacklightComp(final long id_camera);

    //********************************************************************** 本地更新亮度限制
    private final native int nativeUpdateBrightnessLimit(final long id_camera);

    private static final native int nativeSetBrightness(final long id_camera, final int brightness);

    private static final native int nativeGetBrightness(final long id_camera);

    //********************************************************************** 本地更新对比度限制
    private final native int nativeUpdateContrastLimit(final long id_camera);

    private static final native int nativeSetContrast(final long id_camera, final int contrast);

    private static final native int nativeGetContrast(final long id_camera);

    //********************************************************************** 本地更新清晰度限制
    private final native int nativeUpdateSharpnessLimit(final long id_camera);

    private static final native int nativeSetSharpness(final long id_camera, final int sharpness);

    private static final native int nativeGetSharpness(final long id_camera);

    //********************************************************************** 本地更新增益限制
    private final native int nativeUpdateGainLimit(final long id_camera);

    private static final native int nativeSetGain(final long id_camera, final int gain);

    private static final native int nativeGetGain(final long id_camera);

    //********************************************************************** 本地更新伽玛限制
    private final native int nativeUpdateGammaLimit(final long id_camera);

    private static final native int nativeSetGamma(final long id_camera, final int gamma);

    private static final native int nativeGetGamma(final long id_camera);

    //********************************************************************** 本地更新饱和限制
    private final native int nativeUpdateSaturationLimit(final long id_camera);

    private static final native int nativeSetSaturation(final long id_camera, final int saturation);


    private static final native int nativeGetSaturation(final long id_camera);

    //********************************************************************** 本地更新颜色限制
    private final native int nativeUpdateHueLimit(final long id_camera);

    private static final native int nativeSetHue(final long id_camera, final int hue);

    private static final native int nativeGetHue(final long id_camera);

    //********************************************************************** 天然更新的电力线频率的限制
    private final native int nativeUpdatePowerlineFrequencyLimit(final long id_camera);

    private static final native int nativeSetPowerlineFrequency(final long id_camera, final int frequency);

    private static final native int nativeGetPowerlineFrequency(final long id_camera);

    //********************************************************************** 本地更新缩放限制
    private final native int nativeUpdateZoomLimit(final long id_camera);

    private static final native int nativeSetZoom(final long id_camera, final int zoom);

    private static final native int nativeGetZoom(final long id_camera);

    //********************************************************************** 天然更新的变焦相对极限
    private final native int nativeUpdateZoomRelLimit(final long id_camera);

    private static final native int nativeSetZoomRel(final long id_camera, final int zoom_rel);

    private static final native int nativeGetZoomRel(final long id_camera);

    //********************************************************************** 本地更新数字乘法器限制
    private final native int nativeUpdateDigitalMultiplierLimit(final long id_camera);

    private static final native int nativeSetDigitalMultiplier(final long id_camera, final int multiplier);

    private static final native int nativeGetDigitalMultiplier(final long id_camera);

    //********************************************************************** 本地更新数字乘法器极限
    private final native int nativeUpdateDigitalMultiplierLimitLimit(final long id_camera);

    private static final native int nativeSetDigitalMultiplierLimit(final long id_camera, final int multiplier_limit);

    private static final native int nativeGetDigitalMultiplierLimit(final long id_camera);

    //********************************************************************** 本地更新模拟视频标准限制
    private final native int nativeUpdateAnalogVideoStandardLimit(final long id_camera);

    private static final native int nativeSetAnalogVideoStandard(final long id_camera, final int standard);

    private static final native int nativeGetAnalogVideoStandard(final long id_camera);

    //********************************************************************** 本地更新模拟视频锁定状态限制
    private final native int nativeUpdateAnalogVideoLockStateLimit(final long id_camera);

    private static final native int nativeSetAnalogVideoLoackState(final long id_camera, final int state);

    private static final native int nativeGetAnalogVideoLoackState(final long id_camera);

    //********************************************************************** 本地更新隐私限制
    private final native int nativeUpdatePrivacyLimit(final long id_camera);

    private static final native int nativeSetPrivacy(final long id_camera, final boolean privacy);

    private static final native int nativeGetPrivacy(final long id_camera);

    //**********************************************************************
    private static final native int nativeSetColor(final long id_camera, final int contrast);
    //**********************************************************************
}
