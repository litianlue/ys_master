package com.yeespec.libuvccamera.uvccamera.service;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 * 
 * Copyright (c) 2014 Mr_Wen Mr_Wen@yeespec.com
 * 
 * File name: IUVCServiceCallback.aidl
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

//在com.serenegiant.serviceclient.CameraClient实现的IUVCServiceCallback.aidl远程接口; (其实接口的最终实现是ICameraClientCallback)
//在com.serenegiant.service.CameraServerHandler中被远程调用
interface IUVCServiceCallback {
	oneway void onConnected();      //当后台进程连接到UVC时回调该方法, 使前台的进程进行相应操作
	oneway void onDisConnected();    //当后台进程断开连接到UVC时回调该方法, 使前台的进程进行相应操作
}