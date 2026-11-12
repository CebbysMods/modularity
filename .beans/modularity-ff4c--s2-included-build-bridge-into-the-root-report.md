---
# modularity-ff4c
title: 'S2: Included-build bridge into the root report'
status: todo
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T14:33:37Z
parent: modularity-0i9j
---

`allureCollectResults` copies `build-logic/build/allure-results` and depends on `gradle.includedBuild('build-logic').task(':test')`, because the aggregate plugin cannot cross an included build.

Check: the root report lists the L2 classes after `-p build-logic test`.
