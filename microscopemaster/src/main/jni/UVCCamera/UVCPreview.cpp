/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: UVCPreview.cpp
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

#include <stdlib.h>
#include <linux/time.h>
#include <unistd.h>
#include <math.h>
#include "utilbase.h"
#include "UVCPreview.h"
#include "libuvc_internal.h"

#define    LOCAL_DEBUG 0
#define MAX_FRAME 8
#define PREVIEW_PIXEL_BYTES 4    // RGBA/RGBX
#define FRAME_POOL_SZ MAX_FRAME + 2
int mGray = 100;
uint32_t setcolor =0;
UVCPreview::UVCPreview(uvc_device_handle_t *devh)
        : mPreviewWindow(NULL),
          mCaptureWindow(NULL),
          mDeviceHandle(devh),
          requestWidth(DEFAULT_PREVIEW_WIDTH),
          requestHeight(DEFAULT_PREVIEW_HEIGHT),
          requestFps(DEFAULT_PREVIEW_FPS),
          requestMode(DEFAULT_PREVIEW_MODE),
          requestBandwidth(DEFAULT_BANDWIDTH),
          frameWidth(DEFAULT_PREVIEW_WIDTH),
          frameHeight(DEFAULT_PREVIEW_HEIGHT),
          frameBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * 2),    // YUYV
          frameMode(0),
          previewBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * PREVIEW_PIXEL_BYTES),
          previewFormat(WINDOW_FORMAT_RGBA_8888),
          mIsRunning(false),
          mIsCapturing(false),
          captureQueu(NULL),
          mFrameCallbackObj(NULL),
          mFrameCallbackFunc(NULL),
          callbackPixelBytes(2) {          //

    ENTER();
    pthread_cond_init(&preview_sync, NULL);
    pthread_mutex_init(&preview_mutex, NULL);
//
    pthread_cond_init(&capture_sync, NULL);
    pthread_mutex_init(&capture_mutex, NULL);
//	
    pthread_mutex_init(&pool_mutex, NULL);
    changeColor(255, 255, 255); //init recolor
    EXIT();
}

UVCPreview::~UVCPreview() {

    ENTER();
    if (mPreviewWindow)
        ANativeWindow_release(mPreviewWindow);
    mPreviewWindow = NULL;
    if (mCaptureWindow)
        ANativeWindow_release(mCaptureWindow);
    mCaptureWindow = NULL;
    clearPreviewFrame();
    clearCaptureFrame();
    clear_pool();
    pthread_mutex_destroy(&preview_mutex);
    pthread_cond_destroy(&preview_sync);
    pthread_mutex_destroy(&capture_mutex);
    pthread_cond_destroy(&capture_sync);
    pthread_mutex_destroy(&pool_mutex);
    EXIT();
}

/**
 * get uvc_frame_t from frame pool
 * if pool is empty, create new frame
 * this function does not confirm the frame size
 * and you may need to confirm the size
 从池uvc_frame_t框架
 如果池是空的，创建新的框架
 此功能不确认帧大小
 您可能需要确认大小
 */
uvc_frame_t *UVCPreview::get_frame(size_t data_bytes) {
    uvc_frame_t *frame = NULL;
    pthread_mutex_lock(&pool_mutex);
    {
        if (!mFramePool.isEmpty()) {    //get_frame()
            frame = mFramePool.last();  //get_frame()
        } else {
            //2016.09.14 新增 : 重新清空数据帧池 , 防止内存溢出 ;
            LOGW("test mFramePool.isEmpty()");
            /*
		    init_pool( data_bytes);

		    if (!mFramePool.isEmpty()) {    //get_frame()
                frame = mFramePool.last();  //get_frame()
                }
          */
        }
    }
    pthread_mutex_unlock(&pool_mutex);
    if UNLIKELY(!frame)
    {
        LOGW("test : allocate new frame");
        frame = uvc_allocate_frame(data_bytes);
    }
    return frame;
}

int wideTab[256][256];

int intgrayMapMethod(int gray, int wide) {
    int result = 0;
    if (gray <= wide) {
        return 0;
    } else if (gray > wide) {
        int g = int(gray * (255.0 / (255.0 - wide * 1.0)));

        if (g > 255)
            g = 255;
        return g;
    }

}

int twoPolesMethod(int gray, int wide) {
    int result = 0;

    if (gray <= wide) {
        int n = wide / 3;
        if (gray < n) {
            result = gray - int(gray * 1.0 * 0.3);
        } else if(gray>n){
            if(gray<n*2){
                result = gray - int(gray * 1.0 * 0.3);
            } else{
                result = gray - int(gray * 1.0 * 0.3);
            }
        }
        if (result < 0)
            result = 0;
        return result;
    } else if (gray > wide) {
       
        int n = (255-wide)/3;
        if(gray>n*3){
            result= gray + int(gray * 1.0 * 0.6);
        } else if(gray>n*2){
            result = gray + int(gray * 1.0 * 0.6);
        } else{
            result = gray + int(gray * 1.0 * 0.6);
        }
        if (result > 255)
            result = 255;
        return result;
    }

}

//创建伽马映射表
void creatWideTab() {
    for (int i = 1; i < 256; i++) {
        for (int j = 0; j < 256; j++) {
            //int pos = twoPolesMethod(j, i);
            int pos =0;
            if(i==100){
                pos = j;
            } else
             pos = int(255*pow((j*1.0/255.0),(i*1.0/100.0)));


            if (pos > 255) {
                pos = 255;
            } else if (pos < 0) {
                pos = 0;
            }
            wideTab[i][j] = pos;
        }

    }
}


//调用映射表，改变数据内容

void widePro(unsigned char *src, int wide, int len) {
    if (src) {
        for (int temp = 0; temp < len / 2; ++temp) {
            int pos = wideTab[wide][(src[temp])];
            /* if (pos>255)
             {
                 pos=255;
             }
             if (pos<0)
             {
                 pos=0;
             }*/
            src[temp] = pos;
            //src[temp]= src[temp];

        }
        for (int temp = len / 2; temp < len; ++temp) {
            int pos = wideTab[wide][(src[temp])];
            /*  if (pos>255)
              {
                  pos=255;
              }
              if (pos<0)
              {
                  pos=0;
              }*/
            src[temp]=pos;
           // src[temp] = src[temp];
        }
    }
}


void UVCPreview::recycle_frame(uvc_frame_t *frame) {

    pthread_mutex_lock(&pool_mutex);
    {

        // LOGW("test :++++++++++++++++++recycle_frame:  clear_pool %d",mFramePool.size());
        if (LIKELY(mFramePool.size() < FRAME_POOL_SZ)) {    //recycle_frame()

            mFramePool.put(frame);      //recycle_frame()

            frame = NULL;
        } else {

            // LOGW("test : mFramePool.size() > 1 FRAME_POOL_SZ");
            //2016.09.14 新增 : 重新清空数据帧池 , 防止内存溢出 ;
            //20170328修改为不用等待获得锁
            /* const int n = mFramePool.size();    //clear_pool()
             for (int i = 0; i < n; i++) {
                 uvc_free_frame(mFramePool[i]);  //clear_pool()
             }
             mFramePool.clear();     //clear_pool()
             mFramePool.put(frame);      //recycle_frame()*/
            //20180425
            if(!frame){
                frame = mFramePool.last();
            }
            //20180413
            mFramePool.clear();
            mFramePool.put(frame);
            frame = NULL;

            //20170328注释
            // clear_pool();
            //if (LIKELY(mFramePool.size() < FRAME_POOL_SZ)) {    //recycle_frame()
            //    mFramePool.put(frame);      //recycle_frame()
            //    frame = NULL;
            // } else {
            //    LOGW("test : mFramePool.size() > 2 FRAME_POOL_SZ");
            // }
        }
    }
    pthread_mutex_unlock(&pool_mutex);
    if (UNLIKELY(frame)) {
        //uvc_free_frame(frame);
        frame = NULL;
        LOGW("test :+++++++++++UNLIKELY(frame= null");
    }
}


void UVCPreview::init_pool(size_t data_bytes) {     //未被调用执行 ;
    ENTER();
    LOGW("test :++++++++++++++++++init_pool:  clear_pool");
    clear_pool();
    pthread_mutex_lock(&pool_mutex);
    {
        for (int i = 0; i < FRAME_POOL_SZ; i++) {
            mFramePool.put(uvc_allocate_frame(data_bytes));     //init_pool()
        }
    }
    pthread_mutex_unlock(&pool_mutex);
    EXIT();
}

void UVCPreview::clear_pool() {
    ENTER();

    pthread_mutex_lock(&pool_mutex);
    {
        const int n = mFramePool.size();    //clear_pool()
        for (int i = 0; i < n; i++) {
            uvc_free_frame(mFramePool[i]);  //clear_pool()
        }
        mFramePool.clear();     //clear_pool()
    }
    pthread_mutex_unlock(&pool_mutex);
    EXIT();
}

inline const bool UVCPreview::isRunning() const { return mIsRunning; }

int UVCPreview::setPreviewSize(int width, int height, int mode, float bandwidth) {
    ENTER();

    int result = 0;
    if ((requestWidth != width) || (requestHeight != height) || (requestMode != mode)) {
        requestWidth = width;
        requestHeight = height;
        requestMode = mode;
        requestBandwidth = bandwidth;

        uvc_stream_ctrl_t ctrl;
        result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, &ctrl,
                                                     !requestMode ? UVC_FRAME_FORMAT_YUYV
                                                                  : UVC_FRAME_FORMAT_MJPEG,
                                                     requestWidth, requestHeight, 1, requestFps);
    }

    RETURN(result, int);
}

int UVCPreview::setPreviewDisplay(ANativeWindow *preview_window) {
    ENTER();
    pthread_mutex_lock(&preview_mutex);
    {
        if (mPreviewWindow != preview_window) {
            if (mPreviewWindow)
                ANativeWindow_release(mPreviewWindow);
            mPreviewWindow = preview_window;
            if (LIKELY(mPreviewWindow)) {
                //2016.08.24 : 一本地窗口 设置缓冲区的几何结构 ??
                // 设置native window的buffer大小,可自动拉伸
                //4.ANativeWindow_setBuffersGeometry()：将格式应用到窗口
                ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                 frameWidth, frameHeight, previewFormat);
            }
        }
    }
    pthread_mutex_unlock(&preview_mutex);
    RETURN(0, int);
}

//2016.07.19 : IFrameCallback最终传递到这里 : setFrameCallback
int UVCPreview::setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format) {


    ENTER();
    pthread_mutex_lock(&capture_mutex);
    {
        if (isRunning() && isCapturing()) {
            mIsCapturing = false;
            if (mFrameCallbackObj) {
                pthread_cond_signal(&capture_sync);
                pthread_cond_wait(&capture_sync, &capture_mutex);    // wait finishing capturing
            }
        }
        if (!env->IsSameObject(mFrameCallbackObj, frame_callback_obj)) {
            iframecallback_fields.onFrame = NULL;
            if (mFrameCallbackObj) {
                env->DeleteGlobalRef(mFrameCallbackObj);
            }
            mFrameCallbackObj = frame_callback_obj;
            if (frame_callback_obj) {
                // get method IDs of Java object for callback       //IFrameCallback的onFrame()在这里被回调 :
                jclass clazz = env->GetObjectClass(frame_callback_obj);
                if (LIKELY(clazz)) {
                    iframecallback_fields.onFrame = env->GetMethodID(clazz,
                                                                     "onFrame",
                                                                     "(Ljava/nio/ByteBuffer;)V");
                } else {
                    LOGW("failed to get object class");
                }
                env->ExceptionClear();
                if (!iframecallback_fields.onFrame) {
                    LOGE("Can't find IFrameCallback#onFrame");
                    env->DeleteGlobalRef(frame_callback_obj);
                    mFrameCallbackObj = frame_callback_obj = NULL;
                }
            }
        }
        if (frame_callback_obj) {
            mPixelFormat = pixel_format;
            callbackPixelFormatChanged();
        }
    }
    pthread_mutex_unlock(&capture_mutex);
    RETURN(0, int);
}

void UVCPreview::callbackPixelFormatChanged() {
    mFrameCallbackFunc = NULL;
    const size_t sz = requestWidth * requestHeight;
    switch (mPixelFormat) {
        case PIXEL_FORMAT_RAW:
            LOGI("PIXEL_FORMAT_RAW:");
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_YUV:
            LOGI("PIXEL_FORMAT_YUV:");
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_RGB565:
            LOGI("PIXEL_FORMAT_RGB565:");
            mFrameCallbackFunc = uvc_any2rgb565;    //RGB565
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_RGBX:
            LOGI("PIXEL_FORMAT_RGBX:");
            mFrameCallbackFunc = uvc_any2rgbx;
            callbackPixelBytes = sz * 4;
            break;
        case PIXEL_FORMAT_YUV20SP:
            LOGI("PIXEL_FORMAT_YUV20SP:");
            mFrameCallbackFunc = uvc_yuyv2yuv420SP;     //COLOR_FormatYUV420SemiPlanar
            callbackPixelBytes = (sz * 3) / 2;
            break;
        case PIXEL_FORMAT_NV21:
            LOGI("PIXEL_FORMAT_NV21:");
            mFrameCallbackFunc = uvc_yuyv2iyuv420SP;    //COLOR_QCOM_FormatYUV420SemiPlanar
            callbackPixelBytes = (sz * 3) / 2;
            break;
    }
}

void UVCPreview::clearDisplay() {
    ENTER();

    ANativeWindow_Buffer buffer;
    pthread_mutex_lock(&capture_mutex);
    {
        if (LIKELY(mCaptureWindow)) {
            if (LIKELY(ANativeWindow_lock(mCaptureWindow, &buffer, NULL) == 0)) {
                uint8_t *dest = (uint8_t *) buffer.bits;
                const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
                const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
                for (int i = 0; i < buffer.height; i++) {
                    memset(dest, 0, bytes);
                    dest += stride;
                }
                ANativeWindow_unlockAndPost(mCaptureWindow);
            }
        }
    }
    pthread_mutex_unlock(&capture_mutex);
    pthread_mutex_lock(&preview_mutex);
    {
        if (LIKELY(mPreviewWindow)) {
            //通过ANativeWindow_lock获得buffer，再拿buffer的width和height :
            if (LIKELY(ANativeWindow_lock(mPreviewWindow, &buffer, NULL) == 0)) {
                uint8_t *dest = (uint8_t *) buffer.bits;
                const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
                const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
                for (int i = 0; i < buffer.height; i++) {
                    //2016.08.29 : 清空数据缓存 :
                    //memset(*s, ch, n) :  设置s中的所有字节为ch, s数组的大小由n给定
                    memset(dest, 0, bytes);
                    dest += stride;
                }
                ANativeWindow_unlockAndPost(mPreviewWindow);
            }
        }
    }
    pthread_mutex_unlock(&preview_mutex);

    EXIT();
}

//Java层中UVCCamera对象startPreview()调用的final底层 : 开始预览图像 :
int UVCPreview::startPreview() {
    ENTER();
    creatWideTab();
    uvc_init_gamma_tab();
    LOGW("test :----------------------------------startPreview:  clear_pool %d", "");
    int result = EXIT_FAILURE;
    if (!isRunning()) {
        mIsRunning = true;
        pthread_mutex_lock(&preview_mutex);
        {
            if (LIKELY(mPreviewWindow)) {
                result = pthread_create(&preview_thread, NULL, preview_thread_func,
                                        (void *) this);  //创建预览线程 ; 不断更新预览图像 ;

                //2016.09.13 测试log :
                __android_log_print(ANDROID_LOG_INFO, "UVCPre_test",
                                    "startPreview() --> preview_thread_func");

            }
        }
        pthread_mutex_unlock(&preview_mutex);
        if (UNLIKELY(result != EXIT_SUCCESS)) {
            LOGW("UVCCamera::window does not exist/already running/could not create thread etc.");
            mIsRunning = false;
            pthread_mutex_lock(&preview_mutex);
            {
                pthread_cond_signal(&preview_sync);
            }
            pthread_mutex_unlock(&preview_mutex);
        }
    }
    RETURN(result, int);
}

//Java层中UVCCamera对象stopPreview()调用的final底层 : 停止预览图像 :
int UVCPreview::stopPreview() {
    ENTER();
    bool b = isRunning();
    if (LIKELY(b)) {
        mIsRunning = false;
        pthread_cond_signal(&preview_sync);
        pthread_cond_signal(&capture_sync);
        if (pthread_join(capture_thread, NULL) != EXIT_SUCCESS) {
            LOGW("UVCPreview::terminate capture thread: pthread_join failed");
        }
        if (pthread_join(preview_thread, NULL) != EXIT_SUCCESS) {
            LOGW("UVCPreview::terminate preview thread: pthread_join failed");
        }
        clearDisplay();     //清空显示 ;
    }
    LOGW("test :++++++++++++++++++stopPreview:  clearPreviewFrame");
    LOGW("test :++++++++++++++++++stopPreview:  clearCaptureFrame");
    clearPreviewFrame();
    clearCaptureFrame();
    pthread_mutex_lock(&preview_mutex);
    if (mPreviewWindow) {
        ANativeWindow_release(mPreviewWindow);
        mPreviewWindow = NULL;
    }
    pthread_mutex_unlock(&preview_mutex);
    pthread_mutex_lock(&capture_mutex);
    if (mCaptureWindow) {
        ANativeWindow_release(mCaptureWindow);
        mCaptureWindow = NULL;
    }
    pthread_mutex_unlock(&capture_mutex);
    RETURN(0, int);
}

//**********************************************************************
//
//**********************************************************************
//uvc_preview_frame_callback相机的预览数据帧回调 :    该回调函数最终被传递到stream.c中被回调
void UVCPreview::uvc_preview_frame_callback(uvc_frame_t *frame, void *vptr_args) {
    UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
    if
        UNLIKELY(!preview->isRunning() || !frame || !frame->frame_format || !frame->data ||
                 !frame->data_bytes)
    return;
    if (UNLIKELY(
            ((frame->frame_format != UVC_FRAME_FORMAT_MJPEG) &&
             (frame->actual_bytes < preview->frameBytes))
            || (frame->width != preview->frameWidth) || (frame->height != preview->frameHeight))) {

#if LOCAL_DEBUG
        LOGD("broken frame!:format=%d,actual_bytes=%d/%d(%d,%d/%d,%d)",
            frame->frame_format, frame->actual_bytes, preview->frameBytes,
            frame->width, frame->height, preview->frameWidth, preview->frameHeight);
#endif
        return;
    }
    if (LIKELY(preview->isRunning())) {
        uvc_frame_t *copy = preview->get_frame(frame->data_bytes);
        if (UNLIKELY(!copy)) {
#if LOCAL_DEBUG
            LOGE("uvc_callback:unable to allocate duplicate frame!");
#endif
            return;
        }
        uvc_error_t ret = uvc_duplicate_frame(frame, copy);     //UVC复制帧
        if (UNLIKELY(ret)) {
            preview->recycle_frame(copy);
            return;
        }
        //2016.08.24 : addPreviewFrame()向泛型数组previewFrames添加预览帧数据成员;
        preview->addPreviewFrame(copy);
    }
}

//2016.08.24 : 向泛型数组previewFrames添加预览帧数据成员;
void UVCPreview::addPreviewFrame(uvc_frame_t *frame) {

    pthread_mutex_lock(&preview_mutex);
    {
        if (isRunning() && (previewFrames.size() < MAX_FRAME)) {
            previewFrames.put(frame);
            frame = NULL;
            pthread_cond_signal(&preview_sync);
         } else{//20180413 添加
            previewFrames.clear();
            previewFrames.put(frame);
        }
        if (frame) {
            recycle_frame(frame);
         }
    }
    pthread_mutex_unlock(&preview_mutex);
    //20180413 注释 添加到同步锁中
    /*if (frame) {
        recycle_frame(frame);
    }*/
}

//2016.08.24 : 等待预览的图像帧,所以的图像数据都要经过这里等待获取;
uvc_frame_t *UVCPreview::waitPreviewFrame() {
    uvc_frame_t *frame = NULL;

    pthread_mutex_lock(&preview_mutex);
    {
        if (!previewFrames.size()) {
            //20180413 线程休眠2毫秒
            usleep(2000);
            pthread_cond_wait(&preview_sync, &preview_mutex);

        }
        if (LIKELY(isRunning() && previewFrames.size() > 0)) {
            frame = previewFrames.remove(0);
        }
    }

    pthread_mutex_unlock(&preview_mutex);   //线程互斥锁

    return frame;
}

//2016.08.24 : 清空泛型数组previewFrames里的预览数据帧成员 ;
void UVCPreview::clearPreviewFrame() {
    pthread_mutex_lock(&preview_mutex);
    {
        for (int i = 0; i < previewFrames.size(); i++) {


            recycle_frame(previewFrames[i]);
        }
        previewFrames.clear();
    }
    pthread_mutex_unlock(&preview_mutex);
}

void *UVCPreview::preview_thread_func(void *vptr_args) {
    int result;

    ENTER();
    UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
    if (LIKELY(preview)) {
        uvc_stream_ctrl_t ctrl;
        result = preview->prepare_preview(&ctrl);
        if (LIKELY(!result)) {
            preview->do_preview(&ctrl);

            //2016.09.13 测试log :
            __android_log_print(ANDROID_LOG_INFO, "UVCPre_test",
                                "preview_thread_func() --> do_preview()");

        }
    }
    PRE_EXIT();
    pthread_exit(NULL);
}

int UVCPreview::prepare_preview(uvc_stream_ctrl_t *ctrl) {
    uvc_error_t result;

    ENTER();
    result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, ctrl,
                                                 !requestMode ? UVC_FRAME_FORMAT_YUYV
                                                              : UVC_FRAME_FORMAT_MJPEG,
                                                 requestWidth, requestHeight, 1, requestFps
    );
    if (LIKELY(!result)) {
#if LOCAL_DEBUG
        uvc_print_stream_ctrl(ctrl, stderr);
#endif
        uvc_frame_desc_t *frame_desc;
        result = uvc_get_frame_desc(mDeviceHandle, ctrl, &frame_desc);
        if (LIKELY(!result)) {
            frameWidth = frame_desc->wWidth;
            frameHeight = frame_desc->wHeight;
            LOGI("frameSize=(%d,%d)@%s", frameWidth, frameHeight,
                 (!requestMode ? "YUYV" : "MJPEG"));

            pthread_mutex_lock(&preview_mutex);
            if (LIKELY(mPreviewWindow)) {
                ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                 frameWidth, frameHeight, previewFormat);
            }
            pthread_mutex_unlock(&preview_mutex);
        } else {
            frameWidth = requestWidth;
            frameHeight = requestHeight;
        }
        frameMode = requestMode;
        frameBytes = frameWidth * frameHeight * (!requestMode ? 2 : 4);
        previewBytes = frameWidth * frameHeight * PREVIEW_PIXEL_BYTES;
    } else {
        LOGE("could not negotiate with camera:err=%d", result);
    }
    RETURN(result, int);
}

void UVCPreview::do_preview(uvc_stream_ctrl_t *ctrl) {
    ENTER();

    uvc_frame_t *frame = NULL;
    uvc_frame_t *frame_mjpeg = NULL;
    //uvc_preview_frame_callback相机的预览数据帧回调 :
    uvc_error_t result = uvc_start_streaming_bandwidth(     //UVC开始流带宽
            mDeviceHandle, ctrl, uvc_preview_frame_callback, (void *) this, requestBandwidth, 0);

    if (LIKELY(!result)) {
        LOGW("test :++++++++++++++++++do_preview:  clearPreviewFrame");
        clearPreviewFrame();
        pthread_create(&capture_thread, NULL, capture_thread_func,
                       (void *) this);   //创建不断更新拍照图像的线程 ;

        //2016.09.13 测试log :
        __android_log_print(ANDROID_LOG_INFO, "UVCPre_test",
                            "do_preview() --> capture_thread_func");


#if LOCAL_DEBUG
        LOGI("Streaming...");
#endif
        if (frameMode) {
            // MJPEG mode
            for (; LIKELY(isRunning());) {
                frame_mjpeg = waitPreviewFrame();
                if (LIKELY(frame_mjpeg)) {
                    frame = get_frame(
                            frame_mjpeg->width * frame_mjpeg->height * 2);    //乘2 ,每像素2字节 ?
                    //图像格式转换 :
                    result = uvc_mjpeg2yuyv(frame_mjpeg, frame);   // MJPEG => yuyv
                    recycle_frame(frame_mjpeg);
                    if (LIKELY(!result)) {

                        //改回原来的框架而不是返回转换框架即使convert_func不空。
                        frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx,
                                                 4);  //乘4 ,每像素4字节 ?
                        addCaptureFrame(frame);

                        //2016.09.13 测试log : 日志显示 , 程序不进入这里执行 ;
//                       __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "do_preview() --> 1 draw_preview_one()" );

                    } else {
                        recycle_frame(frame);
                    }
                }
            }
        } else {
            // yuvyv mode
            for (; LIKELY(isRunning());) {
                frame = waitPreviewFrame();
                if (LIKELY(frame)) {
                    //改回原来的框架而不是返回转换框架即使convert_func不空。
                    frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx, 4);



                    addCaptureFrame(frame);

                    //2016.09.13 测试log : 日志显示 , 程序在这里循环了 ;
                     // __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "do_preview() -->------------- 2 draw_preview_one()" );

                }
            }
        }
        pthread_cond_signal(&capture_sync);
#if LOCAL_DEBUG
        LOGI("preview_thread_func:wait for all callbacks complete");
#endif
        uvc_stop_streaming(mDeviceHandle);
#if LOCAL_DEBUG
        LOGI("Streaming finished");
#endif
    } else {
        uvc_perror(result, "failed start_streaming");
    }

    EXIT();
}

//img_resample(src, w, h, 32, dest, 1280, 800, 32, 1);
//in_width=5440(1360*4)   in_height=1040
//(src, w, h, 24, dest, 800, 1280, 24, 1)
static void img_resample(const uint8_t *src, const int in_width, int in_height,
                         int bitdepth,    //IMG重采样
                         uint8_t *dest, const int out_width, int out_height, int out_bitdepth,
                         int count) {
    const int pixelcount = bitdepth % 8;   //取余=0

    for (int i = (out_height - in_height) / 2; i < (out_height - in_height) / 2 + out_height; i++) {
        memcpy(dest, src + (in_width - out_width) / 2 * pixelcount, out_width * pixelcount);
        dest += out_width * pixelcount;
        src += i * in_width * pixelcount;
    }
//    __android_log_print(ANDROID_LOG_INFO, "UVCPre_test_img", "%d " ,pixelcount);


}


//2016.058.24 : memcpy(a,b,c) :从内存中复制数据,将b中的数据复制到a中,数据长度为c
//stride_src : 步幅SRC ,步长    (width=1360*4,height=1024,stride_src=1360*4,stride_dest=1360*4(5440))
static void copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height,
                      const int stride_src, const int stride_dest) {
    //unsigned  char *test = src;

    const int h8 = height % 8;  //1024%8取余=0
    for (int i = 0; i < h8; i++) {
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
    }
    for (int i = 0; i < height; i += 8) {
        for (int j = 0; j <= 7; j++) {
            memcpy(dest, src, width); //1
            dest += stride_dest;
            src += stride_src;
        }
        /*
        memcpy(dest, src, width); //1
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width); //2
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width); //3
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width); //4
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width); //5
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width); //6
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width); //7
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width); //8
        dest += stride_dest; src += stride_src;
        */
    }
}

//2016.08.24 : 传递特定的帧数据到surface表面（anativewindow）
// transfer specific frame data to the Surface(ANativeWindow)
int copyToSurface(uvc_frame_t *frame, ANativeWindow **window) {
     ENTER();
    int result = 0;
    if (LIKELY(*window)) {
        ANativeWindow_Buffer buffer;
        //ANativeWindow_lock() : 通过ANativeWindow_lock获得buffer，再拿buffer的width和height ;
        if (LIKELY(ANativeWindow_lock(*window, &buffer, NULL) == 0)) {

            // source = frame data  : 图像数据来源 :
            uint8_t *src = (uint8_t *) frame->data;      //获取复制到surface的帧数据data;

            const int src_w = frame->width * PREVIEW_PIXEL_BYTES;
            const int src_step = frame->width * PREVIEW_PIXEL_BYTES;    //1360*4

            // destination = Surface(ANativeWindow)  : 图像数据要复制到的目标 :
            uint8_t *dest = (uint8_t *) buffer.bits;
            const int dest_w =
                    buffer.width * PREVIEW_PIXEL_BYTES;      //PREVIEW_PIXEL_BYTES = 4	// RGBA/RGBX
            const int dest_step = buffer.stride * PREVIEW_PIXEL_BYTES;  //1360*4

//            __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", " frame.width=%d frame.height=%d buffer.width=%d buffer.height=%d " ,	\
                                          frame->width, frame->height, buffer.width, buffer.height);

            //2016.09.13 测试log :
//           __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "copyToSurface() " );

            // use lower transfer bytes     //使用较低的传输字节
            const int w = src_w < dest_w ? src_w : dest_w;
            // use lower height     //使用较低的高度
            const int h = frame->height < buffer.height ? frame->height : buffer.height;
            // transfer from frame data to the Surface  //从帧数据传输到surface表面



           copyFrame(src, dest, w, h, src_step, dest_step);

            LOGD("w=%d h=%d src_step=%d dest_step=%d", w, h, src_step, dest_step);

            //直接用__android_log_print() ;
//			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "[%d*%s:%d:%s]:" FMT,	\
            							gettid(), basename(__FILE__), __LINE__, __FUNCTION__, ## __VA_ARGS__)
//			__android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "[%d*%s:%d:%s]: \n w=%d h=%d src_step=%d dest_step=%d " ,	\
            							gettid(), basename(__FILE__), __LINE__, __FUNCTION__, w, h, src_step, dest_step);

            img_resample(src, w, h, 24, dest, 800, 1280, 24, 1);    //IMG重采样

            ANativeWindow_unlockAndPost(*window);
        } else {
            result = -1;
        }
    } else {
        result = -1;
    }
    return result; //RETURN(result, int);
}
/*void m_draw_preview_one(uvc_frame_t *frame, ANativeWindow **window,int pixcelBytes){

    pthread_mutex_lock(&preview_mutex);
    {
        converted = get_frame(frame->width * frame->height * pixcelBytes);
        //20170717增加中括号
        recolor((uint8_t *) converted->data, frame->width, frame->height,
                pixcelBytes);   //底层图像着色函数 :
        copyToSurface(converted, window);

    }
    pthread_mutex_unlock(&preview_mutex);
}*/
//改回原来的框架而不是返回转换框架即使convert_func不空。
// changed to return original frame instead of returning converted frame even if convert_func is not null.
uvc_frame_t *UVCPreview::draw_preview_one(uvc_frame_t *frame, ANativeWindow **window,
                                          convFunc_t convert_func, int pixcelBytes) {
    // ENTER();

    int b = 0;
    pthread_mutex_lock(&preview_mutex);
    {
        b = *window != NULL;
    }
    pthread_mutex_unlock(&preview_mutex);
    if (LIKELY(b)) {
        uvc_frame_t *converted;

        if (convert_func) {
            //20180428 增加格式转换与着色同步
            pthread_mutex_lock(&preview_mutex);
            {
                converted = get_frame(frame->width * frame->height * pixcelBytes);


                if (LIKELY(converted)) {
                    //转换视频数据格式 :
                    b = convert_func(frame, converted);     //转换图像格式 ;

                    // LOGW("test :++++++++++++++++++b:  b %d", b);
                    LOGD("width=%d height=%d b=%d pixcelBytes=%d", width, height, b, pixcelBytes);
                    if (!b) {
                        if(mGray!=100)
                            Add_edge((uint8_t *) converted->data, frame->width, frame->height,
                                     mGray);
                        //20180428 annotation
                        // pthread_mutex_lock(&preview_mutex);
                        //  {
                        //  LOGW("test :++++++++++++++++++b0000:  b %d", b);
                        //20170717增加中括号
                        if(setcolor!=-1)
                        recolor((uint8_t *) converted->data, frame->width, frame->height,
                                pixcelBytes);   //底层图像着色函数 :
                        //  LOGW("test :++++++++++++++++++b1111:  b %d", b);
                        // LOGD("width=%d height=%d pixcelBytes=%d", width, height, pixcelBytes);
                        //2016.08.24 : 传递特定的帧数据到surface纹理（anativewindow）
                        //20170908
                        // widePro((uint8_t *) converted->data, mGray, 1360 * 1387 * 3);
                        copyToSurface(converted, window);
                        // LOGW("test :++++++++++++++++++b222:  b %d", b);
                        //2016.09.13 测试log : 日志显示 , copyToSurface()在这里调用
//
                        //}
                        //pthread_mutex_unlock(&preview_mutex);
                    } else {
                        LOGE("failed converting");  //失败的转换
                    }

                    recycle_frame(converted);
                }

            }
            pthread_mutex_unlock(&preview_mutex);
        }  else {
            pthread_mutex_lock(&preview_mutex);
            //2016.08.24 : 传递特定的帧数据到surface表面（anativewindow）
            copyToSurface(frame, window);

            //2016.09.13 测试log :
//           __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "draw_preview_one() --> 2 copyToSurface()" );

            pthread_mutex_unlock(&preview_mutex);
        }
    }
    return frame; //RETURN(frame, uvc_frame_t *);
}

//======================================================================
//
//======================================================================
inline const bool UVCPreview::isCapturing() const { return mIsCapturing; }

int UVCPreview::setCaptureDisplay(ANativeWindow *capture_window) {
    ENTER();
    pthread_mutex_lock(&capture_mutex);
    {
        if (isRunning() && isCapturing()) {
            mIsCapturing = false;
            if (mCaptureWindow) {
                pthread_cond_signal(&capture_sync);
                pthread_cond_wait(&capture_sync, &capture_mutex);    // wait finishing capturing
            }
        }
        if (mCaptureWindow != capture_window) {
            // release current Surface if already assigned.
            if (UNLIKELY(mCaptureWindow))
                ANativeWindow_release(mCaptureWindow);
            mCaptureWindow = capture_window;
            // if you use Surface came from MediaCodec#createInputSurface
            // you could not change window format at least when you use
            // ANativeWindow_lock / ANativeWindow_unlockAndPost
            // to write frame data to the Surface...
            // So we need check here.
            /**
            如果你使用表面来自mediacodec # createinputsurface
            你不能改变窗口格式，至少当你使用
            anativewindow_lock / anativewindow_unlockandpost
            将帧数据写入到表面…
            所以我们需要在这里检查。
            */
            if (mCaptureWindow) {
                int32_t window_format = ANativeWindow_getFormat(mCaptureWindow);
                if ((window_format != WINDOW_FORMAT_RGB_565)
                    && (previewFormat == WINDOW_FORMAT_RGB_565)) {
                    LOGE("window format mismatch, cancelled movie capturing.");
                    ANativeWindow_release(mCaptureWindow);
                    mCaptureWindow = NULL;
                }
            }
        }
    }
    pthread_mutex_unlock(&capture_mutex);
    RETURN(0, int);
}

void UVCPreview::addCaptureFrame(uvc_frame_t *frame) {
    pthread_mutex_lock(&capture_mutex);
    {//-

        //20170717增加中括号
        if (LIKELY(isRunning())) {
            // keep only latest one
            //只保留最新的一个
            if (captureQueu) {
                /* LOGW("test : mFramePool.size() > 1 FRAME_POOL_SZ>>6");
                 int count =mFramePool.size();
                 switch (count){
                     case 0:
                         LOGW("test : mFramePool.size() =0");
                         break;
                     case 1:
                         LOGW("test : mFramePool.size() =1");
                         break;
                     case 2:
                         LOGW("test : mFramePool.size() =2");
                         break;
                     case 3:
                         LOGW("test : mFramePool.size() =3");
                         break;
                     case 4:
                         LOGW("test : mFramePool.size() =4");
                         break;
                     case 5:
                         LOGW("test : mFramePool.size() =5");
                         break;
                 }*/
                recycle_frame(captureQueu);
            }
            captureQueu = frame;
            pthread_cond_broadcast(&capture_sync);
        }
    }
    pthread_mutex_unlock(&capture_mutex);
}

/**
 * get frame data for capturing, if not exist, block and wait
 *获取捕获的帧数据，如果不存在的话，块和等待
 */
uvc_frame_t *UVCPreview::waitCaptureFrame() {
    uvc_frame_t *frame = NULL;
    pthread_mutex_lock(&capture_mutex);
    {
        if (!captureQueu) {
            pthread_cond_wait(&capture_sync, &capture_mutex);
        }
        if (LIKELY(isRunning() && captureQueu)) {
            frame = captureQueu;
            captureQueu = NULL;
        }
    }
    pthread_mutex_unlock(&capture_mutex);
    return frame;
}

/**
 * clear frame data for capturing
 *捕获的清除帧数据
 */
void UVCPreview::clearCaptureFrame() {
    pthread_mutex_lock(&capture_mutex);
    {
        if (captureQueu) {
            recycle_frame(captureQueu);
        }
        captureQueu = NULL;
    }
    pthread_mutex_unlock(&capture_mutex);
}

//======================================================================
/*
 * thread function
 * @param vptr_args pointer to UVCPreview instance
 */
// static
void *UVCPreview::capture_thread_func(void *vptr_args) {
    int result;

    ENTER();

    LOGW("capture_thread_func-");
    UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
    if (LIKELY(preview)) {
        JavaVM *vm = getVM();
        JNIEnv *env;
        // attach to JavaVM
        vm->AttachCurrentThread(&env, NULL);
        //捕获的实际函数
        preview->do_capture(env);    // never return until finish previewing

        //2016.09.13 测试log :
        __android_log_print(ANDROID_LOG_INFO, "UVCPre_test",
                            "capture_thread_func() --> do_capture()");

        // detach from JavaVM
        vm->DetachCurrentThread();
        MARK("DetachCurrentThread");
    }
    PRE_EXIT();
    pthread_exit(NULL);
}

/**
 * the actual function for capturing
 *捕获的实际函数
 */
void UVCPreview::do_capture(JNIEnv *env) {

    ENTER();
    LOGW("test :++++++++++++++++++do_capture:  clearCaptureFrame");
    clearCaptureFrame();
    callbackPixelFormatChanged();
    for (; isRunning();) {
        mIsCapturing = true;
        if (mCaptureWindow) {
            //将帧数据写入捕获的表面 Surface :
            do_capture_surface(env);     //从测试日志显示 , 程序未进入这个函数 ;
        } else {
            do_capture_idle_loop(env);  //回调从这里执行 ;

            //2016.09.13 测试log :
            __android_log_print(ANDROID_LOG_INFO, "UVCPre_test",
                                "do_capture() --> do_capture_idle_loop()");

        }
        pthread_cond_broadcast(&capture_sync);
    }    // end of for (; isRunning() ;)
    EXIT();
}

void UVCPreview::do_capture_idle_loop(JNIEnv *env) {

    ENTER();

    //2016.09.13 测试log :
//   __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "do_capture_idle_loop() " );

    for (; isRunning() && isCapturing();) {
        //获取捕获的帧数据，如果不存在的话，块和等待
      //  LOGW("test : do_capture_idle_loop");
        do_capture_callback(env, waitCaptureFrame());

        //2016.09.13 测试log :       //日志显示 , do_capture_callback()正在执行在这里 ;
        __android_log_print(ANDROID_LOG_INFO, "UVCPre_test",
                            "do_capture_idle_loop() --> do_capture_callback()");

    }

    EXIT();
}

/**
 * write frame data to Surface for capturing
 * 将帧数据写入捕获的表面 Surface :
 */
void UVCPreview::do_capture_surface(JNIEnv *env) {      //从测试日志显示 , 程序未进入这个函数 ;
    ENTER();

    //2016.09.13 测试log :
//       __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "do_capture_surface() " );

    uvc_frame_t *frame = NULL;
    uvc_frame_t *converted = NULL;
    char *local_picture_path;

    for (; isRunning() && isCapturing();) {
        //获取捕获的帧数据，如果不存在的话，块和等待
        frame = waitCaptureFrame();
        if (LIKELY(frame)) {
            // frame data is always YUYV format.
            if (LIKELY(isCapturing())) {
                if (UNLIKELY(!converted)) {
                    converted = get_frame(previewBytes);
                }
                if (LIKELY(converted)) {
                    //转换视频数据格式 :
                    int b = uvc_any2rgbx(frame, converted);
                    //2016.06.06 修改 :修改捕获的图像不进过编码　：
                    //				int b  = 0;
                    if (!b) {
                        if (LIKELY(mCaptureWindow)) {
                            //2016.08.24 : 传递特定的帧数据到surface表面（anativewindow）
                            copyToSurface(converted, &mCaptureWindow);

                            //2016.09.13 测试log :     //从测试日志显示 , 程序未进入这个函数 ;
//                           __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "do_capture_surface() --> copyToSurface()" );
                        }
                    }
                }
            }
            LOGW("test : do_capture_callback");
            do_capture_callback(env, frame);
            //2016.09.13 测试log :    //从测试日志显示 , 程序未进入这个函数 ;
//           __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "do_capture_surface() --> do_capture_callback()" );
        }
    }
    if (converted) {
        recycle_frame(converted);
    }
    if (mCaptureWindow) {
        ANativeWindow_release(mCaptureWindow);
        mCaptureWindow = NULL;
    }

    EXIT();
}

/**
* call IFrameCallback#onFrame if needs      //2016.07.17 : IFrameCallback的onFrame()方法的最终回调 :
 */
void UVCPreview::do_capture_callback(JNIEnv *env, uvc_frame_t *frame) {
    ENTER();

    //2016.09.13 测试log :
//   __android_log_print(ANDROID_LOG_INFO, "UVCPre_test", "do_capture_callback() " );

    if (LIKELY(frame)) {
        uvc_frame_t *callback_frame = frame;        //uvc_frame_t : uvcCamera的帧数据 :

        if (mFrameCallbackObj) {
            if (mFrameCallbackFunc) {
                callback_frame = get_frame(callbackPixelBytes);
                if (LIKELY(callback_frame)) {
                    int b = mFrameCallbackFunc(frame, callback_frame);  //转换视频数据格式 ?
                    //着色 : 对获取到的视频图像帧数据着色 :
                    if(mGray!=100)
                    {
                        Add_edge ((uint8_t *) callback_frame->data, callback_frame->width,
                                  callback_frame->height, mGray);
                    }
                    if(setcolor!=-1)
                    recolor((uint8_t *) callback_frame->data, callback_frame->width,
                            callback_frame->height, 4);      //底层图像着色函数 :
                    //20170908
                   // widePro((uint8_t *) callback_frame->data, mGray, 1360 * 1387 * 3);
                    recycle_frame(frame);
                    if (UNLIKELY(b)) {
                        LOGW("failed to convert for callback frame");
                        goto SKIP;
                    }
                } else {
                    LOGW("failed to allocate for callback frame");
                    callback_frame = frame;
                    goto SKIP;
                }
            }
            //直接创建字节缓冲 NewDirectByteBuffer
            //NewDirectByteBuffer()第一个参数为void* address 即数据存储的地址指针 ,第二个参数为jlong capacity即数据的长度容量 ;
            jobject buf = env->NewDirectByteBuffer(callback_frame->data, callbackPixelBytes);
            //C++调用Java的方法 : CallVoidMethod(调用返回void的Java方法) , 此处IFrameCallback的onFrame()方法的最终回调 :
            //传入的参数buf是ByteBuffer类型的数据 , 最终的图像数据为传入的参数buf ;
            env->CallVoidMethod(mFrameCallbackObj, iframecallback_fields.onFrame, buf);
            env->ExceptionClear();
            env->DeleteLocalRef(buf);
        }
        SKIP:
        recycle_frame(callback_frame);
    }
    EXIT();
}

void UVCPreview::setColor(uint32_t color) {
    uint8_t r, g, b;
    setcolor = color;
    r = (color & 0xff0000) >> 16;
    g = (color & 0xff00) >> 8;
    b = color & 0xff;

    changeColor(r, g, b);
}

void UVCPreview::setGray(int gray) {
    mGray = gray;
    // LOGW("test :++++++++++++++++++do_preview:  setGray %d",mGray);
}

//改变r_map[256] ,g_map[256] ,b_map[256]三个像素分量的着色数据表 ; 供给recolor()着色函数查询和赋值计算;
void UVCPreview::changeColor(uint8_t REF_RED, uint8_t REF_GREEN, uint8_t REF_BlUE) {

    float rFactor = REF_RED / 255.0;
    float gFactor = REF_GREEN / 255.0;
    float bFactor = REF_BlUE / 255.0;

    for (int i = 0; i < 256; i++) {
        r_map[i] = i * rFactor;
        g_map[i] = i * gFactor;
        b_map[i] = i * bFactor;
    }
}

//底层图像着色函数 :
void UVCPreview::recolor(uint8_t *buf, int width, int height, int bpp)   //底层图像着色函数 :
{
    int gray = 0;
    int offset = 0;
    //bpp == 4,. RGBX8888
    if (bpp == 4) {     //判断bpp==4 , 即每个像素位为4个字节 ;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                offset = i * width * 4 + j * 4;
                gray = buf[offset];     //根据实际图像的宽高设置像素的偏移值 ;取得RGB三色分量 ;

                buf[offset + 0] = r_map[gray];


                buf[offset + 1] = g_map[gray];


                buf[offset + 2] = b_map[gray];
                if(mGray!=100) {
                    buf[offset + 0] = wideTab[mGray][(buf[offset + 0])];
                    buf[offset + 1] = wideTab[mGray][(buf[offset + 1])];
                    buf[offset + 2] = wideTab[mGray][(buf[offset + 2])];
                }

            }
        }
        
       /* long len = width*height*4;
        for(long k =0;k<len;k++){
            buf[k] = wideTab[mGray][(buf[k])];
        }*/
       
    }
}
void UVCPreview::Add_edge(uint8_t *buf, int width, int height, int bpp){   //底层图像着色函数 :
    int gray = 0;
    int threadhold =bpp;//设置阀值
    int edgethreadhold=3;//设置边阈值界
    int offset = 0;
    int pixeledge=0;
    // int temp[width*2];

    int  last_edge_pos=-1;
    int count_edge_index=0;
    //  memset(temp,0,width*2);
    // memset(temp_edge,0,width*height*sizeof(int));
    for (int i = 1; i < height-1; i++) {
        //   memset(temp,0,width*2);
        //    last_edge_pos=-1;
        for (int j = 1; j < width-1; j++) { // 计算i行的梯度，保存到temp中，每两行计算一次，不使用相对梯度，而使用绝对梯度
            offset = i * width*4  + j*4;
            //   count_edge_index=0;
            gray = buf[offset];  //根据实际图像的宽高设置像素的偏移值 ;取得RGB三色分量 ;
            //  if (1) {
            int offset1 = (i-1) * width *4 + (j - 1)*4;
            int offset2 = (i-1) * width *4 + j*4;
            int offset3 = (i-1) * width *4+ (j + 1)*4;
            int offset4 = i * width*4  + (j - 1)*4 ;
            int offset5 = i * width*4 + (j + 1)*4;
            int offset6 = (i+1) * width*4 + (j - 1)*4;
            int offset7 = (i+1) * width*4 + j*4;
            int offset8 = (i+1)* width*4 + (j + 1)*4;

            // 获取prewitt 算子梯度
            pixeledge=abs(buf[offset1]+buf[offset2]+buf[offset3]-buf[offset6]-buf[offset7]-buf[offset8])+abs(buf[offset1]+buf[offset4]+buf[offset6]-buf[offset8]-buf[offset3]-buf[offset5]);
            // 设置算子梯度阈值
            if (pixeledge>threadhold)
                buf[offset+2]=1;
            else
                buf[offset+2]=0;
            //  }
        }

    }
    for (int i = 1; i < height-1; i++) {
        for (int j = 0; j < width - 1 && i > 2; j++) {
            offset=i*width*4+j*4;
            if (buf[offset + 2] !=1||buf[offset + 2+4] != 1||buf[offset + 2+8] != 1) {

                buf[offset + 2]=0;
            }
            if (buf[offset + 2] != 1||buf[offset + 2+width*4] != 1||buf[offset + 2+width*8] != 1) {

                buf[offset + 2]=0;
            }

        }
    }
    for (int i = 1; i < height-1; i++) {
        for (int j = 0; j < width - 1 && i > 2; j++) {
            offset = i * width * 4 + j * 4;
            if (buf[offset + 2] == 1) {
                if(buf[offset ] + 15<255)
                    buf[offset ] = buf[offset ] + 15;
                else
                {
                    buf[offset ] =255;
                }
                buf[offset + 1] =buf[offset ];
                buf[offset + 2] = buf[offset ];

            }
            else
            {
                buf[offset + 2] = buf[offset ];
            }
        }
    }

}

