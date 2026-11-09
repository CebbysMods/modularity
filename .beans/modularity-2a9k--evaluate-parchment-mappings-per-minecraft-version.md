---
# modularity-2a9k
title: Parchment mappings per Minecraft version
status: completed
type: feature
priority: high
tags:
    - build
    - tooling
created_at: 2026-11-08T12:03:40Z
updated_at: 2026-11-09T11:45:00Z
---

Minecraft is mapped with official Mojang mappings; Parchment adds parameter names and javadoc on top. It is
compile-time metadata: class and member names, the remapped output and runtime behaviour do not change, so
it does not affect the universal jar or its relocation.

## Availability (maven.parchmentmc.org, checked 2026-11-08)
Artifacts exist for 1.21.1, 1.21.3-1.21.11 (no 1.21.2 artifact; 1.21.3 covers that line) and none for
26.x. 1.21.1 release: 2024.11.17.

## Done
- [x] `[parchment]` section in gradle/platforms.toml: Minecraft version -> Parchment version, only where a
      release exists (an absent entry means official-only mappings for that version); a key without a platform
      of that version is a table error. `1.21.1 = "2024.11.17"` added.
- [x] `PlatformTable.Platform` carries the Parchment version (`null` = official only) and the Loom artifact
      notation (`parchmentArtifact()`).
- [x] Fabric/Quilt: `loom.layered { officialMojangMappings(); parchment(<artifact>) }`
- [x] NeoForge (ModDevGradle): `neoForge.parchment.minecraftVersion` / `mappingsVersion`
- [x] Forge (ForgeGradle 7): `minecraft.mappings("parchment", "<parchment>-<minecraft>")`
- [x] parchmentmc Maven (`https://maven.parchmentmc.org`, content-filtered to `org.parchmentmc.data`) added
      once per platform by `ModularityPlatformPlugin.parchmentRepository`: neither Loom, ModDevGradle nor
      ForgeGradle adds it on its own.
- [x] Parser test (`TableAndMetadataTest.parchmentIsPerMinecraftVersionAndOptional`); docs/07 7.4 and the
      decisions table.
- [x] Verify: `:platforms:21.1:21.1-{fabric,quilt,neoforge,forge}:compileJava` and `:platforms:21.1:build`
      succeed with the Parchment artifact on the mappings; Forge recompiled with it (`--rerun`).

## Remaining
- [ ] Byte-compare the universal jar against one built without Parchment (expected identical: metadata only).
- [ ] Readable parameter names in the IDE once `genSources` has run for the version.

## Acceptance
- [x] Every version in the table without a Parchment release builds with official mappings alone (code path
      covered by the parser test; no such version is enabled today)
- [ ] Parameter names in Minecraft classes are readable in the IDE on every 21.1 platform
- [ ] The universal jar is byte-identical to the one built without Parchment (metadata only)

## Revision (2026-11-09): Forge included too

An earlier revision excluded Forge: with both the official and the Parchment variants published for
`net.minecraftforge:forge`, `smokeServer`/`smokeClient` failed to resolve the run class path ("Cannot
choose between the available variants", through the attribute-less detached configuration that
ForgeGradle's `MinecraftDependencyImpl.init` creates for `minecraft_classpath`, which no project
configuration hook can reach).

Fix: the mappings are now also set as attributes (`net.minecraftforge.mappings.channel` /
`.version`) on the Minecraft dependency itself, through the closure overload of
`minecraft.dependency(...)`. The detached configuration resolves from the dependency's own requested
attributes, so the run class path picks the Parchment variant. The Action overload cannot be used: it
casts the created `ExternalModuleDependency` to `ClosureOwner.MinecraftDependency`, which it is not.

Verified 2026-11-09: `:platforms:21.1:21.1-forge:smokeClient --rerun-tasks` (mavenizer cache deleted)
resolves `net.minecraft:mappings_parchment:1.21.1-…-2024.11.17-1.21.1` on the compile class path and
the client starts, joins the world and finishes the smoke. All four loaders now use Parchment where
`[parchment]` has a version. See `ForgePlatformSetup` (was `ForgeSetup` before the setup refactor),
`AbstractLoomPlatformSetup`, gradle/platforms.toml and docs/07-multi-version-build.md.
