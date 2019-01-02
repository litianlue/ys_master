package com.yeespec.libuvccamera.uvccamera.service;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 * 
 * Copyright (c) 2014 Mr_Wen Mr_Wen@yeespec.com
 * 
 * File name: IUVCServiceOnFrameAvailable.aidl
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
 * Files in the jni/libjpeg, jni/libusb and jin/libuvc folder may have a different license, see the respective files.
*/

//在com.serenegiant.service.CameraServerHandler实现的IUVCServiceOnFrameAvailable.aidl远程接口;
//在com.serenegiant.glutils.RenderHolderRunnable中被远程调用 ,
interface IUVCServiceOnFrameAvailable {
	oneway void onFrameAvailable();     //未曾使用 , 用于在jni层调用数据帧返回 , 被IFrameCallback代替 ;

}

