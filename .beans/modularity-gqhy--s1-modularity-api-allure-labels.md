---
# modularity-gqhy
title: 'S1: modularity-api Allure labels'
status: completed
type: task
priority: normal
tags:
    - api
created_at: 2026-11-10T14:33:29Z
updated_at: 2026-11-10T15:07:48Z
parent: modularity-0i9j
---

Adapter applied from the root. Labels on `VersionTest`, `PlatformDetectorTest`, `VersionDetectorTest`:
`@Epic("Modularity")`, `@Feature("L1 unit tests")`, `@Story("API version" | "Runtime detection")`,
`@Issue("PR-9")`, `@Tag("PR-9")`. The fake loader classes under `src/test/java/net|org` stay unannotated.

Check: `./gradlew -Pmodularity.platforms=none :modularity-api:test`.



The label values live in `ModularityAllureConstants` (`modularity-api` test fixtures).

- Done 2026-11-10: 28 tests, all passed, labels `epic=Modularity`, `feature=L1 unit tests`, `story` per class, `tag=PR-4`, issue link to pull request 4 through `src/test/resources/allure.properties`.
