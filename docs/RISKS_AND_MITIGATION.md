# Risks and Mitigation

A starting register of risks that are realistic for this project. Ratings are proposals, not measurements; edit them. None of these risks is claimed to have happened.

| ID | Risk | Likelihood | Impact | Mitigation | Contingency | Status |
|---|---|---|---|---|---|---|
| R1 | MongoDB service not running at demo time | Medium | High | `check-environment.bat` tests it first; clear start instructions | Show unit tests + Postman run; start service manually | Open |
| R2 | First build needs internet (Maven download, Maven Central) | Medium | High | Run `build.bat` once at home before the demo; dependencies are then cached in `%USERPROFILE%\.m2` | Copy the `.m2` cache and `target\*.jar` to the demo PC | Open |
| R3 | Port 8080 already used | Medium | Medium | Script detects it and explains; `SERVER_PORT` override | Use another port | Open |
| R4 | Java older than 17 on the PC | Low | High | Script checks version and explains | Install a JDK 17+ | Open |
| R5 | Sprint evidence looks reconstructed | Medium | High | Keep real Daily Scrum log, commits and Jira history from now on; never backdate | Explain honestly; show what is real | Open |
| R6 | Duplicate votes via cleared browser storage | High (by design) | Low | Documented as demonstration-level; unique index still protects per voter id | Describe real authentication as future work | Accepted |
| R7 | Anyone can close any poll | High (by design) | Low | Documented limitation | Future work: creator token | Accepted |
| R8 | Dependency versions not verified in the generation sandbox | Medium | Medium | First `.\build.bat` on your PC confirms; fix versions in `pom.xml` if Maven reports a problem | Ask for help with the exact error text | Open |
| R9 | Solo project: no independent review | High | Medium | Self-review checklist in CODE_REVIEW_EVIDENCE.md; ask professor to approve alternatives | - | Open |
| R10 | Running out of time in Sprint 2 | Medium | Medium | Core features already implemented; remaining work is verification and documentation | Drop optional polish | Open |
