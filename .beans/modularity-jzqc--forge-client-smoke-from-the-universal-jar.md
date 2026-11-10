---
# modularity-jzqc
title: Forge client smoke from the universal jar
status: todo
type: task
priority: normal
tags:
    - smoke
    - forge
created_at: 2026-11-09T07:58:57Z
updated_at: 2026-11-09T07:59:04Z
parent: modularity-xbj0
blocked_by:
    - modularity-yg81
    - modularity-up2d
    - modularity-yitg
---

Forge 52.1.21 is not the NeoForge shape: `ForgeBootstrap`, `--launchTarget forge_client`, and a local patched
client artifact with an empty download URL.

## Requirements
- Write `launcher_profiles.json` before the installer; run it with cwd = install dir.
- Map the empty-URL library `net.minecraftforge:forge:1.21.1-52.1.21:client` to its file under `libraries/`.
- Do **not** append the vanilla client jar to the classpath.

## Expected in `client.txt`
- `[modularity] entrypoint mod-constructor active (forge)`
- Forge platform-loaded line
- `[mvd] Detected version 1.21.1 (`
- `[mpd] Detected platform FORGE 52.1.21, CLIENT`
- `Starting integrated minecraft server version 1.21.1`
- no `Duplicate mod`, no `Mod ID 'modularity' is already in use`

## Notes
- Expect the duplicate-`@Mod` / dual-metadata questions (bean modularity-vgs5) to surface here; the check must
  name the offending line and list `mods/`.

## Acceptance
- `:platforms:21.1:21.1-forge:smokeJarClient` is green.

## Update (2026-11-10)

mvd and mpd are merged into Modularity (docs/06). The expected lines are now one line,
`[modularity] platform <LOADER> <loaderVersion>, Minecraft <version>, <SIDE>`, and `mods/` holds only
the universal jar, Fabric API where the loader needs it, and the guest jar when the test uses one.
