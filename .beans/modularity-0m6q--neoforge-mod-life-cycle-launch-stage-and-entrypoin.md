---
# modularity-0m6q
title: 'neoforge: mod life cycle, launch stage and entrypoints'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-yrjc
---

Files: `neoforge/entrypoint/ModularityNeoForge{Common,Client,Server}.java`.
Work: there is no launch stage; add one that runs `ModularityLaunch` before the game starts, and move the shared part into `anvil` when stage 3 exists.
Check: the smoke log holds the four stage lines, launch first.

## Audit 2026-11-11 (stage 4)
In the tree: `ModularityNeoForgeCommon` runs `ModularityAnvilLifecycle.detect()`, then the `launch`, `launchMods` and `common` stages from the mod constructor, because NeoForge closes its registries before common setup. `ModularityNeoForgeClient` wires `ModularityAnvilLifecycle.client` from `FMLClientSetupEvent`; `ModularityNeoForgeServer` wires `ModularityAnvilLifecycle.server` from `FMLDedicatedServerSetupEvent`. `javac` compiles the anvil and neoforge layers (exit 0). No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.



## Proven 2026-11-11 (run)
`:platforms:21.1:21.1-neoforge:smokeServer` and `smokeClient` pass. The server log holds `(server) platform neoforge:21.1.265, minecraft:1.21.1` and the 40-tick stop; the client log holds `(client) platform neoforge:21.1.265, minecraft:1.21.1`, `Client joined world`, the screenshot and `Client done`. So the constructor stage (detect, launch, launchMods, common) and the client and server stages all run. Note: the stage log lines of the anvil lifecycle are not in the smoke output; the bean's check phrase "the four stage lines, launch first" is not what the run shows. The smoke proves the stages run, not their wording.



Correction to the note above: the anvil stage lines ARE in the log. The run pass of 2026-11-11 shows, in order, `Entrypoint mod-constructor active (neoforge)`, `Entrypoint launch active (neoforge)`, `Entrypoint common active (neoforge)`, `(server) platform neoforge:21.1.265, minecraft:1.21.1`, `Entrypoint server active (neoforge)` (and the client variant). They were absent from `build/smoke/server.txt` only because `SmokeCheck` did not expect them. That expectation is now added (see below).

## Check strengthened 2026-11-11
`SmokeCheck` now expects the entrypoint stage lines for every loader: `launch` and `common` on Fabric and Quilt; `mod-constructor`, `launch` and `common` on NeoForge and Forge; then the platform line; then the side stage (`server` or `client`). The `SmokeCheckTest` has a missing-stage case and a Forge-family case, and all nine tests pass under `javac` + a plain JUnit-free runner (no Gradle run).

## Proved 2026-11-11 (re-run with the strengthened check)
The eight smokes of the final 2026-11-11 pass run with the strengthened `SmokeCheck` and all pass, so every real log holds the entrypoint stage lines in the expected form and order. The same pass runs with the shared layer byte-identical to `HEAD`.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The NeoForge log holds the launch, common and side stage lines.
