/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\project\\yeespec\\20181231\\MicroscopeMaster\\microscopemaster\\src\\main\\aidl\\com\\yeespec\\libuvccamera\\uvccamera\\service\\IPackageDataObserver.aidl
 */
package com.yeespec.libuvccamera.uvccamera.service;
// Declare any non-default types here with import statements

public interface IPackageDataObserver extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver
{
private static final java.lang.String DESCRIPTOR = "com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver interface,
 * generating a proxy if needed.
 */
public static com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver))) {
return ((com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver)iin);
}
return new com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver.Stub.Proxy(obj);
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
case TRANSACTION_onRemoveCompleted:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _arg1;
_arg1 = (0!=data.readInt());
this.onRemoveCompleted(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.yeespec.libuvccamera.uvccamera.service.IPackageDataObserver
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
@Override public void onRemoveCompleted(java.lang.String packageName, boolean succeeded) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(packageName);
_data.writeInt(((succeeded)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_onRemoveCompleted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onRemoveCompleted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void onRemoveCompleted(java.lang.String packageName, boolean succeeded) throws android.os.RemoteException;
}
