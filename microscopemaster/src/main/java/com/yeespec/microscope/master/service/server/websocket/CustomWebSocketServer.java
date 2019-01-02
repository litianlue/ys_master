package com.yeespec.microscope.master.service.server.websocket;

import com.yeespec.microscope.master.application.BaseApplication;
import com.yeespec.microscope.master.service.server.websocket.api.channel.CameraOperationChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.CameraOptionsChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.ClientNumberChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.DeviceOperationControlChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.DeviceStatusChannel;
import com.yeespec.microscope.master.service.server.websocket.api.channel.SynchronousScreenChannel;
import com.yeespec.microscope.utils.SettingUtils;
import com.yeespec.microscope.utils.log.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by virgilyan on 15/10/12.
 */
public class CustomWebSocketServer extends WebSocketServer {

    protected static final boolean DEBUG = false;
    private static final String TAG = CustomWebSocketServer.class.getSimpleName();

    public static final String PROTOCOL = "ws://";
    //    public static final String IP = SettingUtils.getIPAddress(BaseApplication.getContext());
    public static final String IP = "127.0.0.1";
    public static final int PORT1 = 8121;
    public static final int PORT2 = 8122;
    public static final int PORT3 = 8123;
    public static final int PORT4 = 8124;
    public static final int PORT5 = 8125;
    public static final int PORT6 = 8126;
    public static final String URI1 = IP + ":" + PORT1;
    public static final String URI2 = IP + ":" + PORT2;
    public static final String URI3 = IP + ":" + PORT3;
    public static final String URI4 = IP + ":" + PORT4;
    public static final String URI5 = IP + ":" + PORT5;
    public static final String URI6 = IP + ":" + PORT6;
    // 路径与通道事件的对应
    public static final HashMap<String, ServerChannel> CHANNELS = new HashMap<>();
    // Socket管理与通道事件的对应
    private static final HashMap<WebSocket, ServerChannel> parentChannel = new HashMap<>();

    private static CustomWebSocketServer webSocketServer;

    private CustomWebSocketServer(int port) throws UnknownHostException {
        this(new InetSocketAddress(port));
    }

    private CustomWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    public synchronized static CustomWebSocketServer getInstance(int port) {
        WebSocketImpl.DEBUG = false;
        try {
            webSocketServer = new CustomWebSocketServer(port);
            webSocketServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return webSocketServer;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        synchronized (parentChannel) {
            String path = handshake.getResourceDescriptor();
            if (conn.getRemoteSocketAddress() != null)
                if (DEBUG)
                    Logger.e(conn.getRemoteSocketAddress().getHostName() + " connected to " + path);
            ServerChannel channel = CHANNELS.get(path);
            if (channel == null) {
                if (handler != null) {
                    channel = handler.channelForPath(path);
                }
                if (channel != null) {
                    attachChannel(path, channel);
                } else {
                    conn.close();
                    return;
                }
            }
            channel.onOpen(conn, handshake);
            parentChannel.put(conn, channel);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        synchronized (parentChannel) {
            ServerChannel channel = parentChannel.get(conn);
            if (channel == null) {
                return;
            } else {
                channel.onClose(conn, code, reason, remote);
                parentChannel.remove(conn);
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        synchronized (parentChannel) {
            ServerChannel channel = parentChannel.get(conn);
            if (channel == null) {
                return;
            } else {
                channel.onMessage(conn, message);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex != null)
            ex.printStackTrace();
        if (conn != null) {
            synchronized (parentChannel) {
                // some errors like port binding failed may not be assignable to a specific websocket
                ServerChannel channel = parentChannel.get(conn);
                connections().remove(conn);
                if (channel == null) {
                    return;
                } else {
                    channel.onError(conn, ex);
                }
            }
        }
    }

    private ChannelHandler handler = new ChannelHandler() {
        @Override
        public ServerChannel channelForPath(String path) {
            // TODO 创建一个新Channel
            switch (path) {
                case SynchronousScreenChannel.CHANNEL_NAME:
                    return new SynchronousScreenChannel();
                case CameraOptionsChannel.CHANNEL_NAME:
                    return new CameraOptionsChannel();
                case DeviceStatusChannel.CHANNEL_NAME:
                    return new DeviceStatusChannel();
                case CameraOperationChannel.CHANNEL_NAME:
                    return new CameraOperationChannel();
                case DeviceOperationControlChannel.CHANNEL_NAME:
                    return new DeviceOperationControlChannel();
                case ClientNumberChannel.CHANNEL_NAME:
                    return new ClientNumberChannel();
                default:
                    break;
            }
            return null;
        }
    };

    /**
     * 增加通道
     *
     * @param channel
     */
    public void attachChannel(String path, ServerChannel channel) {
        if (CHANNELS.containsKey(path)) {
            if (DEBUG)
                Logger.e("Cannot attach second channel on path: " + path);
            return;
        }
        if (DEBUG)
            Logger.v("attaching channel for path: " + path);
        CHANNELS.put(path, channel);
        channel.onAttachedToServer();
    }

    /**
     * 删除通道
     *
     * @param channel
     */
    public void detachChannel(ServerChannel channel) {
        String path = channel.path();
        if (CHANNELS.containsKey(path)) {
            throw new IllegalStateException("Channel not found for path: " + path);
        }
        if (DEBUG)
            Logger.e("detaching channel for path: " + path);
        CHANNELS.remove(path);
        channel.onDetachedFromServer();
    }

    @Override
    public void start() {

        super.start();
    }

    public int getConnectionCount() {
        return connections().size();
    }

    public int getOtherConnectionCount() {
        int count = 0;
        for (WebSocket webSocket : connections()) {
            String host = webSocket.getRemoteSocketAddress().getHostName();
            if (host.contains(SettingUtils.getIPAddress(BaseApplication.getContext())) || host.contains("127.0.0.1"))
                count++;
        }
        return count;
    }


    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }

    public interface ChannelHandler {
        ServerChannel channelForPath(String path);
    }
}
