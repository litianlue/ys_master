package com.yeespec.microscope.master.service.server.websocket.api.channel;

import com.yeespec.microscope.master.service.server.websocket.ServerChannel;

/**
 * Created by Beary on 15/12/11.
 */
public class DeviceStatusChannel extends ServerChannel {

    public static final String CHANNEL_NAME = "/pad/status";

    public DeviceStatusChannel() {
        super(CHANNEL_NAME);
        TAG = DeviceStatusChannel.class.getSimpleName();
    }

}
