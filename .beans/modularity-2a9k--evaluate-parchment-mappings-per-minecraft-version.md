---
# modularity-2a9k
title: Evaluate Parchment mappings per Minecraft version
status: todo
type: task
priority: low
tags:
    - build
    - tooling
created_at: 2026-11-08T12:03:40Z
updated_at: 2026-11-08T12:03:40Z
---

`build-logic` maps Minecraft with `loom.officialMojangMappings()` only (ModularityPlatformPlugin.java:118). The abandoned Aug 2025 1.21.5 attempt pinned Parchment (`parchment_version=2025.04.19` in gradle.properties) for readable parameter names when working inside Minecraft code.

## Scope
- Check Parchment availability for every platform in gradle/platforms.toml (1.21.1 through 26.3, plus Quilt/NeoForge/Forge).
- Prototype one platform; measure build time and remap impact.
- Decide: enable everywhere, only for development/smoke runs, or keep official Mojang mappings.
- Record the decision (and per-version coordinates, if adopted) in docs/07-multi-version-build.md.

## Acceptance
- [ ] Availability reviewed for all platforms
- [ ] Prototype build on one platform
- [ ] Decision and trade-offs recorded in docs/07
