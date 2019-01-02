package com.yeespec.libuvccamera.usb;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 Mr_Wen Mr_Wen@yeespec.com
 *
 * File name: USBMonitor.java
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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.util.SparseArray;

import com.orhanobut.hawk.Hawk;
import com.yeespec.libuvccamera.uvccamera.service.RestartUtil;
import com.yeespec.microscope.utils.FileUtil;
import com.yeespec.microscope.utils.FileUtils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心代码 :
 * 原理是使用
 * UVCCamera来控制、管理与外接设备的连接，
 * UVCCameraTextureView控件进行图像的预览，
 * USBMonitor进行驱动的连接和断开
 */

public final class USBMonitor {

    private static final boolean DEBUG = false;    // TODO set false on production
    private static final String TAG = "USBMonitor";

    private static final String ACTION_USB_PERMISSION_BASE = "com.yeespec.USB_PERMISSION.";
    private final String ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode();

    public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";

    //ConcurrentHashMap是线程安全的HashMap ; 区别于普通HashMap
    private final ConcurrentHashMap<UsbDevice, UsbControlBlock> mCtrlBlocks = new ConcurrentHashMap<UsbDevice, UsbControlBlock>();
    private final SoftReference<Context> mWeakContext;
    private final UsbManager mUsbManager;
    private final OnDeviceConnectListener mOnDeviceConnectListener;
    private PendingIntent mPermissionIntent = null;
    private List<DeviceFilter> mDeviceFilters = new ArrayList<DeviceFilter>();

    private UsbDevice currentDevice;

    private final Handler mHandler;

    public interface OnDeviceConnectListener {
        /**
         * called when device attached      //当设备连接时调用 ;
         *
         * @param device
         */
        public void onAttach(UsbDevice device);

        /**
         * called when device dettach(after onDisconnect)   //当设备断开连接时调用(在onDisconnect之后调用) ;
         *
         * @param device
         */
        public void onDettach(UsbDevice device);

        /**
         * called after device opend        //当设备打开时调用 ;
         *
         * @param device
         * @param createNew
         */
        public void onConnect(UsbDevice device, UsbControlBlock ctrlBlock, boolean createNew);

        /**
         * called when USB device removed or its power off (this callback is called after device closing)
         * 当USB设备移除或关闭电源（这个回调被称为后设备关闭）
         *
         * @param device
         * @param ctrlBlock
         */
        public void onDisconnect(UsbDevice device, UsbControlBlock ctrlBlock);

        /**
         * called when canceled or could not get permission from user
         * 取消或无法获得用户权限时调用
         */
        public void onCancel();
    }

    public USBMonitor(final Context context, final OnDeviceConnectListener listener) {
        if (DEBUG)
            Log.v(TAG, "USBMonitor:Constructor");
/*		if (listener == null)
            throw new IllegalArgumentException("OnDeviceConnectListener should not null."); */
        mWeakContext = new SoftReference<Context>(context);
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mOnDeviceConnectListener = listener;
        if (DEBUG)
            Log.v(TAG, "USBMonitor:mUsbManager=" + mUsbManager);
        // TODO: 2016/6/17 :    添加Handler的 Looper :
        mHandler = new Handler(context.getMainLooper());

    }

    public void destroy() {
        if (DEBUG)
            Log.i(TAG, "destroy:");
        unregister();
        final Set<UsbDevice> keys = mCtrlBlocks.keySet();
        if (keys != null) {
            UsbControlBlock ctrlBlock;
            try {
                for (final UsbDevice key : keys) {
                    ctrlBlock = mCtrlBlocks.remove(key);
                    ctrlBlock.close();
                }
            } catch (final Exception e) {
                e.printStackTrace();
                Log.e(TAG, "destroy:", e);
            }
            mCtrlBlocks.clear();
        }
    }

    /**
     * register BroadcastReceiver to monitor USB events
     * 注册BroadcastReceiver来监视USB事件
     */
    public synchronized void register() {
        if (mPermissionIntent == null) {
            if (DEBUG)
                Log.i(TAG, "register:");
            final Context context = mWeakContext.get();
            if (context != null) {
                mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);    //2016.12.29 添加动作
                context.registerReceiver(mUsbReceiver, filter);
            }
            mDeviceCounts = 0;
            //            mHandler.postDelayed(mDeviceCheckRunnable, 1000);
            //mHandler.postDelayed(mDeviceCheckRunnable, 500);
        }
    }

    /**
     * unregister BroadcastReceiver     //注销BroadcastReceiver
     */
    public synchronized void unregister() {
        if (mPermissionIntent != null) {
            if (DEBUG)
                Log.i(TAG, "unregister:");
            final Context context = mWeakContext.get();
            if (context != null) {
                try {
                    context.unregisterReceiver(mUsbReceiver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mPermissionIntent = null;
        }
        mDeviceCounts = 0;
        mHandler.removeCallbacks(mDeviceCheckRunnable);
    }

    public synchronized boolean isRegistered() {
        return mPermissionIntent != null;
    }

    public void setCurrentDevice(UsbDevice currentDevice) {
        this.currentDevice = currentDevice;
    }

    public UsbDevice getCurrentDevice() {
        return currentDevice;
    }

    /**
     * set device filter    //设置设备过滤器
     *
     * @param filter
     */
    public void setDeviceFilter(final DeviceFilter filter) {
        mDeviceFilters.clear();
        mDeviceFilters.add(filter);
    }

    /**
     * set device filters   //set device filters
     *
     * @param filters
     */
    public void setDeviceFilter(final List<DeviceFilter> filters) {
        mDeviceFilters.clear();
        mDeviceFilters.addAll(filters);
    }

    /**
     * return the number of connected USB devices that matched device filter
     * 返回连接的USB设备，配套设备筛选器的数目
     *
     * @return
     */
    public int getDeviceCount() {
        return getDeviceList().size();
    }

    /**
     * return device list, return empty list if no device matched
     * 返回设备列表，返回空列表，如果没有设备匹配
     *
     * @return
     */
    public List<UsbDevice> getDeviceList() {
        Log.v(TAG, "mList<UsbDevice> getDeviceList()  ");
        return getDeviceList(mDeviceFilters);
    }

    /**
     * return device list, return empty list if no device matched
     * 返回设备列表，返回空列表，如果没有设备匹配
     *
     * @param filters
     * @return
     */
    public List<UsbDevice> getDeviceList(final List<DeviceFilter> filters) {
        //驱动列表是通过Android的USBManager获取得到的 :
        final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        final List<UsbDevice> result = new ArrayList<UsbDevice>();
        if (deviceList != null) {
            for (final DeviceFilter filter : filters) {
                final Iterator<UsbDevice> iterator = deviceList.values().iterator();
                UsbDevice device;
                while (iterator.hasNext()) {
                    device = iterator.next();
                    if ((filter == null) || (filter.matches(device))) {
                        result.add(device);
                    }
                }
            }
        }

        Log.v(TAG, "mList<DeviceFilter> # getDeviceList  " + result.toString());
        return result;
    }

    /**
     * return device list, return empty list if no device matched
     * 返回设备列表，返回空列表，如果没有设备匹配
     *
     * @param filter
     * @return
     */
    public List<UsbDevice> getDeviceList(final DeviceFilter filter) {
        //驱动列表是通过Android的USBManager获取得到的 :
        final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        final List<UsbDevice> result = new ArrayList<UsbDevice>();
        if (deviceList != null) {
            final Iterator<UsbDevice> iterator = deviceList.values().iterator();
            UsbDevice device;
            while (iterator.hasNext()) {
                device = iterator.next();
                if ((filter == null) || (filter.matches(device))) {
                    result.add(device);
                }
            }
        }
        Log.v(TAG, " getDeviceList  " + result.toString());
        return result;
    }

    /**
     * get USB device list  //获取USB设备列表
     *
     * @return
     */
    public Iterator<UsbDevice> getDevices() {
        Iterator<UsbDevice> iterator = null;
        //驱动列表是通过Android的USBManager获取得到的 :
        final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
        if (list != null)
            iterator = list.values().iterator();
        return iterator;
    }

    /**
     * output device list to LogCat
     * 输出设备列表logcat
     */
    public final void dumpDevices() {
        //驱动列表是通过Android的USBManager获取得到的 :
        final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
        if (list != null) {
            final Set<String> keys = list.keySet();
            if (keys != null && keys.size() > 0) {
                final StringBuilder sb = new StringBuilder();
                for (final String key : keys) {
                    final UsbDevice device = list.get(key);
                    final int num_interface = device != null ? device.getInterfaceCount() : 0;
                    sb.setLength(0);
                    for (int i = 0; i < num_interface; i++) {
                        sb.append(String.format("interface%d:%s", i, device.getInterface(i).toString()));
                    }
                    Log.i(TAG, "key=" + key + ":" + device + ":" + sb.toString());
                }
            } else {
                Log.i(TAG, "no device");
            }
        } else {
            Log.i(TAG, "no device");
        }
    }

    /**
     * return whether the specific Usb device has permission
     * 返回是否特定的USB设备的权限
     *
     * @param device
     * @return
     */
    public boolean hasPermission(final UsbDevice device) {
        return mUsbManager.hasPermission(device);
    }

    /**
     * request permission to access to USB device
     * 请求允许访问USB设备
     *
     * @param device
     */
    public synchronized void requestPermission(final UsbDevice device) {
        if (DEBUG)
            Log.v(TAG, "requestPermission:device=" + device);
        if (mPermissionIntent != null) {
            if (device != null) {
                if (mUsbManager.hasPermission(device)) {
                    Log.v(TAG, "processConnect:=" + device);
                    processConnect(device);
                } else {
                    mUsbManager.requestPermission(device, mPermissionIntent);   //请求允许访问USB设备
                }
            } else {
                processCancel(device);
            }
        } else {
            processCancel(device);
        }
    }

    /**
     * BroadcastReceiver for USB permission
     * USB权限BroadcastReceiver监听器
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            final String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {

                synchronized (USBMonitor.this) {

                    final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {   //EXTRA_PERMISSION_GRANTED额外的许可权限
                        if (device != null) {
                            processConnect(device); //过程连接
                        }
                    } else {
                        processCancel(device);
                    }
                }
                Log.e(TAG, "USBMonitor#mUsbReceiver#onReceive() ACTION_USB_PERMISSION = ");

            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                //USB第一次链接 ;
                final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getProductId() == 60000 || device.getProductId() == 248 || device.getProductId() == 239) {
                        FileUtils.writeFileToLogFolder("USB第一次链接-串口异常productid=" + device.getProductId());
                        Hawk.put("port_exception", true);
                        RestartUtil restartUtil = new RestartUtil();
                        restartUtil.killProcess();
                    }
                }
                Log.e(TAG, "USBMonitor#mUsbReceiver#onReceive() ACTION_USB_DEVICE_ATTACHED == ");
                FileUtils.writeFileToLogFolder("USB第一次链接");

                processAttach(device);

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {      //USB断开链接 ;
                 //  name  = 013 014
                // id 4023 4024
                //class 0  239
                //productid 60000 248
                FileUtils.writeFileToLogFolder("USB断开链接");
                final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getProductId() == 248 || device.getProductId() == 60000 || device.getProductId() == 239) {
                        FileUtils.writeFileToLogFolder("USB断开链接productid=" + device.getProductId());
                        Hawk.put("port_exception", true);
                        FileUtils.writeFileToLogFolder("USB断开链接-串口异常 ddeviceclass="+device.getProductId());
                        RestartUtil restartUtil = new RestartUtil();
                        restartUtil.killProcess();
                    }
                }

                if (device != null) {
                    if (device.getDeviceClass() != 239 || device.getDeviceSubclass() != 2) {
                        Log.w(TAG, " === not USB Camera  === ");
                        return;
                    }
                    UsbControlBlock ctrlBlock = null;
                    ctrlBlock = mCtrlBlocks.remove(device);
                    if (ctrlBlock != null) {
                        ctrlBlock.close();
                    }
                    mDeviceCounts = 0;
                    processDettach(device);
                    //                    Log.e("test", "USBMonitor#mUsbReceiver#onReceive() ACTION_USB_DEVICE_DETACHED === 1");
                }
                Log.e(TAG, "USBMonitor#mUsbReceiver#onReceive() ACTION_USB_DEVICE_DETACHED === ");
            }
        }
    };

    private volatile int mDeviceCounts = 0;

    private final Runnable mDeviceCheckRunnable = new Runnable() {
        @Override
        public void run() {
            final int n = getDeviceCount();
            Log.w(TAG,"getDeviceCount="+n);
            if (n != mDeviceCounts) {
                if (n > mDeviceCounts) {
                    mDeviceCounts = n;
                    if (mOnDeviceConnectListener != null)
                        mOnDeviceConnectListener.onAttach(null);
                }
            }
            //  mHandler.postDelayed(this, 2000);    // confirm every 2 seconds     //每2秒确认一次 ;
            mHandler.postDelayed(this, 2000);    // confirm every 2 seconds     //每2秒确认一次 ;

        }
    };

    private final void processConnect(final UsbDevice device) {     //过程连接
        if (DEBUG)
            Log.v(TAG, "processConnect:");
        mUsbManager.openDevice(device);
        //not a uvc camera
        //增加判断 连接的设备是否是 uvc camera : 当前设备列表里只有一个设备ID :
        if (device.getDeviceClass() != 239 || device.getDeviceSubclass() != 2) {
            Log.w(TAG, " === USB Camera find error === ");
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                UsbControlBlock ctrlBlock;
                final boolean createNew;
                ctrlBlock = mCtrlBlocks.get(device);
                if (ctrlBlock == null) {
                    ctrlBlock = new UsbControlBlock(USBMonitor.this, device);
                    mCtrlBlocks.put(device, ctrlBlock);
                    createNew = true;
                } else {
                    createNew = false;
                }
                if (mOnDeviceConnectListener != null) {
                    final UsbControlBlock ctrlB = ctrlBlock;
                    mOnDeviceConnectListener.onConnect(device, ctrlB, createNew);
                }
            }
        });
    }

    private final void processCancel(final UsbDevice device) {
        if (DEBUG)
            Log.v(TAG, "processCancel:");
        //not a uvc camera
        //增加判断 连接的设备是否是 uvc camera : 当前设备列表里只有一个设备ID :
        if (device.getDeviceClass() != 239 || device.getDeviceSubclass() != 2) {
            Log.w(TAG, " === USB Camera find error === ");
            return;
        }

        if (mOnDeviceConnectListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnDeviceConnectListener.onCancel();
                }
            });
        }
    }

    private final void processAttach(final UsbDevice device) {
        if (DEBUG)
            Log.v(TAG, "processAttach:");
        //not a uvc camera
        //增加判断 连接的设备是否是 uvc camera : 当前设备列表里只有一个设备ID :
        if (device.getDeviceClass() != 239 || device.getDeviceSubclass() != 2) {
            Log.w(TAG, "processAttach() === USB Camera find error === ");
            return;
        }

        if (mOnDeviceConnectListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnDeviceConnectListener.onAttach(device);
                }
            });
        }
    }

    private final void processDettach(final UsbDevice device) {
        if (DEBUG)
            Log.v(TAG, "processDettach:");
        Log.w("test_USBMonitor", "processDettach:");
        //not a uvc camera
        //增加判断 连接的设备是否是 uvc camera : 当前设备列表里只有一个设备ID :
        if (device.getDeviceClass() != 239 || device.getDeviceSubclass() != 2) {
            Log.w(TAG, " === USB Camera find error === ");
            return;
        }

        if (mOnDeviceConnectListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnDeviceConnectListener.onDettach(device);
                }
            });
        }
    }

    /**
     * UsbControlBlock :
     * USB 控制模块 :
     */
    public static final class UsbControlBlock {
        private final SoftReference<USBMonitor> mWeakMonitor;
        private final SoftReference<UsbDevice> mWeakDevice;
        protected UsbDeviceConnection mConnection;
        private final SparseArray<UsbInterface> mInterfaces = new SparseArray<UsbInterface>();

        /**
         * this class needs permission to access USB device before constructing
         * 这类需要权限访问USB设备施工前
         *
         * @param monitor
         * @param device
         */
        public UsbControlBlock(final USBMonitor monitor, final UsbDevice device) {
            if (DEBUG)
                Log.i(TAG, "UsbControlBlock:constructor");
            mWeakMonitor = new SoftReference<USBMonitor>(monitor);
            mWeakDevice = new SoftReference<UsbDevice>(device);

            mConnection = monitor.mUsbManager.openDevice(device);   //关键代码 , 调用UsbManager的方法打开usb驱动链接 ;

            final String name = device.getDeviceName();
            if (mConnection != null) {
                if (DEBUG) {
                    final int desc = mConnection.getFileDescriptor();
                    final byte[] rawDesc = mConnection.getRawDescriptors();
                    Log.i(TAG, "UsbControlBlock:name=" + name + ", desc=" + desc + ", rawDesc=" + rawDesc);
                }
            } else {
                Log.e(TAG, "could not connect to device " + name);
            }
        }

        public UsbDevice getDevice() {
            return mWeakDevice.get();
        }

        public String getDeviceName() {
            final UsbDevice device = mWeakDevice.get();
            return device != null ? device.getDeviceName() : "";
        }

        public UsbDeviceConnection getUsbDeviceConnection() {
            return mConnection;
        }

        public synchronized int getFileDescriptor() {
            return mConnection != null ? mConnection.getFileDescriptor() : -1;
        }

        public byte[] getRawDescriptors() {
            return mConnection != null ? mConnection.getRawDescriptors() : null;
        }

        public int getVenderId() {
            final UsbDevice device = mWeakDevice.get();
            return device != null ? device.getVendorId() : 0;
        }

        public int getProductId() {
            final UsbDevice device = mWeakDevice.get();
            return device != null ? device.getProductId() : 0;
        }

        public synchronized String getSerial() {
            return mConnection != null ? mConnection.getSerial() : null;
        }

        /**
         * open specific interface
         * 打开特定的接口
         *
         * @param interfaceIndex
         * @return
         */
        public synchronized UsbInterface open(final int interfaceIndex) {
            if (DEBUG)
                Log.i(TAG, "UsbControlBlock#open:" + interfaceIndex);
            final UsbDevice device = mWeakDevice.get();
            UsbInterface intf = null;
            intf = mInterfaces.get(interfaceIndex);
            if (intf == null) {
                intf = device.getInterface(interfaceIndex);
                if (intf != null) {
                    synchronized (mInterfaces) {
                        mInterfaces.append(interfaceIndex, intf);
                    }
                }
            }
            return intf;
        }

        /**
         * close specified interface. USB device itself still keep open.
         * 关闭指定的接口。USB设备本身仍然保持开放。
         *
         * @param interfaceIndex
         */
        public void close(final int interfaceIndex) {
            UsbInterface intf = null;
            synchronized (mInterfaces) {
                intf = mInterfaces.get(interfaceIndex);
                if (intf != null) {
                    mInterfaces.delete(interfaceIndex);
                    mConnection.releaseInterface(intf);
                }
            }
        }

        /**
         * close specified interface. USB device itself still keep open.
         * 关闭指定的接口。USB设备本身仍然保持开放。
         */
        public synchronized void close() {
            if (DEBUG)
                Log.i(TAG, "UsbControlBlock#close:");

            if (mConnection != null) {
                final int n = mInterfaces.size();
                int key;
                UsbInterface intf;
                for (int i = 0; i < n; i++) {
                    key = mInterfaces.keyAt(i);
                    intf = mInterfaces.get(key);
                    mConnection.releaseInterface(intf);
                }
                mConnection.close();
                mConnection = null;
                final USBMonitor monitor = mWeakMonitor.get();
                if (monitor != null) {
                    if (monitor.mOnDeviceConnectListener != null) {
                        final UsbDevice device = mWeakDevice.get();
                        monitor.mOnDeviceConnectListener.onDisconnect(device, this);
                    }
                    monitor.mCtrlBlocks.remove(getDevice());
                }
            }
        }

        /**
         * 类的Finalize方法，可以告诉垃圾回收器应该执行的操作，该方法从Object类继承而来。
         *
         * @throws Throwable
         */
        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }
    }

}
