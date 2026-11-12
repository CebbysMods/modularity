---
# modularity-v0it
title: 'S3: L3 in-game smoke reporting'
status: todo
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T14:33:37Z
parent: modularity-0i9j
---

Stage 3: every smoke run becomes one Allure test with the log, the summary and the client screenshot attached.

Child beans: check/writer plumbing, task output, one bean per platform, CI.

Design: `SmokeAllureWriter` writes `<uuid>-result.json` in `build/smoke/allure-results/<side>/`; `SmokeCheckTask` writes it before it throws, so a red run still leaves evidence.

Starts only after the owner approves S2.

Exit check (on request): one platform's server and client smoke results appear in the root report next to L1 and L2.
