---
# modularity-efvq
title: 'S1: L1 unit-test reporting'
status: completed
type: task
priority: high
tags:
    - build
created_at: 2026-11-10T14:33:29Z
updated_at: 2026-11-10T15:07:51Z
parent: modularity-0i9j
---

Stage 1 of the Allure test pyramid: the two library modules write results and the root build writes one report.

Child beans: root wiring, modularity-api, modularity-testkit.

Ran on 2026-11-10; see the last line of this bean.

Exit check (on request): `./gradlew -Pmodularity.platforms=none allureAggregateReport` shows the L1 tests of both modules.

- Done 2026-11-10. Exit check passed: `./gradlew -Pmodularity.platforms=none allureAggregateReport` wrote 50 tests (28 + 22), all passed, with a link to pull request 4. Report: `build/reports/allure-report/allureAggregateReport/index.html`.
