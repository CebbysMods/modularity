---
# modularity-ff4c
title: 'S2: Included-build bridge into the root report'
status: completed
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T21:04:18Z
parent: modularity-0i9j
---

`allureCollectResults` copies `build-logic/build/allure-results` and depends on `gradle.includedBuild('build-logic').task(':test')`, because the aggregate plugin cannot cross an included build.

Check: the root report lists the L2 classes after `-p build-logic test`.



In the working tree: ModularityRootPlugin.registerAllureCollectResults copies build-logic/build/allure-results and depends on root.getGradle().includedBuild('build-logic').task(':test'), behind the same -Pmodularity.allure.tests=false gate. Checked with javac only.

Run of 2026-11-10: `./gradlew -Pmodularity.platforms=none allureAggregateReport` passed and wrote one report of 96 tests, `L1 Unit Tests` 65 and `L2 Integration Tests` 31. The copy step of `allureCollectResults` and its dependency on `includedBuild('build-logic').task(':test')` both work.

2026-11-10: `allurerc.json` sets `defaultSortBy` to `name,asc`, so every level of the tree has one order on every run. Allure's default `order` follows the order in which the report reads the result files.
