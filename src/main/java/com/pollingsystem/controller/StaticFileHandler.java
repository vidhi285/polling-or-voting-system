package com.pollingsystem.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serves the frontend from the classpath folder /static. Only files with a known extension are
 * served, so directory listings and path traversal are not possible.
 */
public class StaticFileHandler implements HttpHandler {
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "ico", "image/x-icon",
            "txt", "text/plain; charset=utf-8");

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod().toUpperCase();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                ex.getResponseHeaders().set("Allow", "GET, HEAD");
                plain(ex, 405, "Method not allowed");
                return;
            }
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            if (!isSafe(path)) {
                plain(ex, 404, "Not found");
                return;
            }
            String type = TYPES.get(extension(path));
            byte[] content = null;
            if (type != null) {
                try (InputStream in = StaticFileHandler.class.getResourceAsStream("/static" + path)) {
                    if (in != null) content = in.readAllBytes();
                }
            }
            if (content == null) {
                plain(ex, 404, "Not found");
                return;
            }
            var h = ex.getResponseHeaders();
            h.set("Content-Type", type);
            h.set("Cache-Control", "no-cache");
            h.set("X-Content-Type-Options", "nosniff");
            if (method.equals("HEAD")) {
                h.set("Content-Length", String.valueOf(content.length));
                ex.sendResponseHeaders(200, -1);
            } else {
                ex.sendResponseHeaders(200, content.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(content);
                }
            }
        } finally {
            ex.close();
        }
    }

    static boolean isSafe(String path) {
        if (!path.startsWith("/") || path.contains("\\") || path.contains("\0") || path.contains("//")) return false;
        for (String part : path.split("/")) {
            if (part.equals("..") || part.equals(".")) return false;
        }
        return true;
    }

    private static String extension(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return dot > slash ? path.substring(dot + 1).toLowerCase() : "";
    }

    private static void plain(HttpExchange ex, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
