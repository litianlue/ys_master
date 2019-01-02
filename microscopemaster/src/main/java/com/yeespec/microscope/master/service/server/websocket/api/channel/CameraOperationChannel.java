package com.yeespec.microscope.master.service.server.websocket.api.channel;

import com.yeespec.microscope.master.service.server.websocket.ServerChannel;

/**
 * Created by virgilyan on 15/10/13.
 */
public class CameraOperationChannel extends ServerChannel {

    public static final String CHANNEL_NAME = "/camera/operation";

    public CameraOperationChannel() {
        super(CHANNEL_NAME);
        TAG = CameraOperationChannel.class.getSimpleName();
    }

}
