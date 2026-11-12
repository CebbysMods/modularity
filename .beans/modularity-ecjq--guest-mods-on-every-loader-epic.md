---
# modularity-ecjq
title: Guest mods on every loader (epic)
status: todo
type: epic
priority: high
tags:
    - guest
    - port
created_at: 2026-11-11T23:43:50Z
updated_at: 2026-11-11T23:43:52Z
blocked_by:
    - modularity-cuuj
---

A guest mod is one jar with a `modularity.mod.json` and no loader metadata (docs/08). Fabric loads it today; Quilt, NeoForge and Forge do not.

This epic owns the route on each remaining loader, after Stage 5 of the port programme. It stays `todo` until the owner opens it: the port closes with the guest row recorded as a limit that points here (PORT-REVIEW, TODO-NOW).

Children: `modularity-73sm` (Quilt), `modularity-zbbp` and `modularity-4l2a` (the Forge family). The Fabric route is `modularity-p999`; the loader evidence that closed the Quilt attempt for now is `modularity-4bm8`.

Known constraints, from docs/08 and bean 4bm8:
- Quilt's supported route is a mod-provided loader plugin, and `V1ModMetadataImpl` rejects the `experimental_quilt_loader_plugin` field unless `loader.experimental.allow_loading_plugins` is set. A jar cannot set that property before the loader parses its metadata.
- The reflective route fails at the mod container: the only implementations need a `QuiltPluginContext`, which exists only inside a plugin.
- The Forge family offers no public API to add a mod after discovery.

Acceptance for the epic: the same guest jar loads and initializes on Fabric, Quilt, NeoForge and Forge, and each platform smoke log holds `Loaded guest mod smoke_guest` and `Guest mod smoke_guest: loaded=true initialized=true`. Every proof needs a smoke run.
