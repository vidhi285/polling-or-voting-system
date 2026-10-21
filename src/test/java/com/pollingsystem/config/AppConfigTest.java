package com.pollingsystem.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test
    void usesLocalDefaultsWhenNothingIsSet() {
        AppConfig c = AppConfig.fromEnv(Map.of());
        assertEquals("mongodb://127.0.0.1:27017", c.mongoUri());
        assertEquals("polling_voting_db", c.databaseName());
        assertEquals(8080, c.port());
    }

    @Test
    void environmentVariablesOverrideDefaults() {
        AppConfig c = AppConfig.fromEnv(Map.of("MONGODB_URI", "mongodb://db:27018", "MONGODB_DATABASE", "other", "SERVER_PORT", "9090"));
        assertEquals("mongodb://db:27018", c.mongoUri());
        assertEquals("other", c.databaseName());
        assertEquals(9090, c.port());
    }

    @Test
    void blankValuesFallBackToDefaults() {
        assertEquals(8080, AppConfig.fromEnv(Map.of("SERVER_PORT", "  ")).port());
    }

    @Test
    void rejectsNonNumericOrOutOfRangePort() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromEnv(Map.of("SERVER_PORT", "abc")));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromEnv(Map.of("SERVER_PORT", "0")));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromEnv(Map.of("SERVER_PORT", "70000")));
    }

    @Test
    void rejectsInvalidDatabaseName() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromEnv(Map.of("MONGODB_DATABASE", "bad/name")));
    }

    @Test
    void safeUriHidesCredentials() {
        AppConfig c = AppConfig.fromEnv(Map.of("MONGODB_URI", "mongodb+srv://alice:s3cret@cluster0.example.net/app?retryWrites=true"));
        assertFalse(c.safeMongoUri().contains("s3cret"));
        assertFalse(c.safeMongoUri().contains("alice"));
        assertTrue(c.safeMongoUri().contains("cluster0.example.net"));
    }

    @Test
    void safeUriLeavesCredentialFreeUriUntouched() {
        assertEquals("mongodb://127.0.0.1:27017", AppConfig.fromEnv(Map.of()).safeMongoUri());
    }
}
