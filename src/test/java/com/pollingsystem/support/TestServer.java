package com.pollingsystem.support;

import com.pollingsystem.WebServer;
import com.pollingsystem.controller.ApiHandler;
import com.pollingsystem.controller.HealthController;
import com.pollingsystem.controller.PollController;
import com.pollingsystem.controller.StaticFileHandler;
import com.pollingsystem.controller.VoteController;
import com.pollingsystem.repository.HealthChecker;
import com.pollingsystem.repository.PollRepository;
import com.pollingsystem.repository.StatisticsRepository;
import com.pollingsystem.repository.VoteRepository;
import com.pollingsystem.service.PollService;
import com.pollingsystem.service.ResultsService;
import com.pollingsystem.service.VoteService;
import com.pollingsystem.util.Json;
import java.net.InetSocketAddress;
import java.time.Clock;

/** Starts the real HTTP stack on a free port, backed by fake repositories. TEST USE ONLY. */
public final class TestServer implements AutoCloseable {
    private final WebServer server;

    public TestServer(int port, PollRepository polls, VoteRepository votes, StatisticsRepository stats, HealthChecker health) {
        try {
            Clock clock = Clock.systemUTC();
            Json json = new Json();
            ApiHandler api = new ApiHandler(
                    new PollController(new PollService(polls, clock), new ResultsService(polls, votes, stats), json),
                    new VoteController(new VoteService(polls, votes, clock), new ResultsService(polls, votes, stats), json),
                    new HealthController(health, clock),
                    json);
            server = new WebServer(new InetSocketAddress("127.0.0.1", port), api, new StaticFileHandler());
            server.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static TestServer inMemory(int port) {
        InMemoryPollRepository polls = new InMemoryPollRepository();
        InMemoryVoteRepository votes = new InMemoryVoteRepository();
        return new TestServer(port, polls, votes, new InMemoryStatisticsRepository(polls, votes), () -> true);
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.port();
    }

    @Override
    public void close() {
        server.stop();
    }
}
