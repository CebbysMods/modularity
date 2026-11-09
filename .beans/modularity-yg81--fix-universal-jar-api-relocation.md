---
# modularity-yg81
title: Fix universal jar API relocation
status: completed
type: bug
priority: critical
tags:
    - build
    - jar
created_at: 2026-11-09T07:58:32Z
updated_at: 2026-11-09T08:16:57Z
parent: modularity-xbj0
---

Found while designing the production-jar smoke. `UniversalJar.relocate` remaps every internal name under
`lv/cebbys/mcmods/modularity`, including `.../api/*`, but the nested `modularity-api` jar is copied unrelocated.

Verified on the current `build/libs-all/modularity-1.21.1-0.1.0-SNAPSHOT.jar`:
- relocated `.../universal/fabric/platform/ModularityCore.class` references
  `lv/cebbys/mcmods/modularity/universal/fabric/api/Modularity` and `.../universal/fabric/api/core/McVersion`;
- the jar contains zero classes under `.../universal/<loader>/api/`;
- the nested `META-INF/jars/modularity-api-...jar` has the real `lv/cebbys/mcmods/modularity/api/*` names.

The first production client therefore fails with `NoClassDefFoundError`. This blocks the smoke epic.

## Work
- `UniversalJar.relocate` (`build-logic/src/main/java/lv/cebbys/modularity/gradle/UniversalJar.java`): do not
  remap internal names under `ROOT + "/api/"`; keep remapping `platform`, `fabric`, `forge`, `neoforge`.
- Any `ROOT + "/api/"` entries that appear in a platform jar must likewise stay unrelocated.
- Add `UniversalJarTest`: a relocated platform class references `lv/cebbys/mcmods/modularity/api/...` (never
  `.../universal/<loader>/api/...`) and no `universal/*/api` class is emitted.

## Acceptance
- `javap -c` on the rebuilt relocated `ModularityCore` shows only real `api/*` references.
- `./gradlew -p build-logic test` is green.
- The rebuilt universal jar contains no `lv/cebbys/mcmods/modularity/universal/*/api/` entries and its nested API
  jar is unchanged.
