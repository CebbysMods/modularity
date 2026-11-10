---
# modularity-f5e2
title: Verify the universal jar holds no META-INF/services entry
status: todo
type: task
tags:
    - verification
created_at: 2026-11-10T10:42:35Z
updated_at: 2026-11-10T10:42:35Z
parent: modularity-km3b
---

Check that the universal jar holds no `META-INF/services/lv.cebbys.mcmods.*` entry. TODO-NOW.md open
check: No service files.

Evidence in the tree: no service file outside `build/`, no `ServiceLoader` in the sources. Only the
built jar can prove it, because `UniversalJar` merges the resources of four platform jars first-wins.

Run (only when the owner asks):
`./gradlew -Pmodularity.platforms=21.1 :platforms:21.1:jar`

Jar: `platforms/versions/21.1/build/libs/modularity-1.21.1-0.1.0-SNAPSHOT.jar`.

Pass: `unzip -l <jar> | grep -i 'META-INF/services'` prints no line. Run this alone; the task builds
all four platform jars, so a failure in any of them stops the command.
