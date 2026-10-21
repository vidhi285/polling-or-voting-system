# Scrum and Testing Practices

## Roles (individual project)
You hold all three roles: **Product Owner** (orders the backlog, accepts stories), **Scrum Master** (keeps the events and the process), **Developer**. Write this in your report and ask your professor to confirm that a one-person team is acceptable. Do not invent teammates.

## Events and how to run them alone
| Event | Solo version | Evidence to keep |
|---|---|---|
| Sprint Planning | choose stories from the backlog, set the goal, estimate points | Jira sprint start screenshot |
| Daily Scrum | 5-minute written plan/progress/blocker note | DAILY_SCRUM_LOGS.md, timestamps from commits |
| Sprint Review | demo to professor/classmate if possible, otherwise record a screen demo | SPRINT_n_REVIEW.md, recording |
| Retrospective | timed written reflection, max 2 actions | SPRINT_n_RETROSPECTIVE.md |
Artifacts: Product Backlog, Sprint Backlog, Increment (the running application). Definition of Done suggestion: code compiles, unit tests green (`.\test.bat`), story's acceptance criteria demonstrated, documented, committed.

## Agile Testing Quadrants - what exists in this project
| Quadrant | Purpose | In this project | Status |
|---|---|---|---|
| Q1 technology-facing, supports the team | unit/component tests | JUnit: PollServiceTest, VoteServiceTest, ResultsServiceTest, PollLifecycleTest, AppConfigTest | Exists (81 unit tests passed in the sandbox) |
| Q2 business-facing, supports the team | functional / story tests | HttpApiTest, Postman collection (13 requests) | Exists; Postman run only against in-memory harness so far |
| Q3 business-facing, critiques the product | exploratory / usability testing | manual checklist below | **Not yet performed** |
| Q4 technology-facing, critiques the product | performance, security, load | only the concurrency test for duplicate votes and the MongoIntegrationTest race test | Mostly **not done** (no load or security testing) |

### Q3 manual exploratory checklist (tick only what you really tried)
- [ ] Create a poll with 2 and with 10 options; try 11
- [ ] Vote, reload the page, try to vote again
- [ ] Clear browser storage and vote again (expected: allowed - documented limitation)
- [ ] Close a poll in one tab, vote from another tab
- [ ] Stop MongoDB while the app runs and watch the error messages
- [ ] Phone-width window; dark and light theme; keyboard-only navigation
