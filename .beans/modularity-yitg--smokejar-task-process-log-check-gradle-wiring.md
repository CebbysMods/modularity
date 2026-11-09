---
# modularity-yitg
title: 'SmokeJar task: process, log check, Gradle wiring'
status: todo
type: feature
priority: high
tags:
    - build
    - smoke
created_at: 2026-11-09T07:58:49Z
updated_at: 2026-11-09T07:59:04Z
parent: modularity-xbj0
blocked_by:
    - modularity-1vw1
    - modularity-000m
    - modularity-hxnf
---

Turn the launcher into a Gradle verification task.

## Classes
- `GameProcess` — start via `ProcessBuilder`, stderr -> stdout, tee to `console.log`, watch the world marker,
  grace period, destroy the process tree (`ProcessHandle.descendants()`), total timeout ~180 s.
  Marker: `Starting integrated minecraft server version 1.21.1` on the console (the vanilla `IntegratedServer`
  line; the integrated server never prints `Done (...)! For help`).
- `JarSmokeCheck` — pure expectations -> matched lines + problems (mirror `SmokeCheck.Result`).
- `JarSmokeTask` / `JarSmokeSetup` — per-platform task, `upToDateWhen(x -> false)`; uses the existing
  `SmokeRunService` (one game at a time) plus a `maxParallelUsages=1` install service.

## Run dir
`<platform project>/build/smoke-jar/run`: `mods/` = parent universal jar + the loader's `mvd-<loader>` /
`mpd-<loader>` jars (+ `net.fabricmc.fabric-api:fabric-api:<platform.fabricApi()>` for Fabric/Quilt via a
non-transitive `smokeJarMods` configuration). Never a child platform jar. Plus `saves/smoke` (cached world copy),
`options.txt` (reuse `SmokeSetup.writeOptions`), `logs/`, `launcher_profiles.json`.

## Gradle wiring
- `ModularityPlatformPlugin`: `smokeJarClient` + aggregate `smokeJar` (group `verification`) per platform, lazy
  `dependsOn` on `:platforms:<v>:jar`, the mvd/mpd jars and the Fabric API artifact.
- `ModularityVersionPlugin`: parent `smokeJar` aggregate in the existing `projectsEvaluated` hook (next to
  `wirePlatformJars`; the parent `jar` task may not exist when the child plugin is applied).
- `ModularityRootPlugin`: root `smokeJars`.
- No cycle: no `smokeJar*` task is an input of `jar` or the child jars.

## Outputs / failure reporting
`build/smoke-jar/{client.txt,client.log,client-console.log,command.txt}`; failure message includes the problems,
the `mods/` listing, the command and the last 40 console lines.

## Acceptance
- `./gradlew :platforms:21.1:21.1-fabric:smokeJarClient` launches the client and writes the four files.
- A deliberately broken mod jar makes the task fail with an actionable message.
