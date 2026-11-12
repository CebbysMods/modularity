---
# modularity-lm15
title: 'S1: four-level report tree, display names and test steps'
status: completed
type: task
priority: normal
tags:
    - api
    - testkit
created_at: 2026-11-10T15:34:19Z
updated_at: 2026-11-10T20:33:25Z
parent: modularity-0i9j
---

The report tree gains two levels, and every test gains a sentence and a step list.

Labels: epic `Modularity`; feature `L1 Unit Tests` (`L2 Integration Tests`, `L3 End to End Tests`, `L4 Production Jar`); story = the API concern; and a custom `functionality` label for the behaviour. `@Functionality` is a meta-annotation of Allure's `LabelAnnotation` in the test fixtures, and `allurerc.json` groups by that label name. A behaviour gets a `functionality` label only when two or more tests prove it; a behaviour with one test has no label and sits directly under its story.

Tests: every test has a `@DisplayName` sentence and calls `@Step` helper methods that do one action or one check. A step takes only text, numbers and values with a readable text form, because Allure writes every parameter into the report; objects live in fields of the test instance. The adapter sets `aspectjWeaver = true`, because `@Step` needs the weaver.

Files: `allurerc.json`; `gradle.properties` (`allure_java_version`); root `build.gradle`; `modularity-api/build.gradle` (`testFixturesCompileOnly`); `ModularityAllureConstants`; `Functionality`; the seven L1 test classes; `docs/07` 7.11; `TODO-NOW.md`.

Done: the sources compile with `javac -Xlint:all` (fixtures, api test, testkit test), before and after the regroup.

Run of 2026-11-10 17:34: `:modularity-api:test` and `:modularity-testkit:test` passed (50 tests, 0 failures). The raw results hold 176 steps and the four labels, so the AspectJ weaver works with `@Step`. `allureAggregateReport` failed with `IOException: Unable to delete directory`, because the HTTP server that served the old report held the directory; the HTTP server is stopped.

Report of that run, written by the bundled Allure CLI: it showed two defects. Two stories owned the version tests (`Core API` and `Version API`), and most behaviour groups held one test. Step bodies showed object identity strings, for example `DataCodec$1@308a9264`.

Regroup: `Version Model API` replaces `Version API` and takes the version tests of `CoreApiTest`; a behaviour keeps its label only for two or more tests; the step helpers take no objects.

Run of 2026-11-10 after the regroup: `./gradlew -Pmodularity.platforms=none :modularity-api:test :modularity-testkit:test` passed (50 tests, 0 failures), then `./gradlew -Pmodularity.platforms=none allureAggregateReport` wrote the report (BUILD SUCCESSFUL). The tree is Modularity > L1 Unit Tests > API concern > behaviour > test with 18 groups, and no group holds one test. No step parameter shows an object identity string. The report is served on http://127.0.0.1:8123/index.html.
