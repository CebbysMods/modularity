---
# modularity-4uol
title: 'Forge family: a Modularity mod cannot register after Modularity''s constructor'
status: completed
type: bug
priority: high
tags:
    - forge-family
    - registration
created_at: 2026-11-11T07:05:18Z
updated_at: 2026-11-11T23:04:11Z
blocking:
    - modularity-d5hk
    - modularity-dti9
---

A Modularity mod cannot register on NeoForge or Forge. `ModularityNeoForgeCommon` and `ModularityForge` run
the common stage inside Modularity's own mod constructor (`ModularityAnvilLifecycle.common` ->
`ModularityCommon.init`). That call does two things at once: it freezes `ModularModRegistry`, and it runs the
common stage of the mods that are registered at that moment.

By then no other mod has been constructed, so the list is empty. A dependent mod that calls
`Modularity.registerMod` in its own constructor gets an `IllegalStateException` from the frozen registry. A late
registration would also miss the common stage, because `init` never runs twice.

## Effect

- On the Forge family the API cannot be used by a mod jar. Only Modularity itself is initialized.
- `SmokeFeatures` proves block, item and channel registration on Fabric and Quilt only. The corresponding smoke
  lines are not expected on NeoForge and Forge (`SmokeCheck.expectations`).
- The port cannot reach parity for content registration, networking, storage or client features on the Forge
  family until this is fixed.

## Where the code is

- `platforms/common/common/.../platform/ModularityCommon.java` - `init` freezes the registry, then runs each entry.
- `modularity-api/.../api/ModularModRegistry.java` - `freeze()` rejects every later `register`.
- `platforms/anvil/.../ModularityAnvilLifecycle.java` - `common` calls `DetectionLog.log()` then `ModularityCommon.init`.
- `platforms/neoforge/.../entrypoint/ModularityNeoForgeCommon.java` and
  `platforms/forge/.../ModularityForge.java` - both call `ModularityAnvilLifecycle.common` from the `@Mod` constructor,
  because their registries close before common setup and the common stage is what queues the registrations.

## Options, not chosen

1. **Split freeze from run.** `ModularityCommon` gets `freeze()` and `run()`; the Forge family keeps its seams,
   synced data and detection line in the mod constructor, and runs the mods' common stage on a loader hook that
   fires after every mod constructor and before the first `RegisterEvent`. That hook is not identified yet; it
   must be proved on both loaders before the change.
2. **Late registration window.** `ModularModRegistry` accepts a registration after `freeze()` until the first
   stage reads the entries, and the Forge family runs the common stage on the same hook as option 1.

Both options change `platforms/common` or `modularity-api`, so they are outside the port's scope (the port adds
only loader-side code). The owner decides.

## Check

A NeoForge and a Forge smoke run log `Smoke features: declared ...` and `Smoke features: resolved server ...
present=true`, and `SmokeCheck` expects those lines on all four loaders.


## Fix in the tree, 2026-11-11 (unproven)

The loader-side route was found by bytecode inspection, so no shared file changed. Both loaders post a mod-bus
event after every mod constructor and before the first `RegisterEvent`:

- NeoForge `RegistryManager.postNewRegistryEvent` runs before `GameData.postRegisterEvents` and `freezeData`
  (`CommonModLoader.begin`).
- Forge runs the states `CREATE_REGISTRIES` (which posts `NewRegistryEvent`), `OBJECT_HOLDERS`,
  `INJECT_CAPABILITIES`, `UNFREEZE_DATA`, then `LOAD_REGISTRIES` (`ForgeStatesProvider`).

`InterModEnqueueEvent` is too late on both loaders: NeoForge fires it in `CommonModLoader.finish`, after
registration and common setup; Forge lists it after `SIDED_SETUP`.

Change:

- `ModularityNeoForgeCommon` and `ModularityForge` keep the detection and the launch stage in the constructor and
  run `ModularityAnvilLifecycle.launchMods` and `ModularityAnvilLifecycle.common` from a `NewRegistryEvent` listener.
- The NeoForge and Forge smoke mods call `Modularity.registerMod` in their constructors.
- `SmokeCheck` expects the two `Smoke features:` lines on every loader; `SmokeCheckTest` gained
  `forgeAlsoNeedsFeatureLines`, and its eleven tests pass under a plain runner.

Proof so far: `javac --release 21` compiles `ModularityNeoForgeCommon`, the NeoForge `SmokeMod`, `ModularityForge`
and the Forge `SmokeMod` against their loaders artifacts, and `SmokeFeatures` against the API.

Not proved: no run shows the event order, the flush of the queued registrations after it, or that the NeoForge and
Forge smokes still pass. The closing command is `./gradlew -Pmodularity.platforms=neoforge-21.1,forge-21.1
:platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke`, and it needs the owner word.



## The run of 2026-11-12 disproved the first fix

The owner authorized the full pass. `-p build-logic test` passed (50 tests). Of the eight 21.1 smokes,
Fabric and Quilt passed and logged both `Smoke features:` lines, including `resolved client block=modularity_smoke:feature_block present=true`.
NeoForge and Forge failed the check: `missing log line /Smoke features: declared .../`.

Cause, from the NeoForge server log: `java.lang.IllegalStateException: Registry is already frozen` at
`MappedRegistry.validateWrite` <- `MappedRegistry.createIntrusiveHolder` <- `Block.<init>` <-
`ModularBlock.<init>` <- `SmokeFeatures.initialize`, inside `ModularityAnvilLifecycle.common` inside the
`NewRegistryEvent` listener. NeoForge `CommonModLoader` shows the order: `RegistryManager.postNewRegistryEvent()`,
then `GameData.unfreezeData()`, then `GameData.postRegisterEvents()`. So `NewRegistryEvent` is posted
while every vanilla registry is still frozen: it is for new registries, not for vanilla content. Forge
failed the same way.

## Second fix, in the tree

`ModularityNeoForgeCommon` and `ModularityForge` run `launchMods` and `common` once, on the first
`RegisterEvent`, guarded by a `boolean` field. After the common stage the entrypoint calls
`replay(event)` on `NeoForgeLoaderHooks` / `ForgeLoaderHooks` (now public), because that hook's own
listener has already run for the registry that the event covers. The queue then covers every later
registry. A dependent mod can still register in its constructor, because `RegisterEvent` is posted after
all constructors.

Recorded in docs/07 7.6, 7.8 (three rows) and 7.10, TODO-NOW (the risk and Stage 4/5) and PORT-REVIEW.

Closing command: the eight 21.1 smokes,
`./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1 :platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke :platforms:21.1:21.1-neoforge:smoke :platforms:21.1:21.1-forge:smoke`.
