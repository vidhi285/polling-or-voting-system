# REST API

Base URL: `http://localhost:8080` (same origin as the frontend). All bodies are JSON (`UTF-8`).
Responses carry `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`.
Request bodies are limited to **16 KB** (larger -> `413`).

## Error format (every error)
```json
{ "error": "VALIDATION_ERROR", "message": "Question is required. At least two options are required.",
  "details": ["Question is required.", "At least two options are required."] }
```
`details` appears only for validation errors. Stack traces and credentials are never returned.

| HTTP | `error` code | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | invalid poll, voter id, or option (also option from another poll) |
| 400 | `MALFORMED_JSON` / `EMPTY_BODY` | body missing, not JSON, or wrong JSON shape |
| 404 | `NOT_FOUND` | unknown poll or unknown route |
| 405 | `METHOD_NOT_ALLOWED` | wrong HTTP method (an `Allow` header is sent) |
| 409 | `DUPLICATE_VOTE` | this voter already voted in this poll |
| 409 | `POLL_CLOSED` | voting on a closed poll |
| 409 | `POLL_ALREADY_CLOSED` | closing a poll that is already closed |
| 413 | `PAYLOAD_TOO_LARGE` | body over 16 KB |
| 500 | `INTERNAL_ERROR` | unexpected bug (details only in the server log) |
| 503 | `DATABASE_UNAVAILABLE` | MongoDB cannot be reached |

## Endpoints

### `GET /api/health` -> 200 or 503
Pings MongoDB on every call.
```json
{ "status": "UP", "application": "UP", "database": "UP", "time": "2026-10-20T10:00:00Z" }
```
Returns `503` with `"status":"DOWN","database":"DOWN"` if the ping fails.

### `GET /api/stats` -> 200
```json
{ "totalPolls": 3, "activePolls": 2, "closedPolls": 1, "totalVotes": 17 }
```
### `GET /api/polls` -> 200
Array of polls, newest first (`[]` when none exist).

### `POST /api/polls` -> 201
Request: `{ "question": "Best language?", "options": ["Java", "Python"] }`
Rules: question 1-200 chars after trimming; 2-10 options; each 1-100 chars; options unique (case-insensitive). Text is trimmed.
Response (also the shape of every poll):
```json
{ "id": "6b1f...", "question": "Best language?",
  "options": [ { "id": "opt_3fa91c0d22", "text": "Java" }, { "id": "opt_77be10a4c9", "text": "Python" } ],
  "status": "ACTIVE", "active": true, "createdAt": "2026-10-20T10:00:00Z" }
```
`closedAt` is added once the poll is closed.

### `GET /api/polls/{id}` -> 200 | 404
### `POST /api/polls/{id}/votes` -> 200 | 400 | 404 | 409
Request: `{ "voterId": "<browser-local id>", "optionId": "opt_3fa91c0d22" }`
Response: `{ "message": "Vote recorded.", "pollId": "...", "optionId": "...", "castAt": "..." }` (voter id is not echoed).

### `GET /api/polls/{id}/results` -> 200 | 404
```json
{ "pollId": "...", "question": "Best language?", "status": "ACTIVE", "totalVotes": 3,
  "options": [ { "optionId": "opt_3fa9...", "text": "Java", "votes": 2, "percentage": 66.7 },
               { "optionId": "opt_77be...", "text": "Python", "votes": 1, "percentage": 33.3 } ] }
```
Percentages: `votes / totalVotes * 100`, rounded to one decimal (HALF_UP); `0.0` when there are no votes. Rounded values may sum to 99.9 or 100.1.

### `POST /api/polls/{id}/close` -> 200 | 404 | 409
No body needed. Returns the poll with `"status":"CLOSED"`. Votes are kept; new votes get `409 POLL_CLOSED`.

## Static files
`GET /` serves `index.html`; `/css/*` and `/js/*` are served from the classpath. Only known file extensions are served; directory listings and `..` paths return 404.
