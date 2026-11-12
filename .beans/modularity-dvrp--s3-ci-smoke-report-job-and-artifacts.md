---
# modularity-dvrp
title: 'S3: CI smoke report job and artifacts'
status: todo
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T14:33:37Z
parent: modularity-0i9j
---

- Each `smoke.yml` matrix job uploads `build/smoke/allure-results/` as `smoke-allure-<id>`.
- `merge` downloads them, runs `-Pmodularity.allure.tests=false -Pmodularity.allure.smokeResults=smoke-allure-results allureAggregateReport`, uploads `smoke-allure-report`, and fails last.

Check: a branch push publishes both artifacts, including a run with a failed smoke.
