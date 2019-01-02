package com.yeespec.microscope.master.service.server.websocket;

import android.util.Log;

import com.yeespec.microscope.utils.log.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by virgilyan on 15/10/13.
 */
public class ServerChannel {

    protected static final boolean DEBUG = false;
    public String TAG = ServerChannel.class.getSimpleName();

    protected String path;

    protected final Collection<WebSocket> connections = new HashSet<>();

    public int getConnectionsCount() {
        return connections.size();
    }

    public ServerChannel(String path) {
        this.path = path;
    }

    protected void addConnection(WebSocket webSocket) {
        synchronized (connections) {
            if (webSocket != null)
                connections.add(webSocket);
        }
    }

    protected boolean disconnect(WebSocket webSocket) {
        synchronized (connections) {
            return connections.remove(webSocket);
        }
    }

  /*  public void disconnectAll() {
        synchronized (connections) {
            if (connections.size() > 0) {
                for (WebSocket conn : connections) {
                    conn.close();
                    connections.remove(conn);
                }
            }
        }
    }
*/
    public void broadcast(String msg) {         //改方法用于发送JSON格式的String数据 ;
        synchronized (connections) {
            if (connections.size() > 0) {
                for (WebSocket conn : connections) {
                    try {
                        if(!conn.hasBufferedData())
                        conn.send(msg);
                    } catch (WebsocketNotConnectedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void broadcast(byte[] sendFrame) {        //改方法用于发送byte数组格式的图像或音频数据 ;
        synchronized (connections) {
            if (connections.size() > 0) {
                for (WebSocket conn : connections) {
                    if(!conn.hasBufferedData())
                    conn.send(sendFrame);
                }
            }
        }
    }

    public void sendLocal(String msg) {
        synchronized (connections) {
            if (connections.size() > 0) {
                for (WebSocket conn : connections) {
                    try {
                        if(!conn.hasBufferedData())
                        conn.send(msg);
                    } catch (WebsocketNotConnectedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String path() {
        return path;
    }


    public void onAttachedToServer() {

    }

    public void onDetachedFromServer() {

    }

    //================2016.08.10 修改 : 将抽象方法改为实体方法 :================

    /**
     * Socket接受客户端连接
     *
     * @param conn
     * @param handshake
     */
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (DEBUG)
            Logger.e(TAG, conn.getRemoteSocketAddress().getHostName() + ":onOpen");
        boolean isadd =true;
        synchronized (connections) {

            Iterator it  = connections.iterator();
            while (it.hasNext()){
                WebSocket ws = (WebSocket) it.next();

                if(ws.getRemoteSocketAddress().getHostName().equals(conn.getRemoteSocketAddress().getHostName())){
                    isadd = false;
                    break;
                }
            }

        }
        if(isadd) {
            addConnection(conn);
        }

    }

    /**
     * 关闭Socket与客户端连接
     *ar
     * @param conn
     * @param code
     * @param reason
     * @param remote
     */
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (conn != null) {
            if (conn.getRemoteSocketAddress() != null)
                if (DEBUG)
                    Logger.e(TAG, conn.getRemoteSocketAddress().getHostName() + ":onClose(" + code + "," + reason + ")");
            disconnect(conn);
        }
    }

    public void onMessage(WebSocket conn, String message) {
        if (DEBUG)
            Logger.e(TAG, conn.getRemoteSocketAddress().getHostName() + " send : " + message);
    }

    public void onError(WebSocket conn, Exception ex) {
        if (ex != null)
            ex.printStackTrace();
        conn.close();
        disconnect(conn);
    }
}
