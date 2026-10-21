# Functional (API) Testing with Postman / Newman  (PVS-12)

Collection: `tests/postman/Online-Polling-System.postman_collection.json` - 13 requests, 28 assertions, run in order. It creates its own poll, so the database does not need to be pre-populated. Variables (`baseUrl`, `pollId`, `optionId`, `optionId2`, `voterId`) are filled from earlier responses.

| # | Request | Checks |
|---|---|---|
| 01 | Health check | 200, status and database UP (also sets a fresh `voterId`) |
| 02 | Create valid poll | 201, ACTIVE, 3 options with ids, stores ids |
| 03 | List polls | 200, array contains the new poll |
| 04 | Get one poll | 200, same id/question |
| 05 | Invalid poll | 400, `VALIDATION_ERROR`, >= 2 details |
| 06 | Valid vote | 200, voter id not echoed |
| 07 | Results | totalVotes 1, 100 %, others 0 |
| 08 | Duplicate vote | 409 `DUPLICATE_VOTE` |
| 09 | Count unchanged | totalVotes still 1 |
| 10 | Close poll | 200, CLOSED |
| 11 | Vote on closed poll | 409 `POLL_CLOSED` |
| 12 | Closed results | results preserved |
| 13 | Unknown poll | 404 `NOT_FOUND` |

## Run it on your PC
1. `.\run.bat` in one terminal (leave it running).
2. In a second terminal (needs Node.js; Newman is downloaded on demand):
```
npx --yes newman run tests/postman/Online-Polling-System.postman_collection.json
```
Or import the JSON file into the Postman app and click Run. Expected: `requests 13 / failed 0`, `assertions 28 / failed 0`.

## What was actually run
On 2026-10-19 in the generation sandbox, Newman ran this collection against the **real Java HTTP stack backed by in-memory fake repositories** (`InMemoryServerLauncher`, a test-only harness, because the sandbox has no MongoDB): **13 requests, 28 assertions, 0 failures**.
It has **not** been run against the real MongoDB-backed application. That run is yours to do with the two commands above (GitHub Actions also runs it in the `functional-tests` job once pushed, which has not happened yet).
