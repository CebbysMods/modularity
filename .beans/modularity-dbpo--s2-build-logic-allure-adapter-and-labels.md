---
# modularity-dbpo
title: 'S2: build-logic Allure adapter and labels'
status: todo
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T14:41:37Z
parent: modularity-0i9j
---

- `build-logic/build.gradle`: `id 'io.qameta.allure-adapter' version '4.3.0'`, `allure.adapter.allureJavaVersion = '3.1.1'`, `aspectjWeaver = false`.
- Labels: `LayersTest` PR-8; `ModularityVersionPluginTest` PR-6; `QuiltJarMetadataTest` PR-7; `TableAndMetadataTest` PR-1/PR-7; `UniversalJarTest` PR-10; `VersionsTest` and `SmokeCheckTest` stay `L1 unit tests`.

Check: `./gradlew -p build-logic test` writes `build-logic/build/allure-results`.



The constants class cannot be reused here (included build), so this stage keeps its own copy of the L2 values.
