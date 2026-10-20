package com.pollingsystem;

import com.pollingsystem.config.AppConfig;
import com.pollingsystem.controller.ApiHandler;
import com.pollingsystem.controller.HealthController;
import com.pollingsystem.controller.PollController;
import com.pollingsystem.controller.StaticFileHandler;
import com.pollingsystem.controller.VoteController;
import com.pollingsystem.exception.DataAccessException;
import com.pollingsystem.repository.mongo.MongoConnection;
import com.pollingsystem.repository.mongo.MongoPollRepository;
import com.pollingsystem.repository.mongo.MongoStatisticsRepository;
import com.pollingsystem.repository.mongo.MongoVoteRepository;
import com.pollingsystem.service.PollService;
import com.pollingsystem.service.ResultsService;
import com.pollingsystem.service.VoteService;
import com.pollingsystem.util.Json;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.time.Clock;

/** Composition root: reads config, connects to MongoDB, wires everything with constructor injection. */
public final class Application {
    private Application() { }

    public static void main(String[] args) {
        AppConfig config;
        try {
            config = AppConfig.fromEnv(System.getenv());
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] Invalid configuration: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Connecting to MongoDB at " + config.safeMongoUri() + " (database '" + config.databaseName() + "') ...");
        MongoConnection mongo;
        try {
            mongo = MongoConnection.connect(config);
        } catch (DataAccessException e) {
            System.err.println();
            System.err.println("[ERROR] Cannot start: " + e.getMessage());
            System.err.println("  Connection address : " + config.safeMongoUri());
            System.err.println("  What to check      : 1) Is the MongoDB service running? (Windows: Win+R > services.msc > 'MongoDB Server')");
            System.err.println("                       2) Is MONGODB_URI correct? Leave it unset to use the local default.");
            System.err.println("The application will not fall back to in-memory data.");
            System.exit(2);
            return;
        }
        System.out.println("MongoDB ready: collections and indexes verified.");

        Clock clock = Clock.systemUTC();
        Json json = new Json();
        var pollRepository = new MongoPollRepository(mongo.polls());
        var voteRepository = new MongoVoteRepository(mongo.votes());
        var statistics = new MongoStatisticsRepository(mongo.polls(), mongo.votes());

        var pollService = new PollService(pollRepository, clock);
        var voteService = new VoteService(pollRepository, voteRepository, clock);
        var resultsService = new ResultsService(pollRepository, voteRepository, statistics);

        var api = new ApiHandler(
                new PollController(pollService, resultsService, json),
                new VoteController(voteService, resultsService, json),
                new HealthController(mongo, clock),
                json);

        WebServer server;
        try {
            server = new WebServer(new InetSocketAddress(config.host(), config.port()), api, new StaticFileHandler());
        } catch (BindException e) {
            System.err.println("[ERROR] Port " + config.port() + " is already in use.");
            System.err.println("  Stop the other program using it, or choose another port: set SERVER_PORT=8081");
            mongo.close();
            System.exit(3);
            return;
        } catch (Exception e) {
            System.err.println("[ERROR] Could not start the HTTP server: " + e.getMessage());
            mongo.close();
            System.exit(4);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down ...");
            server.stop();
            mongo.close();
            System.out.println("Stopped.");
        }, "shutdown"));

        server.start();
        System.out.println();
        System.out.println("Online Polling and Voting System is running.");
        System.out.println("  Open: http://localhost:" + server.port());
        System.out.println("  Stop: press Ctrl+C");
    }
}
