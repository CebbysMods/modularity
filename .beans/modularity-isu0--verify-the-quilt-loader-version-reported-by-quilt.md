---
# modularity-isu0
title: Verify the Quilt loader version reported by quilt_loader
status: todo
type: task
priority: high
tags:
    - verification
created_at: 2026-11-10T10:42:35Z
updated_at: 2026-11-10T10:42:35Z
parent: modularity-km3b
---

Check that the `quilt_loader` mod container reports the version in `gradle/platforms.toml`
(`quilt-loader = "0.30.2"`). TODO-NOW.md open check: Quilt loader version.

Evidence in the tree: `LoomPlatformSource.detect` takes the version from the container
`quilt_loader`; `ModMetadata` writes `quilt_loader >=0.30.2` into `quilt.mod.json`.

Run (only when the owner asks):
`./gradlew -Pmodularity.platforms=quilt-21.1 :platforms:21.1:21.1-quilt:smokeServer`

Read: `platforms/quilt/versions/21.1/build/smoke/run/logs/latest.log`.

Pass: `[modularity] platform QUILT 0.30.2, Minecraft 1.21.1, SERVER`.

The smoke check alone does not prove this: `SmokeCheck` accepts any version string. Read the log line.
