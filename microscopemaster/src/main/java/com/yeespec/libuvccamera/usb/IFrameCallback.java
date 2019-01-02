package com.yeespec.libuvccamera.usb;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: IFrameCallback.java
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

import java.nio.ByteBuffer;

/**
 * Callback interface for UVCCamera class
 * If you need frame data as ByteBuffer, you can use this callback interface with UVCCamera#setFrameCallback
 * uvccamera回调接口的类 :
 * 如果你需要帧数据字节缓冲区，您可以使用此回调接口uvccamera # setframecallback
 */
public interface IFrameCallback {
    /**
     * This method is called from native library via JNI on the same thread as UVCCamera#startCapture.
     * You can use both UVCCamera#startCapture and #setFrameCallback
     * but it is better to use either for better performance.
     * You can also pass pixel format type to UVCCamera#setFrameCallback for this method.
     * Some frames may drops if this method takes a time.
     * <p/>
     * 这种方法是从本地库中通过JNI相同的线程上uvccamera # startcapture称。
     * 你可以使用uvccamera # startcapture和# setframecallback但最好是使用性能更好的。
     * 你也可以通过像素格式类型uvccamera # setframecallback本方法。
     * 一些帧可能下降如果这种方法需要时间。
     *
     * 此方法在 com.serenegiant.service . UVCService 中实现 :
     *
     * @param frame
     */
    public void onFrame(ByteBuffer frame);      //在UVCPreview的do_capture_callback中最终回调onFrame :
}
