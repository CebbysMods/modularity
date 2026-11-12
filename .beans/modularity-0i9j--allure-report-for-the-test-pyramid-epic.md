---
# modularity-0i9j
title: Allure report for the test pyramid (epic)
status: in-progress
type: epic
priority: high
tags:
    - docs
    - build
created_at: 2026-11-10T14:33:15Z
updated_at: 2026-11-10T14:33:15Z
---

One Allure report per workflow shows the test pyramid: L1 unit, L2 build contract, L3 in-game smoke.

Plan: C:\Users\valen\.claude-ollama\plans\serene-puzzling-otter.md

Stages (child beans):
1. S1 L1 unit tests: root wiring, modularity-api, modularity-testkit
2. S2 L2 build contract: modularity-gradle adapter, included-build bridge
3. S3 L3 in-game smoke: writer and task plumbing, one bean per platform, CI
4. S4 L4 production jar: blocked by modularity-xbj0

Acceptance:
- One report task, `allureAggregateReport`, shows L1, L2 and L3 together.
- Every test carries its layer as a label; every smoke run is one test with the log, summary and screenshot attached.
- CI publishes the report as `allure-report-build` and `smoke-allure-report`.

A stage stops for the owner's word before the next starts. Risks are tracked in TODO-NOW.md.
