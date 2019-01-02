package com.yeespec.microscope.master.service.server.websocket.api.channel;

import com.yeespec.microscope.master.service.server.websocket.ServerChannel;

/**
 * Created by virgilyan on 15/11/19.
 */
public class ClientNumberChannel extends ServerChannel {

    public static final String CHANNEL_NAME = "/client/number";

    public ClientNumberChannel() {
        super(CHANNEL_NAME);
        TAG = ClientNumberChannel.class.getSimpleName();
    }

}
