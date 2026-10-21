# Final Report Template - Online Polling and Voting System (CSE706)

Replace every `[ ... ]` with your own, true content. Delete sections you cannot support with evidence.

1. **Title page** - project title, name, enrollment details, course CSE706, date.
2. **Product vision and goal** (PVS-1) - [one paragraph]. Suggested starting point: let people create simple polls, vote once each, and see live results.
3. **User stories and acceptance criteria** (PVS-2, PVS-16..22) - from docs/JIRA_MAPPING.md.
4. **Product backlog and prioritisation** (PVS-3) - [Jira screenshot].
5. **Sprint planning** - Sprint 1 (17-22 Oct 2026) and Sprint 2 (23-30 Oct 2026): [goals, committed items].
6. **Architecture and design** (PVS-4, PVS-5) - docs/ARCHITECTURE.md; explain plain Java HttpServer instead of Spring Boot and why.
7. **Implementation** - MongoDB integration (PVS-6), poll creation (PVS-7), voting (PVS-9), results (PVS-11), closing (PVS-22).
8. **Test-Driven Development** (PVS-8, PVS-10) - docs/TDD_EVIDENCE.md. State exactly how the Red/Green evidence was produced.
9. **Functional testing** (PVS-12) - docs/FUNCTIONAL_TESTING.md plus **your own** Newman run against MongoDB: [paste output].
10. **Continuous integration** (PVS-13) - [link to a real GitHub Actions run, screenshot].
11. **Scrum events** (PVS-14) - Daily Scrum logs, reviews, retrospectives: [only what really happened].
12. **Metrics** (PVS-15) - velocity, burndown/burnup from Jira: [real numbers or "not recorded"].
13. **Risks** - docs/RISKS_AND_MITIGATION.md [update with what actually occurred].
14. **Code review / pairing** - docs/CODE_REVIEW_EVIDENCE.md [real activities only].
15. **Limitations** - browser-local voter IDs, any user can close a poll, no HTTPS/auth, Maven build and MongoDB integration tests status at submission time.
16. **Use of AI assistance** - disclose that the code and documents were generated with an AI assistant in a single session and what you changed/verified yourself.
17. **Conclusion and future work** - [authentication, creator-only closing, pagination, rate limiting].
18. **Appendix** - API.md, test output, screenshots.
