package com.pollingsystem;

import com.pollingsystem.controller.ApiHandler;
import com.pollingsystem.controller.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Wraps the JDK HttpServer: /api/** goes to the API, everything else to the static frontend. */
public final class WebServer {
    private final HttpServer server;
    private final ExecutorService executor;

    public WebServer(InetSocketAddress address, ApiHandler api, StaticFileHandler files) throws IOException {
        this.server = HttpServer.create(address, 0);
        this.executor = Executors.newFixedThreadPool(16);
        server.createContext("/api", api);
        server.createContext("/", files);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(1);
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
