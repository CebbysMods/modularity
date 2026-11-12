---
# modularity-dbpo
title: 'S2: modularity-gradle Allure adapter and labels'
status: completed
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T21:04:18Z
parent: modularity-0i9j
---

- `modularity-gradle/build.gradle`: `id 'io.qameta.allure-adapter' version '4.3.0'`, `allure.adapter.allureJavaVersion = '3.1.1'`, `aspectjWeaver = false`.
- Labels: `LayersTest` PR-8; `ModularityVersionPluginTest` PR-6; `QuiltJarMetadataTest` PR-7; `TableAndMetadataTest` PR-1/PR-7; `UniversalJarTest` PR-10; `VersionsTest` and `SmokeCheckTest` stay `L1 unit tests`.

Check: `./gradlew -p modularity-gradle test` writes `modularity-gradle/build/allure-results`.



The constants class cannot be reused here (included build), so this stage keeps its own copy of the L2 values.



In the working tree: modularity-gradle/build.gradle applies io.qameta.allure-adapter 4.3.0 with allure_java_version from modularity-gradle/gradle.properties (3.1.1) and aspectjWeaver = false; the test task clears its allure-results first. New label class lv.cebbys.modularity.gradle.allure.BuildAllureLabels and a Functionality meta-annotation of LabelAnnotation. The 46 tests carry the L2 labels (Layer Assembly, Platform Table and Metadata, Universal Jar, Version Plugin Tasks, Quilt Jar Metadata) and a @DisplayName sentence; VersionsTest and SmokeCheckTest are L1 (Version Ranges, Smoke Log Checks). Checked with javac only.

Run of 2026-11-10: `./gradlew -p modularity-gradle test` passed with 46 tests and wrote `modularity-gradle/build/allure-results`. The labels give feature `L2 Integration Tests` for the five build-contract classes and `L1 Unit Tests` for `VersionsTest` and `SmokeCheckTest`; no group holds one test.

2026-11-10: the feature values lost the layer number. `L1 Unit Tests` is now `Unit Tests` and `L2 Integration Tests` is now `Integration Tests`; the constants are `Features.UNIT` and `Features.INTEGRATION` in both label classes.
