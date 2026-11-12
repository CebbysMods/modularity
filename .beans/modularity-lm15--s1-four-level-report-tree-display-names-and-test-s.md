---
# modularity-lm15
title: 'S1: four-level report tree, display names and test steps'
status: in-progress
type: task
priority: normal
tags:
    - api
    - testkit
created_at: 2026-11-10T15:34:19Z
updated_at: 2026-11-10T15:34:44Z
parent: modularity-0i9j
---

The report tree gains two levels, and every test gains a sentence and a step list.

Labels: epic `Modularity`; feature `L1 Unit Tests` (`L2 Integration Tests`, `L3 End to End Tests`, `L4 Production Jar`); story = the API area; and a custom `functionality` label for the behaviour. `@Functionality` is a meta-annotation of Allure's `LabelAnnotation` in the test fixtures, and `allurerc.json` groups by that label name.

Tests: every test has a `@DisplayName` sentence and calls `@Step` helper methods that do one action or one check. The adapter sets `aspectjWeaver = true`, because `@Step` needs the weaver.

Files: `allurerc.json`; `gradle.properties` (`allure_java_version`); root `build.gradle`; `modularity-api/build.gradle` (`testFixturesCompileOnly`); `ModularityAllureConstants`; `Functionality`; the seven L1 test classes; `docs/07` 7.11; `TODO-NOW.md`.

Done: the sources compile with `javac -Xlint:all` (fixtures, api test, testkit test).

Run of 2026-11-10 17:34: `:modularity-api:test` and `:modularity-testkit:test` passed (50 tests, 0 failures). The raw results hold 176 steps and the four labels, so the AspectJ weaver works with `@Step`. `allureAggregateReport` failed with `IOException: Unable to delete directory`, because the HTTP server that served the old report held the directory; the HTTP server is stopped.

Check (open): run `./gradlew -Pmodularity.platforms=none :modularity-api:test :modularity-testkit:test`, then `./gradlew -Pmodularity.platforms=none allureAggregateReport`. The tree must show Modularity > L1 Unit Tests > API area > behaviour > test, and each test must show its steps. Open the report through an HTTP server.
