package com.yeespec.microscope.master.service.server.http;

import com.koushikdutta.async.Util;
import com.yeespec.microscope.master.service.server.http.api.HttpServerService;
import com.koushikdutta.async.http.server.AsyncHttpServer;

/**
 * Created by virgilyan on 15/10/12.
 */
public class HttpServer {
    public static final int PORT = 5000;
    private static AsyncHttpServer server;
    private static HttpServer httpServer;

    private HttpServer(int PORT) {

        Util.SUPRESS_DEBUG_EXCEPTIONS = true;

        server = new AsyncHttpServer();     //创建一个http  server
        server.listen(PORT);
        new HttpServerService(server);
    }

    public synchronized static HttpServer getInstance(int PORT) {
        if (server == null) {
            httpServer = new HttpServer(PORT);
        }
        return httpServer;
    }
}
