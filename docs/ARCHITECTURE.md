# Architecture

## Selected stack and versions
| Item | Choice |
|---|---|
| Language | Java 17 minimum (`maven.compiler.release=17`); also compiled with JDK 21 |
| HTTP | JDK built-in `com.sun.net.httpserver.HttpServer` (no Spring Boot) |
| Database | MongoDB via `mongodb-driver-sync` **5.2.1** |
| JSON | Jackson databind **2.17.2** |
| Build | Maven (**3.9.9** via `mvnw.cmd` bootstrap, or any Maven on PATH); compiler plugin 3.13.0, surefire 3.5.0, shade 3.6.0 |
| Tests | JUnit Jupiter **5.11.0** |
| Frontend | HTML5, CSS3, vanilla JS (`fetch`), served by the same server |

> These dependency versions could not be downloaded in the sandbox where the project was generated (Maven Central was unreachable), so the Maven build itself is **unverified**; see README "What was verified".

## Layers (dependencies point downward only)
```
Browser (index.html, app.js)  --fetch-->  ApiHandler / StaticFileHandler      controller
                                          PollController, VoteController,     controller
                                          HealthController
                                          PollService, VoteService,           service
                                          ResultsService, PollValidator,
                                          ResultCalculator
                                          PollRepository, VoteRepository,     repository (interfaces)
                                          StatisticsRepository, HealthChecker
                                          Mongo*Repository, MongoConnection   repository.mongo
                                          MongoDB (polls, votes)
```
Packages: `config` (AppConfig), `model` (records), `dto` (JSON shapes), `exception`, `util` (Json, Ids), `Application` (composition root), `WebServer`.

## SOLID mapping
- **S**: HTTP parsing/routing (`ApiHandler`), JSON (`Json`), validation (`PollValidator`), rules (`*Service`), persistence (`Mongo*Repository`) are separate classes.
- **O**: new storage = new implementation of the repository interfaces; services unchanged. New endpoints = new controller method + route line.
- **L**: the in-memory fakes used in tests and the Mongo repositories are interchangeable behind the same interfaces (same duplicate-vote contract).
- **I**: `PollRepository`, `VoteRepository`, `StatisticsRepository`, `HealthChecker` are small and separate.
- **D**: services depend on repository interfaces, never on MongoDB classes. `Application` wires everything with constructor injection.

## Data model
`polls`: `{ _id: <uuid string>, question, options: [{id, text}], status: "ACTIVE"|"CLOSED", createdAt: Date, closedAt: Date|null }`
`votes`: `{ _id: <uuid string>, pollId, voterId, optionId, castAt: Date }`

Indexes created automatically at start-up (idempotent):
- `votes`: **unique** `{pollId:1, voterId:1}` named `uniq_poll_voter`; `{pollId:1, optionId:1}`
- `polls`: `{createdAt:-1}`, `{status:1}`
Start-up verifies the unique index exists, otherwise the application refuses to start.

## Duplicate-vote design (race-free)
`VoteService` never does "check, then insert". It calls `VoteRepository.insert`, which performs a single `insertOne`; MongoDB's unique index decides atomically. A duplicate-key error is translated to `DuplicateVoteException` -> `409`. Two simultaneous identical requests therefore cannot both succeed.
Known small window: a vote that passes the "poll is active" check at the same instant the poll is closed can still be stored. For this prototype that is accepted and documented.

## Failure behaviour
- MongoDB down at start-up: clear message with the credential-free address, exit code 2. No in-memory fallback.
- MongoDB down later: API returns `503 DATABASE_UNAVAILABLE`; `/api/health` reports `DOWN`.
- Port busy: message and exit code 3. Invalid config: exit code 1.
- Shutdown hook (Ctrl+C) stops the HTTP server and closes the Mongo client.

## Prototype limitations
- Voter ID is a random id stored in the browser (`localStorage`). Clearing storage or switching browsers gives a new identity. This is demonstration-level duplicate prevention, not authentication.
- Anyone can close any poll (no creator authorisation).
- No rate limiting, HTTPS or CSP header (the server binds to `127.0.0.1` by default).
