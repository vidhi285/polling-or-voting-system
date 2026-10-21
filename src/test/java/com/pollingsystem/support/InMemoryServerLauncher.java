package com.pollingsystem.support;

/**
 * TEST HARNESS ONLY - never used by the product. Starts the real HTTP stack on in-memory fakes so the
 * Postman/Newman collection can be exercised in environments without MongoDB (e.g. this build sandbox).
 * The real application never falls back to in-memory storage.
 */
public final class InMemoryServerLauncher {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        TestServer server = TestServer.inMemory(port);
        System.out.println("Harness listening on " + server.baseUrl());
        Thread.currentThread().join();
    }
}
