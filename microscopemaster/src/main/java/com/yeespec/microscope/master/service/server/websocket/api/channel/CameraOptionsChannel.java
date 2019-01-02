package com.yeespec.microscope.master.service.server.websocket.api.channel;


import com.yeespec.microscope.master.service.server.websocket.ServerChannel;
import com.yeespec.microscope.utils.log.Logger;
import com.yeespec.libuvccamera.uvccamera.service.UVCService;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

/**
 * Created by virgilyan on 15/11/20.
 */
public class CameraOptionsChannel extends ServerChannel {

    public static final String CHANNEL_NAME = "/camera/options";

    public CameraOptionsChannel() {
        super(CHANNEL_NAME);
        TAG = CameraOptionsChannel.class.getSimpleName();
    }

    @Override
    public void broadcast(String msg) {
        synchronized (connections) {
            if (connections.size() > 0) {
                for (WebSocket conn : connections) {
                    String host = conn.getLocalSocketAddress().getHostName();
                    if (!host.contains("localhost") && !host.contains("127.0.0.1")) {   //相机向外无差别发送状态 ,所以需要屏蔽本机IP地址 ;
                        try {
                            conn.send(msg);
                        } catch (WebsocketNotConnectedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMessage(final WebSocket conn, String message) {
        if (conn != null) {
            if (conn.getRemoteSocketAddress() != null)
                if (DEBUG)
                    Logger.e(TAG, conn.getRemoteSocketAddress().getHostName() + " send : " + message);
            UVCService.WEB_SOCKET_PICTURE_SERVER.CHANNELS.get(CameraOperationChannel.CHANNEL_NAME).sendLocal(message);
        }
    }

}
