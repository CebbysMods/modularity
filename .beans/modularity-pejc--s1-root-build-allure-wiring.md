---
# modularity-pejc
title: 'S1: Root build Allure wiring'
status: completed
type: task
priority: high
tags:
    - build
created_at: 2026-11-10T14:33:29Z
updated_at: 2026-11-10T15:16:31Z
parent: modularity-0i9j
---

- `build.gradle` (root): `io.qameta.allure-adapter` 4.3.0 `apply false`, `io.qameta.allure-aggregate-report` 4.3.0.
- `configure([...])` applies the adapter to api and testkit with `allure.adapter.allureJavaVersion = '3.1.1'`, `aspectjWeaver = false`.
- `allureReport` and `allureGenerateCategories` lose their generated project dependencies; `allureAggregateReport` reads `build/allure-results`.
- `ModularityRootPlugin` registers `allureCollectResults` (Sync), gated by `-Pmodularity.allure.tests=false`.
- `.github/workflows/build.yml`: `report` job, artifact `allure-report-build`, fails last.

Check: `./gradlew help` resolves the plugins; then the S1 report command.

- Done 2026-11-10: `allureAggregateReport` wrote the report from `build/allure-results`. The task always runs: the plugin does not declare the results as an input, so Gradle kept an old report. The `test` tasks clear their results first, because the adapter adds to the directory.

- Report layout 2026-11-10: `allurerc.json` (root) sets the tree grouping to epic, feature and story; without it the tree follows the Java package path. The task runs with `clean = true`, because the Allure 3 CLI writes nothing when the output directory already exists and still exits with 0.
