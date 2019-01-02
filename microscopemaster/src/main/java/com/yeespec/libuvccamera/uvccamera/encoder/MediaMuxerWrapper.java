package com.yeespec.libuvccamera.uvccamera.encoder;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: MediaMuxerWrapper.java
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
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.utils.PictureUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MediaMuxerWrapper {
    private static final boolean DEBUG = true;    // TODO set false on release
    //	private static final String TAG = "MediaMuxerWrapper";
    private static final String TAG = "testMediaMuxerWrapper";

    //  private static final String DIR_NAME = "USBCameraTest";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    private String mOutputPath;
    private final MediaMuxer mMediaMuxer;    // API >= 18
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    private MediaEncoderRunnable mVideoEncoder, mAudioEncoder;

    /**
     * Constructor      构造器
     *
     * @param ext extension of output file      输出文件扩展 , 输出文件后缀 ;
     * @throws IOException
     */
    public MediaMuxerWrapper(String ext) throws IOException {
        if (TextUtils.isEmpty(ext))
            ext = ".mp4";
        try {
            mOutputPath = getCaptureFile(Environment.DIRECTORY_MOVIES, ext,"").toString();
        } catch (final NullPointerException e) {
            e.printStackTrace();
            //这个应用程序没有写入外部存储的权限 异常 :
            throw new RuntimeException("This app has no permission of writing external storage");
        }
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
    }

    public String getOutputPath() {
        return mOutputPath;
    }

    public void prepare() throws IOException {
        if (mVideoEncoder != null)
            mVideoEncoder.prepare();
        if (mAudioEncoder != null)
            mAudioEncoder.prepare();
    }

    public void startRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.startRecording();
        if (mAudioEncoder != null)
            mAudioEncoder.startRecording();
    }

    public void stopRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.stopRecording();
        mVideoEncoder = null;
        if (mAudioEncoder != null)
            mAudioEncoder.stopRecording();
        mAudioEncoder = null;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    //**********************************************************************
    //**********************************************************************

    /**
     * assign encoder to this calss. this is called from encoder.       //这类分配编码器。这是从编码器调用。
     *
     * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder   //对 MediaVideoEncoder 或 MediaAudioEncoder 实例
     */
    /*package*/ void addEncoder(final MediaEncoderRunnable encoder) {   //添加编码器通道 : 1:视频编码通道 ; 2:音频编码通道;
        if (encoder instanceof MediaSurfaceEncoder) {
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioEncoder) {
            if (mAudioEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mAudioEncoder = encoder;
        } else
            throw new IllegalArgumentException("unsupported encoder");
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder      //从编码器的请求开始记录
     *
     * @return true when muxer is ready to write
     */
    /*package*/
    synchronized boolean start() {
        if (DEBUG)
            Log.v(TAG, "start:");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            if (DEBUG)
                Log.v(TAG, "MediaMuxer started:");
        }
        if (DEBUG)
            Log.v(TAG, "MediaMuxer started: mStatredCount = " + mStatredCount + " mEncoderCount=" + mEncoderCount);
        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS    //要求从编码器停止记录时，编码器接收EOS
     */
    /*package*/
    synchronized void stop() {
        if (DEBUG)
            Log.v(TAG, "stop:mStatredCount=" + mStatredCount + " mEncoderCount=" + mEncoderCount);
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            mMediaMuxer.stop();
            //2016.07.06 : 新增 : //释放转换器资源 :
            mMediaMuxer.release();
            mIsStarted = false;
            if (DEBUG)
                Log.v(TAG, "MediaMuxer stopped:" + " ++++---------- 结束 !---------+++");
        }
    }

    /**
     * assign encoder to muxer      //指定编码器 转换器
     *
     * @param format
     * @return minus value indicate error
     */
    /*package*/
    synchronized int addTrack(final MediaFormat format) {       //为MediaMuxer添加数据通道 , 返回通道数 ;
        if (mIsStarted)
            throw new IllegalStateException("muxer already started");
        final int trackIx = mMediaMuxer.addTrack(format);
        if (DEBUG)
            Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
        return trackIx;
    }

    /**
     * write encoded data to muxer      //写入编码数据到 合成器 复用器
     * // writeSampleData()由MediaMuxer向mp4文件写入数据 ;
     * <p/>
     * 注意writeSampleData函数的最后一个参数是一个BufferInfo对象，你必须认真地填入“正确”的值 !!
     * <p/>
     * BufferInfo info = new BufferInfo();
     * info.offset = 0;
     * info.size = sampleSize;
     * info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
     * info.presentationTimeUs = timestamp;
     * 其中，
     * info.size 必须填入数据的大小
     * info.flags 需要给出是否为同步帧/关键帧
     * info.presentationTimeUs 必须给出正确的时间戳，注意单位是 us，例如，对于帧率为 x f/s 的视频而言，时间戳的间隔就是 1000/x ms
     *
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
    /*package*/
    synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStatredCount > 0)
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);       // writeSampleData()由MediaMuxer向mp4文件写入数据 ;
    }

    //**********************************************************************
    //**********************************************************************

    /**
     * generate output file
     *
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext  .mp4(.m4a for audio) or .png 创建文件存储路径
     * @return return null when this app has no writing permission to external storage.
     */
   /* public static final File getCaptureFile(final String type, final String ext) {

        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), BaseApplication.DIR_NAME);
        Log.d(TAG, "path=" + dir.toString());
        dir.mkdirs();
        if (dir.canWrite()) {

            return new File(dir, getDateTimeString() + ext);    //创建以当前时间为名字的 , 以ext为拓展后缀的文件 ;
        }
        return null;
    }*/
    public static  File getCaptureFile( String type, String ext,String mkdirs) {

        File dir=null;
        if(mkdirs.equals("")){
            dir= new File(Environment.getExternalStoragePublicDirectory(type), BaseApplication.DIR_NAME);
        }else
            dir = new File(Environment.getExternalStoragePublicDirectory(type), BaseApplication.DIR_NAME+"/"+mkdirs);

        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, ext);    //创建以当前时间为名字的 , 以ext为拓展后缀的文件 ;
        }
        return null;
    }
    /**
     * get current date and time as String       //获取当前日期和时间作为字符串
     *
     * @return
     */
    public static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }

}
