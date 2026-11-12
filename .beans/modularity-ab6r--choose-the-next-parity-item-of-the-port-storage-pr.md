---
# modularity-ab6r
title: 'Choose the next parity item of the port: storage proofs, shipped-jar smoke or guests'
status: todo
type: task
priority: high
tags:
    - port
    - parity
    - decision
created_at: 2026-11-12T08:59:55Z
updated_at: 2026-11-12T08:59:55Z
blocking:
    - modularity-xbj0
    - modularity-ecjq
---

The port of the Fabric feature set to loom, quilt, anvil, neoforge and forge is code-complete and green on
all eight 21.1 smokes of 2026-11-12 10:52 (docs/07 §7.10). Three parity rows stay open, and each one needs
a decision before code is written. This bean records the choice; it does not decide it.

Recorded state: PORT-REVIEW.md, "Parity against Fabric" and "Unproven in every stage"; TODO-NOW.md,
"Feature parity against Fabric (risk)". The matrix there (Fabric 95, Quilt 91, NeoForge 90, Forge 81)
predates the fourth pass and its Forge number is stale.

## Option 1: storage proofs (recommended)

The only open task beans under the port epics are the four storage tasks: `modularity-dgzs` (loom),
`modularity-mizg` (quilt), `modularity-gz97` (neoforge), `modularity-tll2` (forge). Their remaining check
is the two seam methods that no run has called:

- `StorageBackend.fillFromHeldItem(Player, McFluidTank)` — give the smoke player a water bucket in the main
  hand, call the method, and require the log to show 1000 mB moved.
- `StorageBackend.exposeEntityStorages(EntityType, EntityDefinition)` — give the smoke entity a tank, then
  read it back through the loader's own lookup (Fabric `EntityApiLookup`, NeoForge entity capability,
  Forge capability on `AttachCapabilitiesEvent`).

Files: `modularity-smoke/minecraft/common/.../SmokeFeatures.java`, `SmokeScenario.java`,
`modularity-gradle/.../smoke/SmokeCheck.java` and `SmokeCheckTest.java`, the four `*StorageBackend.java` if the
run finds a defect.
Check: every client and server log holds the two new lines with the expected numbers.
Command: `./gradlew -Pmodularity.platforms=fabric-21.1,quilt-21.1,neoforge-21.1,forge-21.1
:platforms:21.1:21.1-fabric:smoke :platforms:21.1:21.1-quilt:smoke :platforms:21.1:21.1-neoforge:smoke
:platforms:21.1:21.1-forge:smoke`
Risk: `fillFromHeldItem` moves a real item on the server thread, so the scenario must restore the inventory;
the item lookup differs per loader (`ContainerItemContext.forPlayerInteraction` on Fabric, `FluidUtil` on the
Forge family).

## Option 2: shipped-jar smoke (epic `modularity-xbj0`)

No run starts a loader from the universal jar, so the artifact the project ships is unproven. The epic
needs a hand-rolled launcher (vanilla install from Mojang's manifest, loader installers, `inheritsFrom`
merge, natives, cached flat world, process management), the `smokeJarClient` / `smokeJar` / `smokeJars`
tasks and a CI job. It also owns the universal-jar mixin connector: one `MixinConfigs` attribute cannot
name a different config for four loader trees (bean text, "Mixin route for the universal jar").
Risk: the launcher downloads Minecraft and each loader at build time; the mixin connector cannot be proved
without the launcher itself.

## Option 3: guest mods (epic `modularity-ecjq`)

Only Fabric loads a guest jar. Quilt's route is a loader plugin that needs
`loader.experimental.allow_loading_plugins` before the metadata is parsed, and the Forge family has no
public API to add a mod after discovery (docs/08, evidence in `modularity-4bm8`). Children:
`modularity-73sm` (Quilt), `modularity-zbbp` and `modularity-4l2a` (the Forge family).
Check: the same guest jar loads on all four loaders, each smoke log holds `Loaded guest mod smoke_guest`
and `Guest mod smoke_guest: loaded=true initialized=true`.

## What is already proved

Eight 21.1 smokes green (server and client, four loaders); the mod lifecycle and entrypoints; detection;
content registration; networking in both directions; the fluid unit and the portable view; the client seam
(dynamic model, hit particles, HUD, key mapping, entity renderer); the Forge development mixin config
(seven `Mixing` lines); `-p modularity-gradle test` 59 tests; the four per-loader jars and the universal jar
(1,142,719 bytes). See docs/07 §7.10 and PORT-REVIEW.md.
