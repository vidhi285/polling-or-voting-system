package com.pollingsystem.config;

import java.util.Map;

/**
 * Runtime configuration. Local defaults need no .env file; every value can be overridden
 * with the environment variables MONGODB_URI, MONGODB_DATABASE, SERVER_PORT and SERVER_HOST.
 */
public record AppConfig(String mongoUri, String databaseName, int port, String host) {

    public static final String DEFAULT_MONGO_URI = "mongodb://127.0.0.1:27017";
    public static final String DEFAULT_DATABASE = "polling_voting_db";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_HOST = "127.0.0.1";

    public static AppConfig fromEnv(Map<String, String> env) {
        String uri = valueOrDefault(env, "MONGODB_URI", DEFAULT_MONGO_URI);
        String db = valueOrDefault(env, "MONGODB_DATABASE", DEFAULT_DATABASE);
        String host = valueOrDefault(env, "SERVER_HOST", DEFAULT_HOST);
        String rawPort = valueOrDefault(env, "SERVER_PORT", String.valueOf(DEFAULT_PORT));
        int port;
        try {
            port = Integer.parseInt(rawPort);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SERVER_PORT must be a number between 1 and 65535 (got '" + rawPort + "').");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("SERVER_PORT must be a number between 1 and 65535 (got " + port + ").");
        }
        if (db.matches(".*[/\\\\. \"$*<>:|?].*")) {
            throw new IllegalArgumentException("MONGODB_DATABASE contains characters MongoDB does not allow in a database name.");
        }
        return new AppConfig(uri, db, port, host);
    }

    /** The connection string with any user:password part hidden, safe to print or log. */
    public String safeMongoUri() {
        return mongoUri.replaceFirst("(?<=//)[^/@]*@", "***@");
    }

    private static String valueOrDefault(Map<String, String> env, String key, String fallback) {
        String v = env.get(key);
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }
}
