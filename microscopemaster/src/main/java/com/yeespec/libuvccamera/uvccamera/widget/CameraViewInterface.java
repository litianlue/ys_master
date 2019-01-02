package com.yeespec.libuvccamera.uvccamera.widget;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: CameraViewInterface.java
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
import android.view.Surface;

import com.yeespec.libuvccamera.uvccamera.encoder.MediaEncoderRunnable;

public interface CameraViewInterface extends AspectRatioViewInterface {
	public interface Callback {
		//当SurfaceView第一次创建后会立即调用该函数 , 程序可以在该函数中做些和绘制界面相关的初始化工作 , 一般都是在另外的线程来绘制界面 , 所以不要在这个函数绘制Surface
		public void onSurfaceCreated(Surface surface);
		//当Surface的状态(大小和格式)发生变化的时候会调用该函数 , 在SurfaceCreate调用后该函数至少会被调用一次 :
		public void onSurfaceChanged(Surface surface, int width, int height);
		//当SurfaceView被摧毁前会调用该函数 , 该函数被调用后就不能继续使用Surface了 , 一般在该函数中来清理使用的资源 :
		public void onSurfaceDestroy(Surface surface);
	}
	public void setCallback(Callback callback);
	public boolean hasSurface();
    public Surface getSurface();
	public void setVideoEncoder(final MediaEncoderRunnable encoder);
	public Bitmap captureStillImage();
}
