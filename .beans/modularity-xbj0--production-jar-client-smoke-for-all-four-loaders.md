---
# modularity-xbj0
title: Production-jar client smoke for all four loaders
status: todo
type: epic
priority: high
tags:
    - build
    - smoke
    - jar
created_at: 2026-11-09T07:58:29Z
updated_at: 2026-11-11T23:47:20Z
---

Today's smoke tests start all four loaders from development class paths (Loom / ModDevGradle / ForgeGradle),
so the shipped universal jar has never actually been loaded by a real loader. This epic adds a smoke test that
installs Minecraft 1.21.1 plus each loader (Fabric, Quilt, Forge, NeoForge) into real launcher directories,
puts the built universal jar + mvd/mpd (+ Fabric API) in `mods/`, launches the **client** (it also runs the
integrated server), and checks that every entry point logged itself. Wired as Gradle tasks and a CI job.

Design plan: `C:\Users\valen\.claude-ollama\plans\serene-puzzling-otter.md`.

## Scope
- Fix the universal jar's API relocation bug found while designing this (blocks everything else).
- Add `[modularity] entrypoint <name> active (<loader>)` logging to every entry point.
- Hand-rolled launcher in build-logic: vanilla install from Mojang's manifest, loader installers, `inheritsFrom`
  merge, rules/placeholders, natives, classpath, cached flat world, process management.
- `smokeJarClient` / `smokeJar` / `smokeJars` Gradle tasks and a CI job.

## Decisions
- Client, not server: the client also starts the integrated server.
- Production artifact, not the dev class path: that is the point of the smoke.
- Forge/NeoForge are expected to fail with what they fail with (duplicate @Mod, no mixin config) — the smoke must
  report the missing lines, not crash obscurely.

## Acceptance
- `./gradlew smokeJars` launches the client on all four 1.21.1 loaders from the universal jar.
- Each run produces `client.txt` / `client.log` / `client-console.log` / `command.txt` under
  `build/smoke-jar/`, listing every expected entry-point line that was found and every problem.
- CI job is green on a cold cache and reuses the cache on later runs.

## Update (2026-11-10)

mvd and mpd are merged into Modularity (docs/06). The expected lines are now one line,
`[modularity] platform <LOADER> <loaderVersion>, Minecraft <version>, <SIDE>`, and `mods/` holds only
the universal jar, Fabric API where the loader needs it, and the guest jar when the test uses one.

## Update (2026-11-12)

The universal jar now builds green and its contents are recorded in docs/07 §7.10 (`:platforms:21.1:jar`,
1,141,046 bytes, five metadata files, four mixin configs, both nested-API routes). That closes the
packaging half only. This epic stays `todo`: no task launches a loader from the jar, and every smoke run
still loads from a classes directory with `nested=no`.

## Mixin route for the universal jar (proposed 2026-11-12, not implemented)

One jar carries four loader trees, so one `MixinConfigs` manifest attribute cannot name a different config per loader, and NeoForge also reads the manifest through ModLauncher. Proposal: the universal jar sets `MixinConnector` instead, naming one class in a package that `UniversalJar` does not relocate. That class detects the running loader and adds only that loader's config with `Mixins.addConfiguration(...)`. Fabric and Quilt then keep reading their own metadata, and the Forge tree stops needing a manifest attribute. Risks before any code: `MixinConnectorManager` loads the class with `Class.forName(name, false, TCCL)`, and the TCCL at that point may not yet be the transforming loader, so the class must be reachable from the game class path alone; and a connector that adds the wrong loader's config would apply the same mixins twice, because two configs with the same mixins are two configs to Mixin. Only a run from the jar settles it, which is this bean's own acceptance.
