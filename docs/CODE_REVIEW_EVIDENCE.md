# Code Review / Pair-Programming Evidence

No code review or pair-programming session has taken place, and none is claimed here. This file explains how to create **real** evidence when working alone.

## Options (ask your professor which are acceptable)
1. **Peer review**: ask a classmate to review a GitHub pull request. Real comments from them are the evidence (screenshot or PR link).
2. **Professor/mentor review** of a pull request or during the sprint review.
3. **Self-review via pull requests**: open a PR for each feature branch, review your own diff against the checklist below a few hours later, and record what you changed. Label it clearly as *self-review*.
4. **Tool-assisted review**: use static analysis or an AI assistant on a PR; keep the output, label it as tool-generated, and record which suggestions you accepted or rejected and why.
5. **Remote pair session** with a classmate (screen share): note date, duration, who drove, what was found.
An AI-assisted generation of this project is itself something you should disclose in your report; it is not a code review.

## Self-review checklist
- [ ] Each class has one clear responsibility; services depend on interfaces
- [ ] Every request is validated on the backend, not only in the browser
- [ ] No secrets in code; `.env` is git-ignored
- [ ] Errors never expose stack traces
- [ ] Tests exist for the behaviour changed in this PR and pass
- [ ] Names are clear; no dead code or TODOs

## Review log (fill after real reviews)
| Date | PR / commit | Reviewer (name or "self") | Findings | Action taken | Evidence link |
|---|---|---|---|---|---|
| | | | | | |
