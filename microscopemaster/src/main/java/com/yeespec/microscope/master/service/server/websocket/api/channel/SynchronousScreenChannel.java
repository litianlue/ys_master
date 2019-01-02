package com.yeespec.microscope.master.service.server.websocket.api.channel;

import com.yeespec.microscope.master.service.server.websocket.ServerChannel;

/**
 * Created by virgilyan on 15/11/19.
 */
public class SynchronousScreenChannel extends ServerChannel {

    public static final String CHANNEL_NAME = "/picture";

    public SynchronousScreenChannel() {
        super(CHANNEL_NAME);
        TAG = SynchronousScreenChannel.class.getSimpleName();
    }

}
