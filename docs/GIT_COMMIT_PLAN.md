# Git Commit Plan

Rules: commit with **today's real date** (never use `--date` or rewrite history to look earlier); push only when **you** decide. The generated project is complete, so these commits group the existing files by purpose. Commit only what matches the Jira status you are really in.

## 0. Look before you change anything
```
cd path\to\voting_polling_system
git status
git remote -v
git branch --show-current
git log --oneline -10
```
If `git remote -v` is empty, connect to your existing repo (replace the URL with your own; do NOT run this if a remote already exists):
```
git remote add origin https://github.com/<your-username>/polling-or-voting-system.git
git fetch origin
git status
```
If `origin` already exists and points elsewhere, stop and check it. If the GitHub repo already has commits, run `git pull --rebase origin main` (or `master`) and resolve any conflicts before committing.

## 1. Suggested commits (stage specific paths, then commit)
```
git add pom.xml .gitignore .env.example mvnw.cmd
git commit -m "PVS-4: set up plain Java + Maven project (replaces Spring Boot plan)"

git add src/main/java/com/pollingsystem/model src/main/java/com/pollingsystem/exception src/main/java/com/pollingsystem/util
git commit -m "PVS-5: add domain models, exceptions and id utility"

git add src/main/java/com/pollingsystem/repository/PollRepository.java src/main/java/com/pollingsystem/repository/VoteRepository.java src/main/java/com/pollingsystem/repository/StatisticsRepository.java src/main/java/com/pollingsystem/repository/HealthChecker.java docs/ARCHITECTURE.md
git commit -m "PVS-5: define repository abstractions (SOLID)"

git add src/main/java/com/pollingsystem/config src/main/java/com/pollingsystem/repository/mongo src/test/java/com/pollingsystem/config src/test/java/com/pollingsystem/repository
git commit -m "PVS-6: MongoDB connection, automatic collections/indexes, repositories"

git add src/test/java/com/pollingsystem/support src/test/java/com/pollingsystem/service/PollServiceTest.java
git commit -m "PVS-8: tests for poll creation and validation"

git add src/main/java/com/pollingsystem/service/PollService.java src/main/java/com/pollingsystem/service/PollValidator.java
git commit -m "PVS-7: implement poll creation service and validation"

git add src/test/java/com/pollingsystem/service/VoteServiceTest.java
git commit -m "PVS-10: tests for voting and duplicate-vote prevention"

git add src/main/java/com/pollingsystem/service/VoteService.java
git commit -m "PVS-9: implement voting with atomic duplicate prevention"

git add src/main/java/com/pollingsystem/service/ResultCalculator.java src/main/java/com/pollingsystem/service/ResultsService.java src/test/java/com/pollingsystem/service/ResultsServiceTest.java src/test/java/com/pollingsystem/service/PollLifecycleTest.java
git commit -m "PVS-11: results, statistics and poll closing"

git add src/main/java/com/pollingsystem/controller src/main/java/com/pollingsystem/dto src/main/java/com/pollingsystem/WebServer.java src/main/java/com/pollingsystem/Application.java
git commit -m "PVS-4: HTTP server and REST API"

git add src/main/resources/static
git commit -m "PVS-19: frontend with dark/light theme connected to the API"

git add run.bat test.bat build.bat check-environment.bat
git commit -m "PVS-4: Windows run/test/build scripts"

git add tests src/test/java/com/pollingsystem/controller docs/FUNCTIONAL_TESTING.md docs/API.md
git commit -m "PVS-12: HTTP API tests and Postman collection"

git add .github
git commit -m "PVS-13: GitHub Actions CI"

git add README.md docs
git commit -m "PVS-14: Scrum, testing and Jira documentation"
```
Note: the commit for `docs/ARCHITECTURE.md` above is already staged in step 3; `git add docs` later only adds what is left. If a `git add` reports a path that does not exist, skip that line.

## 2. Inspect
```
git status
git log --oneline --graph
git show --stat HEAD
```

## 3. Push (only when you are ready and have checked the remote)
```
git push -u origin main
```
(use `master` if that is your branch name). Then open the repository's **Actions** tab to see the first real CI run. Record the real result in your sprint documents.
