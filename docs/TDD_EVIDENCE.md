# TDD Evidence

## How this evidence was produced (read first)
The whole project was generated in **one AI-assisted session on 2026-10-19**, not across the sprint dates. Inside that session the Red-Green steps below were **actually executed in order**: tests were written against a stub that throws `UnsupportedOperationException`, run (RED), then the implementation was written and the tests re-run (GREEN). There is no Git history of these steps; create one honestly with the commit plan if you replay them yourself. Do not present this as work done on earlier dates.

Runner used in the generation sandbox: `javac --release 17` plus the JUnit Platform Console Launcher (JUnit 5.9 engine bundled in the launcher), **not Maven** (Maven Central was unreachable there). On your PC use `.\test.bat`.

## Module 1 - Poll creation and validation (PVS-7, PVS-8, PVS-16, PVS-21)
File: `src/test/java/com/pollingsystem/service/PollServiceTest.java`

| Step | What was done | Observed result |
|---|---|---|
| RED | 20 tests written; `PollService.createPoll` only threw `UnsupportedOperationException` | 20 found, **0 passed, 20 failed** |
| GREEN | `PollValidator` and `PollService.createPoll` implemented | 20 found, **20 passed, 0 failed** |
| REFACTOR | The validation rules were put in their own class (`PollValidator`) while writing the implementation, so there was no separate later refactor step | tests stayed green |

Tests (what each verifies): rejects null / empty / whitespace-only question; question over 200 chars rejected, exactly 200 accepted; null options list; fewer than two options; empty option; whitespace-only option; null option entry; duplicate options (case/space-insensitive); more than ten options; option over 100 chars; all problems reported together; question and options trimmed; poll is ACTIVE with `createdAt` from the clock and an id; every option gets a unique stable id; valid poll persisted; two polls get different ids; database failure propagates (`DataAccessException`); invalid poll is never stored.

## Module 2 - Voting and duplicate-vote prevention (PVS-9, PVS-10, PVS-17, PVS-20)
File: `src/test/java/com/pollingsystem/service/VoteServiceTest.java`

| Step | What was done | Observed result |
|---|---|---|
| RED | 17 voting tests added; `VoteService.castVote` only threw `UnsupportedOperationException` | 37 found (20 from module 1), 20 passed, **17 failed** |
| GREEN | `VoteService.castVote` implemented (validate -> load poll -> closed check -> option check -> atomic insert) | 37 found, **37 passed, 0 failed** |
| REFACTOR | None needed after GREEN | - |

Tests: valid vote recorded; voter id trimmed; second vote by same voter rejected; duplicate for a different option rejected and changes nothing; different voters allowed; same voter in different polls allowed; nonexistent poll -> not found; closed poll -> rejected; option from another poll rejected; invalid option id rejected; option looked up by stable id not by position; missing/blank voter id; missing/blank option id; over-long voter id; database failure when reading the poll; database failure when storing the vote; **24 concurrent identical requests -> exactly one accepted**.

## Other tests (written AFTER the code - not TDD)
`PollLifecycleTest` (7: list order, get, close, close twice, close keeps votes), `ResultsServiceTest` (8: zero votes, percentages, rounding, option order, closed results, stats), `AppConfigTest` (7), `HttpApiTest` (22: real `HttpServer` with in-memory repositories - status codes, error format, routing, static files, frontend JSON field names).
`MongoIntegrationTest` (6, tagged `integration`): needs a live MongoDB; **not executed** (see below).

## Final run in the sandbox
`81 tests found, 81 successful, 0 failed` (all unit tests; integration test excluded).

## Commands
```
.\test.bat                              (unit tests, no MongoDB needed)
.\mvnw.cmd -B -ntp test                 (same, direct)
.\mvnw.cmd -B -ntp verify -Pintegration (MongoDB integration tests; needs MongoDB running)
```
Expected: `Tests run: 81, Failures: 0, Errors: 0` for the unit run.

## Limitations
- Unit tests use in-memory fakes. The duplicate-vote guarantee of the **real** unique index is only proven by `MongoIntegrationTest`, which has not been run yet. Run it on your PC and paste the result here.
- The Red phase cannot be shown as historical evidence with dates or commits.
