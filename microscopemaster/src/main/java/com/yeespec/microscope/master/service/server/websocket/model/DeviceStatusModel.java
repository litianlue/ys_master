package com.yeespec.microscope.master.service.server.websocket.model;

/**
 * Created by Beary on 15/12/11.
 */
public class DeviceStatusModel {
    //TODO 获取电池、内存、wifi连接、相机连接、连接当前手机数、IP、MAC、关机
    private String battery;
    // 占用内存
    private String employMemory;
    // 总内存
    private String totalMemory;
    private String wifi;
    private String cameraConnect;   //相机给手机连接的状态 ;
    private String connectCount;
    private String deviceIP;
    private String deviceMac;

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public String getEmployMemoery() {
        return employMemory;
    }

    public void setEmployMemory(String employMemory) {
        this.employMemory = employMemory;
    }

    public String getTotalMemoery() {
        return totalMemory;
    }

    public void setTotalMemory(String totalMemory) {
        this.totalMemory = totalMemory;
    }

    public String getWifi() {
        return wifi;
    }

    public void setWifi(String wifi) {
        this.wifi = wifi;
    }

    public String getCameraConnect() {
        return cameraConnect;
    }

    public void setCameraConnect(String cameraConnect) {
        this.cameraConnect = cameraConnect;
    }

    public String getConnectCount() {
        return connectCount;
    }

    public void setConnectCount(String connectCount) {
        this.connectCount = connectCount;
    }

    public String getDeviceIP() {
        return deviceIP;
    }

    public void setDeviceIP(String deviceIP) {
        this.deviceIP = deviceIP;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }
}
