package com.yeespec.libuvccamera.uvccamera.encoder;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: MediaAudioEncoder.java
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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

public class MediaAudioEncoder extends MediaEncoderRunnable {
    private static final boolean DEBUG = true;    // TODO set false on release
    //	private static final String TAG = "MediaAudioEncoder";
    private static final String TAG = "test_MediaAudioEncoder";

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;

    private AudioThread mAudioThread = null;

    public MediaAudioEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        super(muxer, listener);
    }

    @Override
    protected void prepare() throws IOException {   //准备工作 : 需要配置编码器
        if (DEBUG)
            Log.v(TAG, "prepare:	====	");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;
        // prepare MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE + " === ");
            return;
        }
        if (DEBUG)
            Log.i(TAG, "selected codec: " + audioCodecInfo.getName() + " === ");

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);  //CHANNEL_IN_STEREO 立体声
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        //		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
        //      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
        if (DEBUG)
            Log.i(TAG, "format: " + audioFormat + " === ");
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (DEBUG)
            Log.i(TAG, "prepare finishing" + " === ");
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                e.printStackTrace();
                Log.e(TAG, "prepare:" + " === ", e);
            }
        }
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            mAudioThread.start();
        }
    }

    @Override
    protected void release() {
        mAudioThread = null;
        super.release();
    }

    /**
     * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
     * and write them to the MediaCodec encoder
     */
    private class AudioThread extends Thread {
        @Override
        public void run() {     //获取音频设备,用于获取音频数据:
            try {
                final int buf_sz = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4;
                final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buf_sz);
                try {
                    if (mIsCapturing) {
                        if (DEBUG)
                            Log.v(TAG, "AudioThread:start audio recording" + " === ");
                        //                        final byte[] buf = new byte[buf_sz];
                        //2016.08.18 : 修改去掉final修饰 ;
                        byte[] buf = new byte[buf_sz];
                        int readBytes;
                        audioRecord.startRecording();       //固定写法
                        try {
                            while (mIsCapturing && !mRequestStop && !mIsEOS) {
                                // read audio data from internal mic    //从内部麦克风读取音频数据
                                readBytes = audioRecord.read(buf, 0, buf_sz);   //读取音频数据到buf
                                if (readBytes > 0) {

                                    //2016.08.18 : 应应用需求需要屏蔽录像的声音 , 所以修改读取到的音频源数据 , 将音频数据清空置零 :
                                    buf = new byte[buf_sz];

                                    // set audio data to encoder    //将音频数据设置为编码器
                                    encode(buf, readBytes, getPTSUs());     //开始编码
                                    frameAvailableSoon();

                                }
                            }
                            frameAvailableSoon();
                        } finally {
                            audioRecord.stop();
                        }
                    }
                } finally {
                    audioRecord.release();
                }
            } catch (final Exception e) {
                e.printStackTrace();
                Log.e(TAG, "AudioThread#run" + " === ", e);
            }
            if (DEBUG)
                Log.v(TAG, "AudioThread:finished" + " === ");
        }
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG)
            Log.v(TAG, "selectAudioCodec:" + " === ");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG)
                    Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j] + " === ");
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }

}
