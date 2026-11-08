---
# modularity-vgs5
title: Ship one universal jar per Minecraft version
status: in-progress
type: feature
priority: high
tags:
    - build
    - architecture
    - jar
created_at: 2026-11-08T19:47:53Z
updated_at: 2026-11-08T19:47:53Z
---

Goal: `:platforms:<version>:build` produces one artifact, `modularity-1.21.1-<modversion>.jar`, loadable by Fabric,
Quilt, Forge and NeoForge. No per-loader artifacts in the parent; each loader keeps its own build as an intermediate.

## Why not simply merge today's jars
Fabric/Quilt are remapped to intermediary at build time; Forge/NeoForge run with Mojang official names. One compiled
class set cannot do both, so the universal jar carries one class set per mapping family, relocated into its own
package (`lv.cebbys.mcmods.modularity.universal.<loader>...`), with one metadata file per loader. Parchment (bean
2a9k) is compile-time only and does not change this.

## Shape
- fabric.mod.json        -> relocated fabric set (intermediary); Quilt loads it through Fabric compatibility
- META-INF/mods.toml     -> relocated forge set (official)
- META-INF/neoforge.mods.toml -> relocated neoforge set (official)
- META-INF/jars/ (Fabric/Quilt) and META-INF/jarjar/ (Forge/NeoForge) nested modularity-api

Scope narrowed 2026-11-08: parchment (bean 2a9k) and assembling the single jar now; these moved to their own beans:
- native Quilt entry points (`quilt.mod.json`) -> modularity-yfl7
- production readiness: Mixin refmap, mixins on Forge/NeoForge, real-loader verification -> modularity-dals,
  modularity-vk1z

## Work
- [x] UniversalJarTask in build-logic: merge the platform jars, ASM-relocate each class set, rewrite mixin configs,
      merge metadata + nested jars; output build/libs/modularity-<minecraft>-<version>.jar
- [x] Replace collectJars at the version parent; root collectJars/buildAll pick up the universal jar
- [ ] Spike: Forge and NeoForge both scan the jar for @Mod("modularity"); confirm duplicate entry classes are
      tolerated or replace them with one joint entry class
- [x] Update docs/07-multi-version-build.md (artifact, layout, ordering rule)

## Acceptance
- [x] `:platforms:21.1:build` leaves exactly one jar in platforms/versions/21.1/build/libs, named
      modularity-1.21.1-<modversion>.jar
- [ ] It loads and initializes Modularity on Fabric, Quilt, Forge and NeoForge 1.21.1
