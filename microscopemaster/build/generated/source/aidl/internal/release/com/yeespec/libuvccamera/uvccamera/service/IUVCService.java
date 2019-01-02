/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\project\\yeespec\\20181231\\MicroscopeMaster\\microscopemaster\\src\\main\\aidl\\com\\yeespec\\libuvccamera\\uvccamera\\service\\IUVCService.aidl
 */
package com.yeespec.libuvccamera.uvccamera.service;
/**
	<select						select UVC camera
		<connect				open device and start streaming
		disconnect>				stop streaming and close device
	release>					release camera
*///在com.serenegiant.service.UVCService实现的IUVCService.aidl远程接口;
//在com.serenegiant.serviceclient.CameraClient中中被远程调用 ;

public interface IUVCService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.yeespec.libuvccamera.uvccamera.service.IUVCService
{
private static final java.lang.String DESCRIPTOR = "com.yeespec.libuvccamera.uvccamera.service.IUVCService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.yeespec.libuvccamera.uvccamera.service.IUVCService interface,
 * generating a proxy if needed.
 */
public static com.yeespec.libuvccamera.uvccamera.service.IUVCService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.yeespec.libuvccamera.uvccamera.service.IUVCService))) {
return ((com.yeespec.libuvccamera.uvccamera.service.IUVCService)iin);
}
return new com.yeespec.libuvccamera.uvccamera.service.IUVCService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_select:
{
data.enforceInterface(DESCRIPTOR);
android.hardware.usb.UsbDevice _arg0;
if ((0!=data.readInt())) {
_arg0 = android.hardware.usb.UsbDevice.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
com.yeespec.libuvccamera.uvccamera.service.IUVCServiceCallback _arg1;
_arg1 = com.yeespec.libuvccamera.uvccamera.service.IUVCServiceCallback.Stub.asInterface(data.readStrongBinder());
int _result = this.select(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_release:
{
data.enforceInterface(DESCRIPTOR);
this.release();
reply.writeNoException();
return true;
}
case TRANSACTION_isSelected:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isSelected();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_releaseAll:
{
data.enforceInterface(DESCRIPTOR);
this.releaseAll();
reply.writeNoException();
return true;
}
case TRANSACTION_resize:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.resize(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_connect:
{
data.enforceInterface(DESCRIPTOR);
this.connect();
reply.writeNoException();
return true;
}
case TRANSACTION_disconnect:
{
data.enforceInterface(DESCRIPTOR);
this.disconnect();
reply.writeNoException();
return true;
}
case TRANSACTION_isConnected:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
boolean _result = this.isConnected(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_addSurface:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
android.view.Surface _arg1;
if ((0!=data.readInt())) {
_arg1 = android.view.Surface.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
boolean _arg2;
_arg2 = (0!=data.readInt());
this.addSurface(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_removeSurface:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.removeSurface(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isRecording:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isRecording();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_startRecording:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.startRecording(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_stopRecording:
{
data.enforceInterface(DESCRIPTOR);
this.stopRecording();
reply.writeNoException();
return true;
}
case TRANSACTION_captureStillImage:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.captureStillImage(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_cameraControl:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.cameraControl(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_setCameraAutoFocus:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setCameraAutoFocus(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_cameraParameter:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.cameraParameter(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_clientControl:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.clientControl(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setColor:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setColor(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setGray:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setGray(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_capture:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
this.capture(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_restartPreview:
{
data.enforceInterface(DESCRIPTOR);
this.restartPreview();
reply.writeNoException();
return true;
}
case TRANSACTION_sotpProcess:
{
data.enforceInterface(DESCRIPTOR);
this.sotpProcess();
reply.writeNoException();
return true;
}
case TRANSACTION_getBackProcessPid:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getBackProcessPid();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_isConnectUSB:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isConnectUSB();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_reStartCamera:
{
data.enforceInterface(DESCRIPTOR);
this.reStartCamera();
reply.writeNoException();
return true;
}
case TRANSACTION_stopCamer:
{
data.enforceInterface(DESCRIPTOR);
this.stopCamer();
reply.writeNoException();
return true;
}
case TRANSACTION_sendAutoFocusMessege:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
boolean _arg3;
_arg3 = (0!=data.readInt());
int _arg4;
_arg4 = data.readInt();
this.sendAutoFocusMessege(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_mSetGamma:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _arg1;
_arg1 = (0!=data.readInt());
int _arg2;
_arg2 = data.readInt();
java.lang.String _arg3;
_arg3 = data.readString();
this.mSetGamma(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
return true;
}
case TRANSACTION_releadFocus:
{
data.enforceInterface(DESCRIPTOR);
this.releadFocus();
reply.writeNoException();
return true;
}
case TRANSACTION_getAutoFocusResult:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.getAutoFocusResult();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getUartGamma:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.getUartGamma(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setUartGamma:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setUartGamma(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getOperationBrignes:
{
data.enforceInterface(DESCRIPTOR);
float _result = this.getOperationBrignes();
reply.writeNoException();
reply.writeFloat(_result);
return true;
}
case TRANSACTION_getFreshnes:
{
data.enforceInterface(DESCRIPTOR);
float _result = this.getFreshnes();
reply.writeNoException();
reply.writeFloat(_result);
return true;
}
case TRANSACTION_getMaxBrightness:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getMaxBrightness();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getObjectiveIsSwith:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _result = this.getObjectiveIsSwith(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_updateMessagePhoneMethod:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.updateMessagePhoneMethod(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_uadateTPicterFileName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.uadateTPicterFileName(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_sendToBackgroundProgram:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.sendToBackgroundProgram(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_sendStringToUart:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.sendStringToUart(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_closeUVC:
{
data.enforceInterface(DESCRIPTOR);
this.closeUVC();
reply.writeNoException();
return true;
}
case TRANSACTION_openUVC:
{
data.enforceInterface(DESCRIPTOR);
this.openUVC();
reply.writeNoException();
return true;
}
case TRANSACTION_sendRecolorString:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.sendRecolorString(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_correctionPictureXY:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.correctionPictureXY(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getSaturationState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getSaturationState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setResponeModel:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setResponeModel(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getPicPercentage:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.getPicPercentage(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getMaxNuberType:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.getMaxNuberType(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setRockerState:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.setRockerState(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getAppRestartState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getAppRestartState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.yeespec.libuvccamera.uvccamera.service.IUVCService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public int select(android.hardware.usb.UsbDevice device, com.yeespec.libuvccamera.uvccamera.service.IUVCServiceCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((device!=null)) {
_data.writeInt(1);
device.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_select, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void release() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_release, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isSelected() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isSelected, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void releaseAll() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_releaseAll, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void resize(int width, int height) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(width);
_data.writeInt(height);
mRemote.transact(Stub.TRANSACTION_resize, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void connect() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_connect, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void disconnect() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_disconnect, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isConnected(int constant, int gamma) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(constant);
_data.writeInt(gamma);
mRemote.transact(Stub.TRANSACTION_isConnected, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void addSurface(int id_surface, android.view.Surface surface, boolean isRecordable) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(id_surface);
if ((surface!=null)) {
_data.writeInt(1);
surface.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeInt(((isRecordable)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_addSurface, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeSurface(int id_surface) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(id_surface);
mRemote.transact(Stub.TRANSACTION_removeSurface, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isRecording() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isRecording, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void startRecording(int number) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(number);
mRemote.transact(Stub.TRANSACTION_startRecording, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void stopRecording() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopRecording, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void captureStillImage(java.lang.String path) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(path);
mRemote.transact(Stub.TRANSACTION_captureStillImage, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void cameraControl(int control_type, int control_parameter) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(control_type);
_data.writeInt(control_parameter);
mRemote.transact(Stub.TRANSACTION_cameraControl, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setCameraAutoFocus(boolean is_auto_focus) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((is_auto_focus)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setCameraAutoFocus, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int cameraParameter(int parameter_type) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(parameter_type);
mRemote.transact(Stub.TRANSACTION_cameraParameter, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void clientControl(boolean type) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((type)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_clientControl, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setColor(int color) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(color);
mRemote.transact(Stub.TRANSACTION_setColor, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setGray(int gray) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(gray);
mRemote.transact(Stub.TRANSACTION_setGray, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void capture(java.lang.String path, java.lang.String pathrequest, java.lang.String pathscale) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(path);
_data.writeString(pathrequest);
_data.writeString(pathscale);
mRemote.transact(Stub.TRANSACTION_capture, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
//先在aidl中定义方法名; 再在java文件中定义方法实体 ; 然后就可以当作远程服务来调用了 !

@Override public void restartPreview() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_restartPreview, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sotpProcess() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_sotpProcess, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getBackProcessPid() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getBackProcessPid, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//获取后台进程的PID

@Override public boolean isConnectUSB() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isConnectUSB, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//判断是否连接USB

@Override public void reStartCamera() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_reStartCamera, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void stopCamer() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopCamer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sendAutoFocusMessege(int count, int x, int y, boolean isstop, int movestep) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(count);
_data.writeInt(x);
_data.writeInt(y);
_data.writeInt(((isstop)?(1):(0)));
_data.writeInt(movestep);
mRemote.transact(Stub.TRANSACTION_sendAutoFocusMessege, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void mSetGamma(int focus, boolean isForward, int currentstep, java.lang.String speed) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(focus);
_data.writeInt(((isForward)?(1):(0)));
_data.writeInt(currentstep);
_data.writeString(speed);
mRemote.transact(Stub.TRANSACTION_mSetGamma, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void releadFocus() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_releadFocus, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean getAutoFocusResult() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAutoFocusResult, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getUartGamma(int uartgamma) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(uartgamma);
mRemote.transact(Stub.TRANSACTION_getUartGamma, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setUartGamma(int uartgamma) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(uartgamma);
mRemote.transact(Stub.TRANSACTION_setUartGamma, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public float getOperationBrignes() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
float _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getOperationBrignes, _data, _reply, 0);
_reply.readException();
_result = _reply.readFloat();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//读取图片亮度值

@Override public float getFreshnes() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
float _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getFreshnes, _data, _reply, 0);
_reply.readException();
_result = _reply.readFloat();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getMaxBrightness() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getMaxBrightness, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//获取最大亮度值

@Override public boolean getObjectiveIsSwith(boolean isOn) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((isOn)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_getObjectiveIsSwith, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//判断物镜是否在切换中

@Override public void updateMessagePhoneMethod(java.lang.String type, java.lang.String number) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(type);
_data.writeString(number);
mRemote.transact(Stub.TRANSACTION_updateMessagePhoneMethod, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
//更新数据到手机端

@Override public void uadateTPicterFileName(java.lang.String filename) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filename);
mRemote.transact(Stub.TRANSACTION_uadateTPicterFileName, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
//更新缩略图文件名

@Override public void sendToBackgroundProgram(java.lang.String type, java.lang.String message) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(type);
_data.writeString(message);
mRemote.transact(Stub.TRANSACTION_sendToBackgroundProgram, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
//发送给其他进程字符消息

@Override public void sendStringToUart(java.lang.String str) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(str);
mRemote.transact(Stub.TRANSACTION_sendStringToUart, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void closeUVC() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_closeUVC, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void openUVC() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_openUVC, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sendRecolorString(java.lang.String recolorstring) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(recolorstring);
mRemote.transact(Stub.TRANSACTION_sendRecolorString, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void correctionPictureXY(int x, int y) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(x);
_data.writeInt(y);
mRemote.transact(Stub.TRANSACTION_correctionPictureXY, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getSaturationState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSaturationState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//查询激发快位置

@Override public void setResponeModel(boolean remote) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((remote)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setResponeModel, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
//设置响应模式 true为远程

@Override public int getPicPercentage(int temp) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(temp);
mRemote.transact(Stub.TRANSACTION_getPicPercentage, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//获取图片的二值化百分比

@Override public int getMaxNuberType(int type) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(type);
mRemote.transact(Stub.TRANSACTION_getMaxNuberType, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//获取摄像头最大参数 type 1 亮度 2 iso

@Override public void setRockerState(java.lang.String state) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(state);
mRemote.transact(Stub.TRANSACTION_setRockerState, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getAppRestartState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAppRestartState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_select = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_release = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_isSelected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_releaseAll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_resize = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_connect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_disconnect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_isConnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_addSurface = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_removeSurface = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_isRecording = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_startRecording = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_stopRecording = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_captureStillImage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_cameraControl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_setCameraAutoFocus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_cameraParameter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_clientControl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_setColor = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_setGray = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_capture = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_restartPreview = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
static final int TRANSACTION_sotpProcess = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
static final int TRANSACTION_getBackProcessPid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
static final int TRANSACTION_isConnectUSB = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
static final int TRANSACTION_reStartCamera = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
static final int TRANSACTION_stopCamer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
static final int TRANSACTION_sendAutoFocusMessege = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
static final int TRANSACTION_mSetGamma = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
static final int TRANSACTION_releadFocus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
static final int TRANSACTION_getAutoFocusResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
static final int TRANSACTION_getUartGamma = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
static final int TRANSACTION_setUartGamma = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
static final int TRANSACTION_getOperationBrignes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
static final int TRANSACTION_getFreshnes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
static final int TRANSACTION_getMaxBrightness = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
static final int TRANSACTION_getObjectiveIsSwith = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
static final int TRANSACTION_updateMessagePhoneMethod = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
static final int TRANSACTION_uadateTPicterFileName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
static final int TRANSACTION_sendToBackgroundProgram = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
static final int TRANSACTION_sendStringToUart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
static final int TRANSACTION_closeUVC = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
static final int TRANSACTION_openUVC = (android.os.IBinder.FIRST_CALL_TRANSACTION + 42);
static final int TRANSACTION_sendRecolorString = (android.os.IBinder.FIRST_CALL_TRANSACTION + 43);
static final int TRANSACTION_correctionPictureXY = (android.os.IBinder.FIRST_CALL_TRANSACTION + 44);
static final int TRANSACTION_getSaturationState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 45);
static final int TRANSACTION_setResponeModel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 46);
static final int TRANSACTION_getPicPercentage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 47);
static final int TRANSACTION_getMaxNuberType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 48);
static final int TRANSACTION_setRockerState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 49);
static final int TRANSACTION_getAppRestartState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 50);
}
public int select(android.hardware.usb.UsbDevice device, com.yeespec.libuvccamera.uvccamera.service.IUVCServiceCallback callback) throws android.os.RemoteException;
public void release() throws android.os.RemoteException;
public boolean isSelected() throws android.os.RemoteException;
public void releaseAll() throws android.os.RemoteException;
public void resize(int width, int height) throws android.os.RemoteException;
public void connect() throws android.os.RemoteException;
public void disconnect() throws android.os.RemoteException;
public boolean isConnected(int constant, int gamma) throws android.os.RemoteException;
public void addSurface(int id_surface, android.view.Surface surface, boolean isRecordable) throws android.os.RemoteException;
public void removeSurface(int id_surface) throws android.os.RemoteException;
public boolean isRecording() throws android.os.RemoteException;
public void startRecording(int number) throws android.os.RemoteException;
public void stopRecording() throws android.os.RemoteException;
public void captureStillImage(java.lang.String path) throws android.os.RemoteException;
public void cameraControl(int control_type, int control_parameter) throws android.os.RemoteException;
public void setCameraAutoFocus(boolean is_auto_focus) throws android.os.RemoteException;
public int cameraParameter(int parameter_type) throws android.os.RemoteException;
public void clientControl(boolean type) throws android.os.RemoteException;
public void setColor(int color) throws android.os.RemoteException;
public void setGray(int gray) throws android.os.RemoteException;
public void capture(java.lang.String path, java.lang.String pathrequest, java.lang.String pathscale) throws android.os.RemoteException;
//先在aidl中定义方法名; 再在java文件中定义方法实体 ; 然后就可以当作远程服务来调用了 !

public void restartPreview() throws android.os.RemoteException;
public void sotpProcess() throws android.os.RemoteException;
public int getBackProcessPid() throws android.os.RemoteException;
//获取后台进程的PID

public boolean isConnectUSB() throws android.os.RemoteException;
//判断是否连接USB

public void reStartCamera() throws android.os.RemoteException;
public void stopCamer() throws android.os.RemoteException;
public void sendAutoFocusMessege(int count, int x, int y, boolean isstop, int movestep) throws android.os.RemoteException;
public void mSetGamma(int focus, boolean isForward, int currentstep, java.lang.String speed) throws android.os.RemoteException;
public void releadFocus() throws android.os.RemoteException;
public boolean getAutoFocusResult() throws android.os.RemoteException;
public int getUartGamma(int uartgamma) throws android.os.RemoteException;
public void setUartGamma(int uartgamma) throws android.os.RemoteException;
public float getOperationBrignes() throws android.os.RemoteException;
//读取图片亮度值

public float getFreshnes() throws android.os.RemoteException;
public int getMaxBrightness() throws android.os.RemoteException;
//获取最大亮度值

public boolean getObjectiveIsSwith(boolean isOn) throws android.os.RemoteException;
//判断物镜是否在切换中

public void updateMessagePhoneMethod(java.lang.String type, java.lang.String number) throws android.os.RemoteException;
//更新数据到手机端

public void uadateTPicterFileName(java.lang.String filename) throws android.os.RemoteException;
//更新缩略图文件名

public void sendToBackgroundProgram(java.lang.String type, java.lang.String message) throws android.os.RemoteException;
//发送给其他进程字符消息

public void sendStringToUart(java.lang.String str) throws android.os.RemoteException;
public void closeUVC() throws android.os.RemoteException;
public void openUVC() throws android.os.RemoteException;
public void sendRecolorString(java.lang.String recolorstring) throws android.os.RemoteException;
public void correctionPictureXY(int x, int y) throws android.os.RemoteException;
public int getSaturationState() throws android.os.RemoteException;
//查询激发快位置

public void setResponeModel(boolean remote) throws android.os.RemoteException;
//设置响应模式 true为远程

public int getPicPercentage(int temp) throws android.os.RemoteException;
//获取图片的二值化百分比

public int getMaxNuberType(int type) throws android.os.RemoteException;
//获取摄像头最大参数 type 1 亮度 2 iso

public void setRockerState(java.lang.String state) throws android.os.RemoteException;
public int getAppRestartState() throws android.os.RemoteException;
}
