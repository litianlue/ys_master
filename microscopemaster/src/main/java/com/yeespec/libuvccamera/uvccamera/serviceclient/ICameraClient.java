package com.yeespec.libuvccamera.uvccamera.serviceclient;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: ICameraClient.java
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
 *   相机各种接口
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.hardware.usb.UsbDevice;
import android.view.Surface;

public interface ICameraClient {
    public UsbDevice getDevice();
    public void select(UsbDevice device);
    public void release();
    public void resize(int width, int height);
    public void connect();
    public void disconnect();
    public void addSurface(Surface surface, boolean isRecordable);
    public void removeSurface(Surface surface);
    public void startRecording(int filenumber);
    public void stopRecording();
    public boolean isRecording();
    public void captureStill(String path);
    public void setBrightness(int bright_abs);
    public int getBrightness();
    public void setSaturation(int saturation_abs);
    public int getSaturation();
    public void setContrast(int contrast_abs);
    public int getContrast();
    public void setGain(int gain_abs);
    public int getGain();
    public void setGamma(int gamma);
    public int getGamma();

    //2016.09.14 : 新增 : 用于待机唤醒后重新设置并开始预览图像 :
    public void restartPreview();

}