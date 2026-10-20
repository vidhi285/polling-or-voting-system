package com.pollingsystem.repository.mongo;

import com.mongodb.MongoException;
import com.pollingsystem.exception.DataAccessException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Converts driver exceptions into DataAccessException so services never see MongoDB types. */
final class MongoGuard {
    private static final Logger LOG = Logger.getLogger(MongoGuard.class.getName());

    private MongoGuard() { }

    static <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (MongoException e) {
            LOG.log(Level.WARNING, "MongoDB operation failed", e);
            throw new DataAccessException("The database is currently unavailable. Please try again.", e);
        }
    }

    static void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }
}
