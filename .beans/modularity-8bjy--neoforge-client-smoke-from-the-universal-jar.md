---
# modularity-8bjy
title: NeoForge client smoke from the universal jar
status: todo
type: task
priority: normal
tags:
    - smoke
    - neoforge
created_at: 2026-11-09T07:58:57Z
updated_at: 2026-11-09T07:59:04Z
parent: modularity-xbj0
blocked_by:
    - modularity-yg81
    - modularity-up2d
    - modularity-yitg
---

NeoForge 21.1.265 is the module-path loader; this ticket is where the `InstalledLauncher` details pay off.

## Requirements
- `version_name=neoforge-21.1.265` so `-DignoreList=client-extra,${version_name}.jar` matches.
- Original client jar copied to `versions/neoforge-21.1.265/neoforge-21.1.265.jar` and appended **last**.
- `-DlibraryDirectory=${library_directory}`, `-p .../bootstraplauncher/...`, `--add-modules ALL-MODULE-PATH`,
  `--offlineDeveloperMode`.

## Expected in `client.txt`
- `[modularity] entrypoint mod-constructor active (neoforge)`
- NeoForge platform-loaded line
- `[mvd] Detected version 1.21.1 (`
- `[mpd] Detected platform NEOFORGE 21.1.265, CLIENT`
- `Starting integrated minecraft server version 1.21.1`
- no `Duplicate mod`, no `Missing or unsupported mandatory dependencies`

## Notes
- The core does not start on NeoForge yet (bean modularity-vk1z); that is expected and recorded, not hidden.

## Acceptance
- `:platforms:21.1:21.1-neoforge:smokeJarClient` is green with exactly the four jars in `mods/`.
