---
# modularity-72ey
title: 'S3: SmokeCheck expectations and SmokeAllureWriter'
status: todo
type: task
priority: normal
tags:
    - build
created_at: 2026-11-10T14:33:37Z
updated_at: 2026-11-10T14:33:37Z
parent: modularity-0i9j
---

- `SmokeCheck.expectations(minecraft, side, platform, guests)` public; `check` calls it.
- New `SmokeAllureWriter` (`Run` record, `write`): one step per expectation, test status `passed`/`failed`/`broken`, labels `epic=Modularity`, `feature=L3 in-game smoke`, `story=<platform> <mc> <side>`, attachments for log, summary and screenshot, deterministic `historyId`, clears its directory first.
- `SmokeAllureWriterTest`: 8 cases (all passed, one missing line, wrong version, crash, attachments, file name and historyId, JSON escaping, second write).

Check: `./gradlew -p modularity-gradle test --tests '*SmokeAllureWriterTest'`.
