/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\project\\yeespec\\20181231\\MicroscopeMaster\\microscopemaster\\src\\main\\aidl\\com\\yeespec\\libuvccamera\\uvccamera\\service\\IUVCSlaveService.aidl
 */
package com.yeespec.libuvccamera.uvccamera.service;
//在com.serenegiant.service.UVCService实现的IUVCSlaveService.aidl远程接口;
//未发现具体被调用的节点 , 接口的四个方法都在UVCService内重复了 ;

public interface IUVCSlaveService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService
{
private static final java.lang.String DESCRIPTOR = "com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService interface,
 * generating a proxy if needed.
 */
public static com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService))) {
return ((com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService)iin);
}
return new com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService.Stub.Proxy(obj);
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
case TRANSACTION_isSelected:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.isSelected(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isConnected:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.isConnected(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_addSurface:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
android.view.Surface _arg2;
if ((0!=data.readInt())) {
_arg2 = android.view.Surface.CREATOR.createFromParcel(data);
}
else {
_arg2 = null;
}
boolean _arg3;
_arg3 = (0!=data.readInt());
com.yeespec.libuvccamera.uvccamera.service.IUVCServiceOnFrameAvailable _arg4;
_arg4 = com.yeespec.libuvccamera.uvccamera.service.IUVCServiceOnFrameAvailable.Stub.asInterface(data.readStrongBinder());
this.addSurface(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_removeSurface:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.removeSurface(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.yeespec.libuvccamera.uvccamera.service.IUVCSlaveService
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
//未曾使用 ,

@Override public boolean isSelected(int serviceID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(serviceID);
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
@Override public boolean isConnected(int serviceID) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(serviceID);
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
@Override public void addSurface(int serviceID, int id_surface, android.view.Surface surface, boolean isRecordable, com.yeespec.libuvccamera.uvccamera.service.IUVCServiceOnFrameAvailable callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(serviceID);
_data.writeInt(id_surface);
if ((surface!=null)) {
_data.writeInt(1);
surface.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeInt(((isRecordable)?(1):(0)));
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addSurface, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeSurface(int serviceID, int id_surface) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(serviceID);
_data.writeInt(id_surface);
mRemote.transact(Stub.TRANSACTION_removeSurface, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_isSelected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_isConnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_addSurface = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_removeSurface = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
}
//未曾使用 ,

public boolean isSelected(int serviceID) throws android.os.RemoteException;
public boolean isConnected(int serviceID) throws android.os.RemoteException;
public void addSurface(int serviceID, int id_surface, android.view.Surface surface, boolean isRecordable, com.yeespec.libuvccamera.uvccamera.service.IUVCServiceOnFrameAvailable callback) throws android.os.RemoteException;
public void removeSurface(int serviceID, int id_surface) throws android.os.RemoteException;
}
