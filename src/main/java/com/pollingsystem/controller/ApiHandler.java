package com.pollingsystem.controller;

import com.pollingsystem.dto.ErrorResponse;
import com.pollingsystem.exception.ApiException;
import com.pollingsystem.exception.MethodNotAllowedException;
import com.pollingsystem.exception.NotFoundException;
import com.pollingsystem.exception.PayloadTooLargeException;
import com.pollingsystem.exception.ValidationException;
import com.pollingsystem.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes /api/** requests, enforces method and body-size rules, and converts every failure to the
 * single JSON error format. Stack traces are logged on the server and never sent to the browser.
 */
public class ApiHandler implements HttpHandler {
    public static final int MAX_BODY_BYTES = 16 * 1024;
    private static final Logger LOG = Logger.getLogger(ApiHandler.class.getName());

    private final PollController polls;
    private final VoteController votes;
    private final HealthController health;
    private final Json json;

    public ApiHandler(PollController polls, VoteController votes, HealthController health, Json json) {
        this.polls = polls;
        this.votes = votes;
        this.health = health;
        this.json = json;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ApiResponse response = dispatch(exchange);
            send(exchange, response.status(), json.write(response.body()), null);
        } catch (ApiException e) {
            String allow = e instanceof MethodNotAllowedException m ? m.allow() : null;
            List<String> details = e instanceof ValidationException v ? v.errors() : null;
            if (e.status() >= 500) LOG.log(Level.WARNING, "API error " + e.code(), e);
            send(exchange, e.status(), json.write(new ErrorResponse(e.code(), e.getMessage(), details)), allow);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error while handling " + exchange.getRequestMethod() + " request", e);
            send(exchange, 500, json.write(new ErrorResponse("INTERNAL_ERROR",
                    "Something went wrong on the server. Please try again.", null)), null);
        } finally {
            exchange.close();
        }
    }

    private ApiResponse dispatch(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        List<String> seg = segments(exchange.getRequestURI().getPath());

        if (seg.size() == 1 && seg.get(0).equals("health")) {
            requireMethod(method, "GET");
            return health.health();
        }
        if (seg.size() == 1 && seg.get(0).equals("stats")) {
            requireMethod(method, "GET");
            return polls.stats();
        }
        if (!seg.isEmpty() && seg.get(0).equals("polls")) {
            switch (seg.size()) {
                case 1 -> {
                    if (method.equals("GET")) return polls.list();
                    if (method.equals("POST")) return polls.create(readBody(exchange));
                    throw new MethodNotAllowedException("GET, POST");
                }
                case 2 -> {
                    requireMethod(method, "GET");
                    return polls.get(seg.get(1));
                }
                case 3 -> {
                    switch (seg.get(2)) {
                        case "votes" -> {
                            requireMethod(method, "POST");
                            return votes.castVote(seg.get(1), readBody(exchange));
                        }
                        case "results" -> {
                            requireMethod(method, "GET");
                            return votes.results(seg.get(1));
                        }
                        case "close" -> {
                            requireMethod(method, "POST");
                            return polls.close(seg.get(1));
                        }
                        default -> { }
                    }
                }
                default -> { }
            }
        }
        throw new NotFoundException("Endpoint not found.");
    }

    private static void requireMethod(String actual, String allowed) {
        if (!actual.equals(allowed)) throw new MethodNotAllowedException(allowed);
    }

    private static List<String> segments(String path) {
        String rest = path.startsWith("/api") ? path.substring(4) : path;
        List<String> out = new ArrayList<>();
        for (String s : rest.split("/")) {
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static byte[] readBody(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) throw new PayloadTooLargeException(MAX_BODY_BYTES);
        return body;
    }

    private static void send(HttpExchange ex, int status, byte[] body, String allow) throws IOException {
        var h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Cache-Control", "no-store");
        h.set("X-Content-Type-Options", "nosniff");
        if (allow != null) h.set("Allow", allow);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
