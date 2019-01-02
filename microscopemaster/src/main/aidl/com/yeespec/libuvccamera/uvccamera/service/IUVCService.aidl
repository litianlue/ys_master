package com.yeespec.libuvccamera.uvccamera.service;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 * 
 * Copyright (c) 2014 Mr_Wen Mr_Wen@yeespec.com
 * 
 * File name: IUVCService.aidl
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

import com.yeespec.libuvccamera.uvccamera.service.IUVCServiceCallback;
import android.hardware.usb.UsbDevice;
import android.view.Surface;

/**
	<select						select UVC camera
		<connect				open device and start streaming
		disconnect>				stop streaming and close device
	release>					release camera
*/

//在com.serenegiant.service.UVCService实现的IUVCService.aidl远程接口;
//在com.serenegiant.serviceclient.CameraClient中中被远程调用 ;
interface IUVCService {
	int select(in UsbDevice device, IUVCServiceCallback callback);
	void release();
	boolean isSelected();
	void releaseAll();
	void resize(int width, int height);
	void connect();
	void disconnect();
	boolean isConnected(int  constant,int gamma);
	void addSurface(int id_surface, in Surface surface, boolean isRecordable);
	void removeSurface(int id_surface);
	boolean isRecording();
	void startRecording(int number);
	void stopRecording();
	void captureStillImage(String path);
	void cameraControl(int control_type, int control_parameter);
	void setCameraAutoFocus(boolean is_auto_focus);
    int cameraParameter(int parameter_type);
    void clientControl(boolean type);
    void setColor(int color);
    void setGray(int gray);
    void capture(String path,String pathrequest,String pathscale);

    //先在aidl中定义方法名; 再在java文件中定义方法实体 ; 然后就可以当作远程服务来调用了 !
    void restartPreview();
    void sotpProcess();
    int getBackProcessPid();//获取后台进程的PID
    boolean isConnectUSB();//判断是否连接USB
    void reStartCamera();
    void stopCamer();
    void sendAutoFocusMessege(int count,int x,int y,boolean isstop,int movestep);
    void mSetGamma(int focus,boolean isForward,int currentstep,String speed);
    void releadFocus();
    boolean getAutoFocusResult();
    int getUartGamma(int uartgamma);
    void setUartGamma(int uartgamma);
    float getOperationBrignes();//读取图片亮度值
    float getFreshnes();

    int getMaxBrightness();//获取最大亮度值
    boolean getObjectiveIsSwith(boolean isOn);//判断物镜是否在切换中

    void updateMessagePhoneMethod(String type,String number);//更新数据到手机端
    void uadateTPicterFileName(String filename);//更新缩略图文件名
    void sendToBackgroundProgram(String type,String message);//发送给其他进程字符消息
    void sendStringToUart(String str);

    void closeUVC();
    void openUVC();
    void sendRecolorString(String recolorstring);
    void correctionPictureXY(int x,int y);
    int getSaturationState();//查询激发快位置
    void setResponeModel(boolean remote);//设置响应模式 true为远程
    int getPicPercentage(int temp);//获取图片的二值化百分比
    int getMaxNuberType(int type);//获取摄像头最大参数 type 1 亮度 2 iso
    void setRockerState(String state);
    int getAppRestartState();

}
