---
# modularity-isu0
title: Verify the Quilt loader version reported by quilt_loader
status: completed
type: task
priority: high
tags:
    - verification
created_at: 2026-11-10T10:42:35Z
updated_at: 2026-11-10T10:44:06Z
parent: modularity-km3b
---

Check that the `quilt_loader` mod container reports the version in `gradle/platforms.toml`
(`quilt-loader = "0.30.2"`). TODO-NOW.md open check: Quilt loader version.

Evidence in the tree: `ModularityLoomPlatformSource.detect` takes the version from the container
`quilt_loader`; `ModMetadata` writes `quilt_loader >=0.30.2` into `quilt.mod.json`.

Run (only when the owner asks):
`./gradlew -Pmodularity.platforms=quilt-21.1 :platforms:21.1:21.1-quilt:smokeServer`

Read: `platforms/quilt/versions/21.1/build/smoke/run/logs/latest.log`.

Pass: `[modularity] platform QUILT 0.30.2, Minecraft 1.21.1, SERVER`.

The smoke check alone does not prove this: `SmokeCheck` accepts any version string. Read the log line.


## Result (2026-11-10)

`./gradlew -Pmodularity.platforms=quilt-21.1 :platforms:21.1:21.1-quilt:smokeServer` passed in 16 s.
`platforms/quilt/versions/21.1/build/smoke/run/logs/latest.log` line 72:

`[modularity] platform QUILT 0.30.2, Minecraft 1.21.1, SERVER`

The version equals `quilt-loader` in `gradle/platforms.toml`, with no suffix. The three Quilt
entrypoints ran (`launch`, `common`, `server`), and the guest mod stayed off. The run is recorded in
docs/07 §7.10. The platform jars in `build/libs` are from 2026-11-09 and still name `mvd` and `mpd`;
they were not rebuilt by this task and are not evidence of the current metadata.
