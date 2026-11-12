---
# modularity-xndk
title: 'S3: SmokeCheckTask and SmokeSetup result output'
status: todo
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T14:33:37Z
parent: modularity-0i9j
---

- `SmokeCheckTask`: `@OutputDirectory resultsDir`, `@Optional @Internal screenshot`; one `failureText` for the writer and the exception; write before the throw in both branches.
- `SmokeSetup`: results dir `build/smoke/allure-results/{server,client}`; client screenshot `<run dir>/screenshots/smoke.png`.
- `ModularityRootPlugin`: collect `platforms/**/build/smoke/allure-results/**` and `-Pmodularity.allure.smokeResults`, `mustRunAfter` the smoke tasks, never depend on them.

Check: one `smokeServer` run leaves a result file.
