package com.pollingsystem.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollingsystem.model.Poll;
import com.pollingsystem.repository.PollRepository;
import com.pollingsystem.support.FailingRepositories;
import com.pollingsystem.support.InMemoryPollRepository;
import com.pollingsystem.support.InMemoryStatisticsRepository;
import com.pollingsystem.support.InMemoryVoteRepository;
import com.pollingsystem.support.TestServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real HttpServer, routing, JSON and error handling end to end (PVS-12).
 * Repositories are in-memory fakes, so no MongoDB is needed. The JSON field names asserted here are
 * exactly the ones the frontend (app.js) reads.
 */
class HttpApiTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private TestServer server;
    private String base;

    @BeforeEach
    void start() {
        server = TestServer.inMemory(0);
        base = server.baseUrl();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody());
        else b.method(method, HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return mapper.readTree(r.body());
    }

    private JsonNode createPoll() throws Exception {
        HttpResponse<String> r = send("POST", "/api/polls", "{\"question\":\"Best language?\",\"options\":[\"Java\",\"Python\"]}");
        assertEquals(201, r.statusCode());
        return json(r);
    }

    private HttpResponse<String> vote(String pollId, String voter, String optionId) throws Exception {
        return send("POST", "/api/polls/" + pollId + "/votes", "{\"voterId\":\"" + voter + "\",\"optionId\":\"" + optionId + "\"}");
    }

    // ---------- health ----------
    @Test
    void healthIsUpWhenDatabaseIsReachable() throws Exception {
        HttpResponse<String> r = send("GET", "/api/health", null);
        assertEquals(200, r.statusCode());
        assertEquals("UP", json(r).get("status").asText());
        assertEquals("UP", json(r).get("database").asText());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
    }

    @Test
    void healthReportsDatabaseDownWith503() throws Exception {
        InMemoryPollRepository polls = new InMemoryPollRepository();
        InMemoryVoteRepository votes = new InMemoryVoteRepository();
        try (TestServer down = new TestServer(0, polls, votes, new InMemoryStatisticsRepository(polls, votes), () -> false)) {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(down.baseUrl() + "/api/health")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(503, r.statusCode());
            assertEquals("DOWN", mapper.readTree(r.body()).get("database").asText());
        }
    }

    // ---------- polls ----------
    @Test
    void emptyDatabaseReturnsEmptyListAndZeroStats() throws Exception {
        HttpResponse<String> list = send("GET", "/api/polls", null);
        assertEquals(200, list.statusCode());
        assertEquals("[]", list.body());
        JsonNode stats = json(send("GET", "/api/stats", null));
        assertEquals(0, stats.get("totalPolls").asInt());
        assertEquals(0, stats.get("activePolls").asInt());
        assertEquals(0, stats.get("closedPolls").asInt());
        assertEquals(0, stats.get("totalVotes").asInt());
    }

    @Test
    void createPollReturns201WithTheFieldsTheFrontendReads() throws Exception {
        JsonNode poll = createPoll();
        assertFalse(poll.get("id").asText().isBlank());
        assertEquals("Best language?", poll.get("question").asText());
        assertEquals("ACTIVE", poll.get("status").asText());
        assertTrue(poll.get("active").asBoolean());
        assertNotNull(poll.get("createdAt"));
        assertEquals(2, poll.get("options").size());
        assertFalse(poll.get("options").get(0).get("id").asText().isBlank());
        assertEquals("Java", poll.get("options").get(0).get("text").asText());
    }

    @Test
    void listAndGetReturnTheCreatedPoll() throws Exception {
        JsonNode poll = createPoll();
        JsonNode list = json(send("GET", "/api/polls", null));
        assertEquals(1, list.size());
        assertEquals(poll.get("id").asText(), list.get(0).get("id").asText());
        HttpResponse<String> one = send("GET", "/api/polls/" + poll.get("id").asText(), null);
        assertEquals(200, one.statusCode());
        assertEquals("Best language?", json(one).get("question").asText());
    }

    @Test
    void invalidPollReturns400WithUsefulMessages() throws Exception {
        HttpResponse<String> r = send("POST", "/api/polls", "{\"question\":\"  \",\"options\":[\"Only\"]}");
        assertEquals(400, r.statusCode());
        JsonNode body = json(r);
        assertEquals("VALIDATION_ERROR", body.get("error").asText());
        assertTrue(body.get("message").asText().contains("Question is required."));
        assertEquals(2, body.get("details").size());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        HttpResponse<String> r = send("POST", "/api/polls", "{not json");
        assertEquals(400, r.statusCode());
        assertEquals("MALFORMED_JSON", json(r).get("error").asText());
    }

    @Test
    void wrongJsonShapeReturns400() throws Exception {
        assertEquals(400, send("POST", "/api/polls", "[1,2,3]").statusCode());
        assertEquals(400, send("POST", "/api/polls", "{\"question\":\"Q\",\"options\":\"nope\"}").statusCode());
        assertEquals(400, send("POST", "/api/polls", "null").statusCode());
    }

    @Test
    void emptyBodyReturns400() throws Exception {
        HttpResponse<String> r = send("POST", "/api/polls", "");
        assertEquals(400, r.statusCode());
        assertEquals("EMPTY_BODY", json(r).get("error").asText());
    }

    @Test
    void oversizedBodyReturns413() throws Exception {
        String huge = "{\"question\":\"" + "x".repeat(20_000) + "\",\"options\":[\"a\",\"b\"]}";
        HttpResponse<String> r = send("POST", "/api/polls", huge);
        assertEquals(413, r.statusCode());
        assertEquals("PAYLOAD_TOO_LARGE", json(r).get("error").asText());
    }

    @Test
    void unknownPollReturns404() throws Exception {
        HttpResponse<String> r = send("GET", "/api/polls/does-not-exist", null);
        assertEquals(404, r.statusCode());
        assertEquals("NOT_FOUND", json(r).get("error").asText());
        assertEquals(404, send("GET", "/api/polls/does-not-exist/results", null).statusCode());
        assertEquals(404, send("POST", "/api/polls/does-not-exist/close", "").statusCode());
    }

    @Test
    void unknownRouteReturnsJson404() throws Exception {
        HttpResponse<String> r = send("GET", "/api/nothing-here", null);
        assertEquals(404, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(404, send("GET", "/api/polls/x/unknown", null).statusCode());
        assertEquals(404, send("GET", "/api/polls/x/results/extra", null).statusCode());
    }

    @Test
    void wrongMethodReturns405WithAllowHeader() throws Exception {
        HttpResponse<String> r = send("DELETE", "/api/polls", null);
        assertEquals(405, r.statusCode());
        assertEquals("GET, POST", r.headers().firstValue("Allow").orElse(""));
        assertEquals(405, send("GET", "/api/polls/x/votes", null).statusCode());
        assertEquals(405, send("POST", "/api/health", "").statusCode());
        assertEquals(405, send("PUT", "/api/polls/x", "{}").statusCode());
    }

    // ---------- voting, results, closing ----------
    @Test
    void fullVotingFlow() throws Exception {
        JsonNode poll = createPoll();
        String id = poll.get("id").asText();
        String java = poll.get("options").get(0).get("id").asText();
        String python = poll.get("options").get(1).get("id").asText();

        HttpResponse<String> ok = vote(id, "voter-1", java);
        assertEquals(200, ok.statusCode());
        assertFalse(ok.body().contains("voter-1"), "voter identity must not be echoed back");

        HttpResponse<String> dup = vote(id, "voter-1", python);
        assertEquals(409, dup.statusCode());
        assertEquals("DUPLICATE_VOTE", json(dup).get("error").asText());
        assertEquals("You have already voted in this poll.", json(dup).get("message").asText());

        assertEquals(200, vote(id, "voter-2", java).statusCode());
        assertEquals(200, vote(id, "voter-3", python).statusCode());

        JsonNode results = json(send("GET", "/api/polls/" + id + "/results", null));
        assertEquals(3, results.get("totalVotes").asInt());
        JsonNode r0 = results.get("options").get(0);
        assertEquals(java, r0.get("optionId").asText());
        assertEquals("Java", r0.get("text").asText());
        assertEquals(2, r0.get("votes").asInt());
        assertEquals(66.7, r0.get("percentage").asDouble(), 0.0001);
        assertEquals(33.3, results.get("options").get(1).get("percentage").asDouble(), 0.0001);

        JsonNode stats = json(send("GET", "/api/stats", null));
        assertEquals(1, stats.get("totalPolls").asInt());
        assertEquals(3, stats.get("totalVotes").asInt());
    }

    @Test
    void duplicateVoteDoesNotChangeCounts() throws Exception {
        JsonNode poll = createPoll();
        String id = poll.get("id").asText();
        String opt = poll.get("options").get(0).get("id").asText();
        vote(id, "voter-1", opt);
        vote(id, "voter-1", opt);
        assertEquals(1, json(send("GET", "/api/polls/" + id + "/results", null)).get("totalVotes").asInt());
    }

    @Test
    void invalidOptionAndMissingFieldsReturn400() throws Exception {
        String id = createPoll().get("id").asText();
        assertEquals(400, vote(id, "voter-1", "bogus").statusCode());
        assertEquals(400, send("POST", "/api/polls/" + id + "/votes", "{\"voterId\":\"v\"}").statusCode());
        assertEquals(404, vote("missing", "voter-1", "x").statusCode());
    }

    @Test
    void closingBlocksVotingButKeepsResults() throws Exception {
        JsonNode poll = createPoll();
        String id = poll.get("id").asText();
        String opt = poll.get("options").get(0).get("id").asText();
        vote(id, "voter-1", opt);

        HttpResponse<String> closed = send("POST", "/api/polls/" + id + "/close", "");
        assertEquals(200, closed.statusCode());
        assertEquals("CLOSED", json(closed).get("status").asText());
        assertFalse(json(closed).get("active").asBoolean());

        HttpResponse<String> late = vote(id, "voter-2", opt);
        assertEquals(409, late.statusCode());
        assertEquals("POLL_CLOSED", json(late).get("error").asText());
        assertTrue(json(late).get("message").asText().toLowerCase().contains("closed"));

        JsonNode results = json(send("GET", "/api/polls/" + id + "/results", null));
        assertEquals(1, results.get("totalVotes").asInt());
        assertEquals("CLOSED", results.get("status").asText());

        HttpResponse<String> again = send("POST", "/api/polls/" + id + "/close", "");
        assertEquals(409, again.statusCode());
        assertEquals("CLOSED", json(send("GET", "/api/polls/" + id, null)).get("status").asText());
    }

    // ---------- failure handling ----------
    @Test
    void databaseFailureReturns503WithoutInternals() throws Exception {
        InMemoryVoteRepository votes = new InMemoryVoteRepository();
        try (TestServer broken = new TestServer(0, new FailingRepositories.Polls(), votes, () -> null, () -> false)) {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(broken.baseUrl() + "/api/polls")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(503, r.statusCode());
            assertEquals("DATABASE_UNAVAILABLE", mapper.readTree(r.body()).get("error").asText());
            assertFalse(r.body().contains("Exception"));
        }
    }

    @Test
    void unexpectedErrorReturnsGeneric500WithoutLeakingDetails() throws Exception {
        PollRepository exploding = new PollRepository() {
            public void save(Poll p) { throw new IllegalStateException("secret-internal-detail"); }
            public Optional<Poll> findById(String id) { throw new IllegalStateException("secret-internal-detail"); }
            public List<Poll> findAll() { throw new IllegalStateException("secret-internal-detail"); }
            public boolean closeIfActive(String id, Instant t) { throw new IllegalStateException("secret-internal-detail"); }
        };
        InMemoryVoteRepository votes = new InMemoryVoteRepository();
        try (TestServer broken = new TestServer(0, exploding, votes, () -> null, () -> true)) {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(broken.baseUrl() + "/api/polls")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(500, r.statusCode());
            assertEquals("INTERNAL_ERROR", mapper.readTree(r.body()).get("error").asText());
            assertFalse(r.body().contains("secret-internal-detail"));
            assertFalse(r.body().contains("at com."));
        }
    }

    // ---------- static frontend ----------
    @Test
    void servesFrontendOverHttpWithCorrectContentTypes() throws Exception {
        HttpResponse<String> index = send("GET", "/", null);
        assertEquals(200, index.statusCode());
        assertTrue(index.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertTrue(index.body().contains("Ballot"));
        assertTrue(index.body().contains("id=\"themeToggle\""));
        assertTrue(send("GET", "/css/style.css", null).headers().firstValue("Content-Type").orElse("").startsWith("text/css"));
        assertTrue(send("GET", "/js/app.js", null).headers().firstValue("Content-Type").orElse("").startsWith("text/javascript"));
        assertEquals(200, send("GET", "/index.html", null).statusCode());
    }

    @Test
    void staticHandlerRejectsUnknownFilesDirectoriesAndTraversal() throws Exception {
        assertEquals(404, send("GET", "/missing.html", null).statusCode());
        assertEquals(404, send("GET", "/css/", null).statusCode());
        assertEquals(404, send("GET", "/css", null).statusCode());
        assertEquals(404, send("GET", "/%2e%2e/pom.xml", null).statusCode());
        assertEquals(404, send("GET", "/static/../../pom.xml", null).statusCode());
        assertEquals(404, send("GET", "/com/pollingsystem/Application.class", null).statusCode());
        assertEquals(405, send("POST", "/", "x").statusCode());
    }

    @Test
    void frontendOnlyCallsEndpointsTheBackendImplements() throws Exception {
        String js = send("GET", "/js/app.js", null).body();
        for (String path : List.of("/api/polls", "/api/stats", "/votes", "/results", "/close")) {
            assertTrue(js.contains(path), "frontend no longer references " + path);
        }
        assertTrue(js.contains("optionId") && js.contains("voterId"));
    }
}
