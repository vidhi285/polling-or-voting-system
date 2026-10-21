# Jira Mapping

Nothing in Jira was changed by this project. Statuses below are the **last known statuses you gave** (not verified); update Jira yourself. No story points are assigned here: use your own estimates and count points **once** (on the product story, not again on the planning task that delivers it).

## PVS-4 - architecture change (manual step for you)
Issue title is "Set Up Java and Spring Boot Project". The project uses **plain Java with `HttpServer` + Maven**, not Spring Boot.
Recommended manual edit in Jira (keeps the ID and history): rename to **"Set Up Java (plain HttpServer) and Maven Project"** and add a comment: *"Architecture changed: Spring Boot replaced by JDK HttpServer + MongoDB sync driver, see docs/ARCHITECTURE.md."*

## Planning tasks
| Issue | Last known status | Delivered by (files) |
|---|---|---|
| PVS-1 Product vision and goal | Done | README.md (vision), docs/FINAL_REPORT_TEMPLATE.md |
| PVS-2 User stories and acceptance criteria | Done | this file (stories below), docs/API.md |
| PVS-3 Product backlog | Done | this file |
| PVS-4 Java project setup | In Progress | pom.xml, Application.java, WebServer.java, run.bat, test.bat, build.bat, check-environment.bat, mvnw.cmd, .gitignore, .env.example |
| PVS-5 SOLID design | In Progress | docs/ARCHITECTURE.md, packages model/repository/service/controller, repository interfaces |
| PVS-6 Connect to MongoDB | To Do | config/AppConfig.java, repository/mongo/MongoConnection.java, MongoGuard.java, MongoIntegrationTest.java |
| PVS-7 Implement poll creation | To Do | service/PollService.java, PollValidator.java, PollController.java, POST /api/polls |
| PVS-8 JUnit tests for poll creation | To Do | test/service/PollServiceTest.java (20 tests), docs/TDD_EVIDENCE.md |
| PVS-9 Implement voting | - | service/VoteService.java, VoteController.java, POST /api/polls/{id}/votes |
| PVS-10 JUnit tests for voting | - | test/service/VoteServiceTest.java (17 tests) |
| PVS-11 Display poll results | - | ResultCalculator.java, ResultsService.java, GET .../results, app.js results view |
| PVS-12 Functional testing | - | tests/postman/*.json, HttpApiTest.java, docs/FUNCTIONAL_TESTING.md |
| PVS-13 GitHub Actions CI | - | .github/workflows/ci.yml |
| PVS-14 Sprint reviews and retrospectives | - | docs/SPRINT_1_REVIEW.md ... SPRINT_2_RETROSPECTIVE.md (templates to fill in) |
| PVS-15 Final report and sprint metrics | - | docs/SPRINT_METRICS.md, docs/FINAL_REPORT_TEMPLATE.md |
(`-` = status not provided to me.)

## Product stories
| Story | Backend | Frontend (`static/`) | Tests |
|---|---|---|---|
| PVS-16 Create a poll | `POST /api/polls`, PollService.createPoll, MongoPollRepository.save | index.html + app.js `renderCreate` | PollServiceTest, HttpApiTest.createPoll*, Postman 02 |
| PVS-17 Cast a vote | `POST /api/polls/{id}/votes`, VoteService | app.js `renderPoll` (ballot form) | VoteServiceTest, HttpApiTest.fullVotingFlow, Postman 06 |
| PVS-18 View results | `GET /api/polls/{id}/results`, ResultCalculator | app.js result bars | ResultsServiceTest, Postman 07/09/12 |
| PVS-19 View available polls | `GET /api/polls`, `GET /api/stats`, `GET /api/polls/{id}` | app.js `renderHome` (dashboard, list, empty state) | PollLifecycleTest, HttpApiTest, Postman 03/04 |
| PVS-20 Prevent duplicate voting | unique index `uniq_poll_voter` + atomic `insertOne` | app.js shows "already voted" from `DUPLICATE_VOTE` | VoteServiceTest (incl. concurrency), MongoIntegrationTest, Postman 08/09 |
| PVS-21 Validate poll details | PollValidator -> 400 `VALIDATION_ERROR` + `details` | client-side checks in `renderCreate` | PollServiceTest, HttpApiTest.invalidPoll*, Postman 05 |
| PVS-22 Close a poll | `POST /api/polls/{id}/close`, `closeIfActive` | "Close poll" button with confirm | PollLifecycleTest, Postman 10-12 |

## Endpoint -> story
health: PVS-6 / PVS-12 | polls (GET): PVS-19 | polls (POST): PVS-16, PVS-21 | polls/{id}: PVS-19 | votes: PVS-17, PVS-20 | results: PVS-18 | close: PVS-22 | stats: PVS-19.
