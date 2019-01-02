package com.yeespec.microscope.master.service.server.websocket.api.channel;

import com.yeespec.microscope.master.service.server.websocket.ServerChannel;

/**
 * Created by virgilyan on 15/10/13.
 */
public class DeviceOperationControlChannel extends ServerChannel {

    public static final String CHANNEL_NAME = "/device/operation/status";

    public DeviceOperationControlChannel() {
        super(CHANNEL_NAME);
        TAG = DeviceOperationControlChannel.class.getSimpleName();
    }

}
