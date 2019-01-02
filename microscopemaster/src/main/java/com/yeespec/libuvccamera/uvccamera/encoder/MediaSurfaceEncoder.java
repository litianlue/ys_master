package com.yeespec.libuvccamera.uvccamera.encoder;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: MediaSurfaceEncoder.java
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
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.yeespec.libuvccamera.usb.UVCCamera;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaSurfaceEncoder extends MediaEncoderRunnable {
    private static final boolean DEBUG = true;    // TODO set false on release
    //	private static final String TAG = "MediaSurfaceEncoder";
    private static final String TAG = "testMediaSurfaceEncoder";

    public static final boolean VIDEO_DATA_RESCOURCE_CHOOSE = true;    //true 为从 surface获取视频数据资源 ; false为从jni底层获取数据资源 ;

    private static final String MIME_TYPE = "video/avc";    // H.264的mime类型
    // parameters for recording
    // VIDEO_WITH and VIDEO_HEIGHT should be same as the camera preview size.
    private final int VIDEO_WIDTH;
    private final int VIDEO_HEIGHT;
    private static final int FRAME_RATE = 15;
    private static final float BPP = 0.125f;

    private Surface mSurface;

    private VideoThread mVideoThread = null;

    //    public static final Bitmap BITMAP = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);//RGB565
    public Bitmap BITMAP;

    public static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocateDirect(UVCCamera.DEFAULT_PREVIEW_WIDTH * UVCCamera.DEFAULT_PREVIEW_HEIGHT * 4);

    public static byte[] mPixelsArrayBuffer;

    public MediaSurfaceEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        this(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, muxer, listener);
    }

    public MediaSurfaceEncoder(final int width, final int height, final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        super(muxer, listener);
        if (DEBUG)
            Log.i(TAG, "MediaVideoEncoder: ");
        VIDEO_WIDTH = width;
        VIDEO_HEIGHT = height;
    }

    /**
     * Returns the encoder's input surface.		//返回编码器的输入面。
     */
    public Surface getInputSurface() {
        return mSurface;
    }

    @Override
    protected void prepare() throws IOException {   //首先初始化编码器 ;
        if (DEBUG)
            Log.i(TAG, "prepare: ");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);    //选择系统用于编码H264的编码器信息,固定的调用
        if (videoCodecInfo == null) {

            return;
        }
        if (DEBUG)
            Log.i(TAG, "selected codec: " + videoCodecInfo.getName());

        final MediaFormat videoFormat = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);     //根据MIME创建MediaFormat,固定的调用

        if (VIDEO_DATA_RESCOURCE_CHOOSE) {
            //以下参数的设置,尽量固定.当然,如果你非常了解,也可以自行修改
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18  //设置颜色格式
        } else {

            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB4444);   //ARGB_8888 // API >= 18  //设置颜色格式
            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888);   //ARGB_8888 // API >= 18  //设置颜色格式
            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format8bitRGB332);   //ARGB_8888 // API >= 18  //设置颜色格式
            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444);   //ARGB_8888 // API >= 18  //设置颜色格式
            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB1555);   //ARGB_8888 // API >= 18  //设置颜色格式
            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444);   //ARGB_8888 // API >= 18  //设置颜色格式

            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar);   //2130706688 //ARGB_8888 // API >= 18  //设置颜色格式
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);   //2130708361 //ARGB_8888 // API >= 18  //设置颜色格式
            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);   //2135033992 //ARGB_8888 // API >= 18  //设置颜色格式
            //            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar);   //2141391872 //ARGB_8888 // API >= 18  //设置颜色格式

            //OMX :

        }

        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());     //设置比特率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);      //设置帧率
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);        //设置关键帧的时间
        if (DEBUG)
            Log.i(TAG, "format: " + videoFormat);

        //这里就是根据上面拿到的编码器创建一个MediaCodec了;//MediaCodec还有一个方法可以直接用MIME类型,创建
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);    //按类型创建编码器
        //第二个参数用于播放MP4文件,显示图像的Surface;
        //第四个参数,编码H264的时候,固定CONFIGURE_FLAG_ENCODE, 播放的时候传入0即可;API文档有解释
        mMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);    //关键方法
        // get Surface for encoder input		//获取编码器输入的表面
        // this method only can call between #configure and #start		//这种方法只能在#配置和#开始之间调用

        if (VIDEO_DATA_RESCOURCE_CHOOSE) {
            mSurface = mMediaCodec.createInputSurface();    // API >= 18
        } else {
        }

        mMediaCodec.start();    //必须
        if (DEBUG)
            Log.i(TAG, "prepare finishing");
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                e.printStackTrace();

            }
        }
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (VIDEO_DATA_RESCOURCE_CHOOSE) {

        } else {
            if (mVideoThread == null) {
                mVideoThread = new VideoThread();
                mVideoThread.start();
            }
        }
    }

    @Override
    protected void release() {
        if (DEBUG)
            Log.i(TAG, "release: ");
        if (mVideoThread != null) {
            mVideoThread = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        super.release();
    }


    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * VIDEO_WIDTH * VIDEO_HEIGHT);

        return bitrate;
    }

    /**
     * select the first codec that match a specific MIME type		 //选择第一编解码器，一个特定的MIME类型匹配
     *
     * @param mimeType
     * @return null if no codec matched			//返回空 如果没有匹配解码器匹配
     */
    protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
        if (DEBUG)
            Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (DEBUG)
                        Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);    //根据MIME格式,选择颜色格式,固定的调用
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.	//选择特定的解码器上的颜色格式，我们可以使用。
     * //根据MIME格式,选择颜色格式,固定的调用
     *
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        if (DEBUG)
            Log.i(TAG, "selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
                MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888,
                MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565,
                MediaCodecInfo.CodecCapabilities.COLOR_Format32bitBGRA8888,
                MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar,
        };
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
        if (DEBUG)
            Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }


    /**
     * 从底层jni获取视频数据用于视频录制 :
     */
    private class VideoThread extends Thread {
        @Override
        public void run() {     //获取音频设备,用于获取音频数据:
            try {
                //                final int buf_sz = AudioRecord.getMinBufferSize(
                //                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4;
                //                final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                //                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buf_sz);

                if (mIsCapturing) {
                    if (DEBUG)
                        Log.v(TAG, "AudioThread:start audio recording" + " === ");
                    //                        final byte[] buf = new byte[buf_sz];
                    //2016.08.18 : 修改去掉final修饰 ;
                    //                        byte[] buf = new byte[1360 * 1040 * 4];
                    //                        int readBytes = 1360 * 1040 * 4;

                    int CAPTURE_BYTE_COUNT = VIDEO_WIDTH * VIDEO_HEIGHT * 4;
                    //                        byte[] mPixelsArrayBuffer;

                    //                        audioRecord.startRecording();       //固定写法
                    if (mIsCapturing && !mRequestStop && !mIsEOS) {
                    } else {
                        Log.i("test_mediasur", " mIsCapturing = " + mIsCapturing + " mRequestStop = " + mRequestStop + " mIsEOS = " + mIsEOS + " === ");
                    }
                    while (mIsCapturing && !mRequestStop && !mIsEOS) {  //  true/false/false
                        // read audio data from internal mic    //从内部麦克风读取音频数据
                        //                                readBytes = audioRecord.read(buf, 0, buf_sz);   //读取音频数据到buf
                        //                                if (readBytes > 0) {

                        //2016.08.18 : 应应用需求需要屏蔽录像的声音 , 所以修改读取到的音频源数据 , 将音频数据清空置零 :
                        //                                    buf = new byte[buf_sz];

                        //                                synchronized (BYTE_BUFFER) {
                        synchronized (mPixelsArrayBuffer) {

                            //                                    byte[] frameArrayBytes = new byte[0];
                            //
                            //                                    if (BYTE_BUFFER != null) {
                            //                                        if (BYTE_BUFFER.array() != null) {
                            //
                            //                                            frameArrayBytes = BYTE_BUFFER.array();
                            //                                        } else {
                            //                                            Log.i("test_mediasur", "BYTE_BUFFER.array() == null ");
                            //                                        }
                            //                                    } else {
                            //                                        Log.i("test_mediasur", " BYTE_BUFFER == null ");
                            //                                    }

                            //                                    if (BITMAP.getWidth() == VIDEO_WIDTH && BITMAP.getWidth() == VIDEO_HEIGHT) {
                            if (mPixelsArrayBuffer.length == CAPTURE_BYTE_COUNT) {
                                //                                        mPixelsArrayBuffer = new byte[CAPTURE_BYTE_COUNT];//保存所有的像素的数组，图片宽×高
                                //                                        BITMAP.getPixels(mPixelsArrayBuffer, 0, VIDEO_WIDTH, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
                                //                                        BITMAP.copyPixelsToBuffer();

                                // set audio data to encoder    //将音频数据设置为编码器
                                encode(mPixelsArrayBuffer, CAPTURE_BYTE_COUNT, getPTSUs());     //开始编码
                                //                                        encode(frameArrayBytes, CAPTURE_BYTE_COUNT, getPTSUs());     //开始编码
                                frameAvailableSoon();

                                Log.i("test_mediasur", " while here ! === ");

                            } else {
                                //                                        Log.i("test_mediasur", " frameArrayBytes.length = " + frameArrayBytes.length);
                                Log.i("test_mediasur", " mPixelsArrayBuffer.length = " + mPixelsArrayBuffer.length);
                            }
                            Log.i("test_mediasur", " here ! === ");
                            //                                }
                            //                                }
                        }
                    }
                    frameAvailableSoon();

                } else {
                    Log.i("test_mediasur", " mIsCapturing = false ! ");
                }


            } catch (final Exception e) {
                e.printStackTrace();
                Log.e(TAG, "AudioThread#run" + " === ", e);
            }
            if (DEBUG)
                Log.v(TAG, "AudioThread:finished" + " === ");
        }
    }

}
