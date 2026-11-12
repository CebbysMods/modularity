---
# modularity-n44y
title: 'anvil: mod life cycle and the mod bus'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-arii
---

Files: new `anvil/entrypoint/**`.
Work: NeoForge constructs `@Mod` classes and Forge constructs one. Put the shared part (seams install, `SyncedData.init`, `DetectionLog.log`, `ModularityCommon.init`) in `anvil`, and let each loader class call it from its own event.
Check: the NeoForge and the Forge smoke log show the four stage lines.

## Audit 2026-11-11 (stage 3)
In the tree: `ModularityAnvilLifecycle` gives the stage order `detect`, `launch`, `launchMods`, `common`, `client`, `server`; each stage logs `Entrypoints ... active (<loader>)` and installs the seams. `ModularityAnvilSeams` holds `LoaderInfo`, `RenderDataAccess`, `StorageBackend` and `LoaderHooks` and installs them with `Loaders`. The NeoForge entrypoints call it; the Forge entrypoint calls `detect()` only until Forge has seam implementations (stage 5). No Gradle run: AGENTS.md hard rule 1. Verification stays open.

## State 2026-11-11 (stage 5)
Supersedes the last sentence of the audit above: `ModularityForge` builds `ModularityAnvilSeams` from `ForgeLoaderInfo`, `ForgeRenderData`, `ForgeStorageBackend` and `ForgeLoaderHooks` and calls `ModularityAnvilLifecycle.launch`, `launchMods` and `common` in the `@Mod` constructor; `ModularityForgeClient` and `ModularityForgeServer` call the client and the server stage. `javac --release 21` compiles the Forge tree (98 sources, exit 0). No Gradle run: AGENTS.md hard rule 1. Check: `:platforms:21.1:21.1-forge:smokeServer` and `smokeClient` show the four stage lines.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The NeoForge and Forge logs each hold the four stage lines.
