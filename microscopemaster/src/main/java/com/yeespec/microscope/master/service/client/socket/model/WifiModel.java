package com.yeespec.microscope.master.service.client.socket.model;

/**
 * Created by Beary on 15/12/10.
 */
public class WifiModel {

    private String wifiName;
    private String securityMode;
    private int level;

    public String getWifiName() {
        return wifiName;
    }

    public void setWifiName(String wifiName) {
        this.wifiName = wifiName;
    }

    public String getSecurityMode() {
        return securityMode;
    }

    public void setSecurityMode(String securityMode) {
        this.securityMode = securityMode;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
