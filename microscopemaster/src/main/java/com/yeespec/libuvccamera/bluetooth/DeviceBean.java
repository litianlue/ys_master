package com.yeespec.libuvccamera.bluetooth;

public class DeviceBean {
	public String message;
	public boolean isReceive;

	public DeviceBean(String msg, boolean isReceive) {
		this.message = msg;
		this.isReceive = isReceive;
	}
}