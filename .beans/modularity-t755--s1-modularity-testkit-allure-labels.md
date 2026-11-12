---
# modularity-t755
title: 'S1: modularity-testkit Allure labels'
status: completed
type: task
priority: normal
tags:
    - api
created_at: 2026-11-10T14:33:29Z
updated_at: 2026-11-10T15:07:49Z
parent: modularity-0i9j
---

Adapter applied from the root. Labels on `CoreApiTest`, `EntityApiTest`, `LifecycleTest`, `MultipartApiTest`:
`@Epic("Modularity")`, `@Feature("L1 unit tests")`, `@Story("Testkit core" | "entities" | "lifecycle" | "multipart")`,
`@Issue("PR-9")`, `@Tag("PR-9")`.

Check: `./gradlew -Pmodularity.platforms=none :modularity-testkit:test`.

- Done 2026-11-10: 22 tests, all passed, labels `epic=Modularity`, `feature=L1 unit tests`, `story` per class, `tag=PR-4`, issue link to pull request 4 through `src/test/resources/allure.properties`.
