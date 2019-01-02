package com.yeespec.libuvccamera.uvccamera.encoder;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: MediaEncoderRunnable.java
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

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

public abstract class MediaEncoderRunnable implements Runnable {
    private static final boolean DEBUG = false;    // TODO set false on release
    //    private static final String TAG = "MediaEncoderRunnable";
    private static final String TAG = "test_MediaEncoder";

    protected static final int TIMEOUT_USEC = 10000;    // 10[msec]
    protected static final int MSG_FRAME_AVAILABLE = 1;
    protected static final int MSG_STOP_RECORDING = 9;

    public interface MediaEncoderListener {
        public void onPrepared(MediaEncoderRunnable encoder);

        public void onStopped(MediaEncoderRunnable encoder);
    }

    protected final Object mSync = new Object();
    /**
     * Flag that indicate this encoder is capturing now.
     * 标志，该标志表示该编码器正在捕获。
     */
    protected volatile boolean mIsCapturing;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     * 标志，表明编码器接收EOS（结束流）
     */
    protected boolean mIsEOS;
    /**
     * Flag the indicate the muxer is running       //标志的指示器运行
     */
    protected boolean mMuxerStarted;
    /**
     * Track Number      //通道数 ;
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     * 一般用MediaCodec创建编解码器时，使用的都是createDecoderByType方法，
     * 但在用三星PAD（API LEVEL 18， Android version 4.3）调试时发现，调用该方法创建音频编码器时会出错。
     * 故改为使用createByCodecName("OMX.SEC.aac.enc")创建音频编码器。估计这是一个API bug。
     */
    protected MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance    //MediaMuxerWarapper的弱引用的实例 , 录制视频时 , 经常出现引用为空的情况 , 导致录制的视频缺失数据 ;
     */
    protected final SoftReference<MediaMuxerWrapper> mWeakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    /**
     * Handler of encoding thread
     */
    private EncoderHandler mHandler;
    protected final MediaEncoderListener mListener;

    //2016.07.07 新增 :用于引用传递进来的MediaMuxerWrapper , 防止WeakReference弱引用被回收 ;
    private MediaMuxerWrapper mWrapper;

    public MediaEncoderRunnable(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        if (listener == null)
            throw new NullPointerException("MediaEncoderListener is null");
        if (muxer == null)
            throw new NullPointerException("MediaMuxerWrapper is null");
        mWeakMuxer = new SoftReference<MediaMuxerWrapper>(muxer);

        mWrapper = muxer;      //添加强引用 ; 防止WeakReference弱引用被提前回收 ;

        muxer.addEncoder(this);
        mListener = listener;
        synchronized (mSync) {
            // create BufferInfo here for effectiveness(to reduce GC)
            mBufferInfo = new MediaCodec.BufferInfo();
            // wait for Handler is ready
            new Thread(this, getClass().getSimpleName()).start();   //启动MediaEncoderRunnable线程本身 ;
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getOutputPath() {
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        return muxer != null ? muxer.getOutputPath() : null;
    }

    /**
     * the method to indicate frame data is soon available or already available
     * 表示帧数据的方法很快可用或已经可用 ; 通知编码器调用encoder.drain()对数据进行H.246编码 ;
     *
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
        //    	if (DEBUG) Log.v(TAG, "frameAvailableSoon");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return false;
            }
            mHandler.sendEmptyMessage(MSG_FRAME_AVAILABLE);
        }
        return true;
    }

    /**
     * Message loop for encoding thread
     * Prepare Looper/Handler and execute message loop and wait terminating.
     */
    @Override
    public void run() {
        // create Looper and Handler to access to this thread
        Looper.prepare();
        synchronized (mSync) {
            mHandler = new EncoderHandler(this);
            mRequestStop = false;
            mSync.notify();
        }
        Looper.loop();

        if (DEBUG)
            Log.d(TAG, "Encoder thread exiting");
        synchronized (mSync) {
            mIsCapturing = false;
            mRequestStop = true;
            mHandler = null;
        }
    }

    /*
    * prepareing method for each sub class
    * this method should be implemented in sub class, so set this as abstract method
    * @throws IOException
    *
    * 每个子类的制备方法
    *    这种方法应该在子类中实现，所以将此方法设置为抽象方法
    */
   /*package*/
    abstract void prepare() throws IOException;

    /*package*/ void startRecording() {
        if (DEBUG)
            Log.v(TAG, "startRecording");
        synchronized (mSync) {
            mIsCapturing = true;
            mRequestStop = false;
            mSync.notifyAll();
        }
    }

    /**
     * the method to request stop encoding      //请求停止编码的方法
     */
    /*package*/ void stopRecording() {
        if (DEBUG)
            Log.v(TAG, "stopRecording");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;    // for rejecting newer frame    //用于拒绝新的图像帧 ;
            mSync.notifyAll();
            mHandler.removeMessages(MSG_FRAME_AVAILABLE);
            // request endoder handler to stop encoding         //请求endoder处理停止编码
            mHandler.sendEmptyMessage(MSG_STOP_RECORDING);
            // We can not know when the encoding and writing finish.        //我们不知道什么时候编码和 写操作结束 ,所以我们需要在请求停止信号后立刻返回以避免延迟调用的线程 ;
            // so we return immediately after request to avoid delay of caller thread
        }
    }

    //********************************************************************************
    //********************************************************************************

    /**
     * Method to request stop recording
     * this method is called from message hander of EncoderHandler
     * 请求停止记录的方法
     * 这种方法是从encoderhandler监听消息称
     */
    private final void handleStopRecording() {
        if (DEBUG)
            Log.d(TAG, "handleStopRecording");
        // process all available output data        //处理所有可用的输出数据
        drain();
        // request stop recording                   //要求停止记录
        signalEndOfInputStream();
        // process output data again for EOS signale    //流程的输出数据了EOS清楚了
        drain();
        // release all related objects      //释放所有相关的对象
        release();
    }

    /**
     * Release all releated objects         //释放所有相关的对象
     */
    protected void release() {
        if (DEBUG)
            Log.d(TAG, "release:");
        try {
            mListener.onStopped(this);
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "failed onStopped", e);
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
//                Log.i(TAG, " ++++---------- 结束 ! step 1---------+++");
            } catch (final Exception e) {
                e.printStackTrace();
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
        }
        if (mMuxerStarted) {
            final MediaMuxerWrapper muxer = mWeakMuxer.get();

//            Log.i(TAG, " ++++---------- 结束 ! step 2---------+++ " + (mWrapper != null) + " MediaMuxerWrapper : muxer " + (muxer != null));
            if (muxer != null) {
//                Log.i(TAG, " ++++---------- 结束 ! step 3---------+++");
                try {
                    muxer.stop();        //停止录像编码 :
//                    Log.i(TAG, " ++++---------- 结束 ! step 4---------+++");
                } catch (final Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "failed stopping muxer", e);
                }
            }
            //2016.07.07 新增 :
            mWrapper = null;   //任务完成 , 将强引用清空 ;
        }
        mBufferInfo = null;
    }

    protected void signalEndOfInputStream() {
        if (DEBUG)
            Log.d(TAG, "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty BYTE_BUFFER with BUFFER_FLAG_END_OF_STREAM flag.
        //		mMediaCodec.signalEndOfInputStream();	// API >= 18
        encode(null, 0, getPTSUs());
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     * 将字节数组设置到mediacodec编码器的方法
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final byte[] buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing)
            return;
        int ix = 0, sz;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();    //拿到输入缓冲区,用于传送数据进行编码
        while (mIsCapturing && ix < length) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);  //得到当前有效的输入缓冲区的索引 //dequeueInputBuffer()通知编码器MediaCodec结束工作 ;
            if (inputBufferIndex >= 0) {    //当输入缓冲区有效时,就是>=0
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                sz = inputBuffer.remaining();
                sz = (ix + sz < length) ? sz : length - ix;
                if (sz > 0 && (buffer != null)) {
                    inputBuffer.put(buffer, ix, sz);    //往输入缓冲区写入数据,关键点
                }
                ix += sz;
                //	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                    if (DEBUG)
                        Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM); //将缓冲区入队 //结束流缓冲标志 ;
                    break;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, sz,
                            presentationTimeUs, 0);     //将缓冲区入队
                }
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode               //等待mediacodec编码器准备编码
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)   //不在这里是因为mediacodec # dequeueinputbuffer（timeout_usec）
                // will wait for maximum TIMEOUT_USEC(10msec) on each call      //将等待的最大timeout_usec（10msec）在每次调用
            }
        }
    }

    /**
     * drain encoded data and write them to muxer       //导出编码数据 并将它们写到 转换器 ;
     */
    protected void drain() {        //开始H264的编码:
        if (mMediaCodec == null)
            return;
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();     //拿到输出缓冲区,用于取到编码后的数据
        int encoderStatus, count = 0;
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        if (muxer == null) {
            //        	throw new NullPointerException("muxer is unexpectedly null");   //抛出空指针异常 :
            Log.w(TAG, "muxer is unexpectedly null");
            return;
        }
        LOOP:
        while (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])    //获取最大TIMEOUT_USEC(=10[msec])持续超时时间的编码数据
            //调用dequeueInputBuffer()方法来获得这个用来作为媒体文件源码的ByteBuffer（从输入的buffers的数组中）的索引位置。
            //通过调用queueInputBuffer()方法来释放缓存区的所有权 ;
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);     //拿到输出缓冲区的索引

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come    //等待5秒（= timeout_usec x 5 = 50msec）直到接收到停止数据/ EOS来
                if (!mIsEOS) {
                    if (++count > 5)
                        break LOOP;        // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (DEBUG)
                    Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();      //拿到输出缓冲区,用于取到编码后的数据
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (DEBUG)
                    Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mMuxerStarted) {    // second time request is error
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                //特别注意此处的调用 :
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                //如果要合成视频和音频,需要处理混合器的音轨和视轨的添加.因为只有添加音轨和视轨之后,写入数据才有效
                mTrackIndex = muxer.addTrack(format);       //为MediaMuxer添加数据通道 , 返回通道数 ;
                mMuxerStarted = true;
                if (!muxer.start()) {           //开始录像 ;
                    // we should wait until muxer is ready       //我们应该等到器准备就绪
                    synchronized (muxer) {
                        while (!muxer.isStarted())
                            try {
                                muxer.wait(100);
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                                break LOOP;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status         //意想不到的状况
                if (DEBUG)
                    Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                //走到这里的时候,说明数据已经编码成H264格式了 :
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];     //encoderOutputBuffers保存的就是H264数据了
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error       //这不应该…可能是一个mediacodec内部错误
                    Log.e(TAG, ".may be a MediaCodec internal error");
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from BYTE_BUFFER data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    /**
                     * 你可以设置输出格式器在这里当你目标Android4.3或更少
                     但mediacodec # getoutputformat不能在这里打电话（因为info_output_format_changed还不出来）
                     因此，我们应该扩大和准备从byte_buffer数据输出格式。
                     此示例是为“接口”= 18（=安卓4.3），请忽略此标志在这里
                     */
                    if (DEBUG)
                        Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter     //已准备好的编码数据，清除等待计数器
                    count = 0;
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will prrograming failure.      //复用器是没有准备好…这将prrograming失败。
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.   //写入编码数据复用器（需要调整presentationtimeus。
                    //info.presentationTimeUs 必须给出正确的时间戳，注意单位是 us，例如，对于帧率为 x f/s 的视频而言，时间戳的间隔就是 1000/x ms
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return BYTE_BUFFER to encoder    //返回byte_buffer编码器
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);      //释放资源 // 当数据灌完后，要使用releaseOutputBuffer，把缓冲区释放掉。否则，你会发现queueInputBuffer总是返回-1，因为没有空闲的缓冲区了。
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mMuxerStarted = mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }

    /**
     * previous presentationTimeUs for writing      //写入的前一个时间戳 :
     */
    private long prevOutputPTSUs = 0;

    /**
     * get next encoding presentationTimeUs      //取下一个编码的时间戳 : presentationtimeus
     *
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        /**
         * 时间戳presentationtimeus应该单调
         *否则无法写入转换器
         */
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    /**
     * Handler class to handle the asynchronous request to encoder thread       //处理异步请求到编码器线程的处理程序类
     */
    private static final class EncoderHandler extends Handler {
        private final SoftReference<MediaEncoderRunnable> mWeakEncoder;

        public EncoderHandler(final MediaEncoderRunnable encoder) {
            mWeakEncoder = new SoftReference<MediaEncoderRunnable>(encoder);
        }

        /**
         * message handler
         */
        @Override
        public void handleMessage(final Message inputMessage) {
            final int what = inputMessage.what;
            final MediaEncoderRunnable encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler#handleMessage: encoder is null");
                return;
            }
            switch (what) {
                case MSG_FRAME_AVAILABLE:
                    encoder.drain();
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("unknown message what=" + what);
            }
        }
    }

}
