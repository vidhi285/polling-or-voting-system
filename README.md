# Online Polling and Voting System

CSE706 mini-project (*Agile Development of a Mini Software Product Using the Scrum Framework*). A real full-stack app: **plain Java** (`HttpServer`) + **MongoDB** + **vanilla HTML/CSS/JS**, built with Maven, tested with JUnit 5 and Postman/Newman, with GitHub Actions CI. No Spring Boot, no Node backend.

## Product vision and goal
Let anyone create a simple poll, vote once, and see trustworthy results, with the data stored in MongoDB and the rules enforced by the backend.

## Features
Dashboard (real totals) - available polls with status, empty/loading/error states - create poll (2-10 options, add/remove, client and server validation) - vote once per poll - results with counts, percentages and bars - close a poll (votes kept, new votes rejected) - dark/light theme toggle (top-right corner, remembered) - responsive layout.

## Quick start (Windows, PowerShell or Command Prompt)
Prerequisites you install once: **JDK 17 or newer**, **MongoDB Server running locally** (`mongodb://127.0.0.1:27017`), internet access for the first build (Maven dependencies).
```
cd voting_polling_system
.\run.bat
```
`run.bat` checks Java, Maven (downloads Apache Maven once via `mvnw.cmd` if you have none), MongoDB and the port; compiles; runs the unit tests; and **only then** starts the server. Open **http://localhost:8080** (never open `index.html` as a file). Stop with Ctrl+C (answer `Y` if Windows asks to terminate the batch job).

| Command | What it does |
|---|---|
| `.\run.bat` | check, build, test, start the server |
| `.\test.bat` | compile + JUnit unit tests only (no MongoDB, no server) |
| `.\build.bat` | clean build -> `target\polling-voting-system.jar` |
| `.\check-environment.bat` | just the environment checks |
Failures return a non-zero exit code and print what to fix.

## MongoDB - nothing to create by hand
On start the app pings MongoDB, creates the `polls` and `votes` collections if missing, creates the indexes (including the **unique** `{pollId, voterId}` index) and verifies it. Database `polling_voting_db`. No sample data is inserted; a new database shows an empty state. If MongoDB is down the app prints the address it tried (credentials hidden), exits with code 2, and never falls back to memory.

Optional environment variables (defaults in brackets): `MONGODB_URI` [`mongodb://127.0.0.1:27017`], `MONGODB_DATABASE` [`polling_voting_db`], `SERVER_PORT` [`8080`], `SERVER_HOST` [`127.0.0.1`]. Example: `$env:SERVER_PORT="8081"; .\run.bat`. A `.env` file is **not** read (see `.env.example`). For MongoDB Atlas: create a free cluster, a database user, allow your IP under Network Access, then set `MONGODB_URI` to the `mongodb+srv://...` string (never commit it).

## Technology and versions
Java 17+ (compiled with `--release 17`) - mongodb-driver-sync 5.2.1 - Jackson 2.17.2 - JUnit 5.11.0 - Maven 3.9.9 (via `mvnw.cmd`) - surefire 3.5.0 / shade 3.6.0 / compiler 3.13.0.

## Folder structure
```
voting_polling_system/
  pom.xml  run.bat  test.bat  build.bat  check-environment.bat  mvnw.cmd
  .gitignore  .env.example  README.md
  .github/workflows/ci.yml
  src/main/java/com/pollingsystem/
      Application.java  WebServer.java
      config/      AppConfig
      model/       Poll, PollOption, PollStatus, Vote, PollResults, OptionResult, PollStats
      dto/         request/response JSON shapes
      exception/   ApiException family
      repository/  PollRepository, VoteRepository, StatisticsRepository, HealthChecker
      repository/mongo/  MongoConnection (init + indexes), Mongo*Repository, MongoGuard
      service/     PollService, VoteService, ResultsService, PollValidator, ResultCalculator
      controller/  ApiHandler (routing/errors), PollController, VoteController,
                   HealthController, StaticFileHandler
      util/        Json, Ids
  src/main/resources/static/  index.html  css/style.css  js/app.js
  src/test/java/...           unit, HTTP and (tagged) MongoDB integration tests
  tests/postman/Online-Polling-System.postman_collection.json
  docs/  (API, ARCHITECTURE, TDD_EVIDENCE, FUNCTIONAL_TESTING, JIRA_MAPPING, GIT_COMMIT_PLAN,
          DAILY_SCRUM_LOGS, SPRINT_1/2 review + retrospective, SPRINT_METRICS,
          RISKS_AND_MITIGATION, SCRUM_AND_TESTING_PRACTICES, CODE_REVIEW_EVIDENCE, FINAL_REPORT_TEMPLATE)
```

## API summary (details in `docs/API.md`)
`GET /api/health` - `GET /api/stats` - `GET|POST /api/polls` - `GET /api/polls/{id}` - `POST /api/polls/{id}/votes` - `GET /api/polls/{id}/results` - `POST /api/polls/{id}/close`. Errors always look like `{"error":"CODE","message":"...","details":[...]}`.

## Architecture and SOLID
Controllers -> services -> repository interfaces -> MongoDB implementations, wired by constructor injection in `Application`. Services never import MongoDB classes. Duplicate votes are stopped by MongoDB's unique index through a single atomic insert, so concurrent duplicates cannot both succeed. See `docs/ARCHITECTURE.md`.

## Testing
- Unit and HTTP tests (81, no MongoDB needed): `.\test.bat`
- MongoDB integration tests (6, need MongoDB): `.\mvnw.cmd -B -ntp verify -Pintegration` (skipped automatically if MongoDB is unreachable)
- Functional API tests: start `.\run.bat`, then in another terminal `npx --yes newman run tests/postman/Online-Polling-System.postman_collection.json` (or import the file into Postman)
- TDD evidence (real Red/Green runs for poll creation and voting): `docs/TDD_EVIDENCE.md`

## GitHub Actions
`.github/workflows/ci.yml` runs on push/PR: **build-and-test** (compile, unit tests, jar, test summary; fails on any error), **integration-tests** (MongoDB service container), **functional-tests** (starts the app against MongoDB, runs Newman). Connecting to your repository and committing: see `docs/GIT_COMMIT_PLAN.md`. Nothing has been pushed.

## Using the app
Home lists polls and totals -> **Create a poll** -> open a poll, pick one option, **Submit vote** -> results update -> **Close poll** (confirm). The sun/moon button in the top-right switches theme.

## Jira and Scrum
Mapping of every file, endpoint and test to PVS-1...PVS-22: `docs/JIRA_MAPPING.md` (including the PVS-4 "Spring Boot" -> plain Java note you must apply by hand in Jira). Scrum templates (daily log, reviews, retrospectives, metrics) are **blank on purpose**; fill them only with what really happens.

## What was verified when this project was generated (2026-10-19, sandbox)
| Check | Result |
|---|---|
| Compile all main + test code (JDK 21, `--release 17`) | passed |
| 81 unit/HTTP tests (JUnit launcher) | **81 passed, 0 failed** |
| Postman collection via Newman against the real HTTP stack on in-memory fakes | 13 requests, 28 assertions, 0 failed |
| MongoDB layer | type-checked only, against an old 3.6 driver with a temporary stub; **never run** |
| Maven build (`pom.xml`, shaded jar), dependency download | **not run** (no access to Maven Central) |
| `run.bat`, `test.bat`, `build.bat`, `mvnw.cmd` | **not run** (no Windows) |
| MongoDB integration tests, start-up failure messages, real end-to-end browser use | **not run** (no MongoDB, no browser) |
| GitHub Actions workflow | YAML parses; **never executed** |
Your first `.\run.bat` is therefore the first real end-to-end run. If anything fails, send me the exact output.

## Known limitations
- Voter IDs live in browser `localStorage` -> demonstration-level duplicate prevention, not real identity (clear storage or change browser = new voter).
- Anyone can close any poll (no creator authorisation).
- A vote arriving at the very instant a poll is closed may still be counted.
- No HTTPS, authentication, rate limiting or CSP; the server listens on `127.0.0.1` only by default.
- `mvnw.cmd` is a simple bootstrap script, not the official Maven Wrapper.

## Troubleshooting
| Symptom | Fix |
|---|---|
| "Java was not found" / "needs Java 17" | install a JDK 17+ (adoptium.net), reopen the terminal, check `java -version` and `javac -version` |
| "MongoDB is not reachable" | Win+R -> `services.msc` -> start *MongoDB Server*; or `net start MongoDB` in an Administrator terminal |
| "Port 8080 is already in use" | `netstat -ano \| findstr :8080`, then `taskkill /PID <pid> /F`, or `$env:SERVER_PORT="8081"` |
| Maven download fails | check internet/proxy; or install Maven and put it on PATH |
| Tests fail | read the "Tests run" section; the server is intentionally not started |
| `.\run.bat` blocked in PowerShell | run it as `cmd /c run.bat` (the scripts do not change any security setting) |
