---
# modularity-4bm8
title: 'quilt: guest mods are closed by the loader''s plugin gate'
status: completed
type: task
priority: low
tags:
    - guest
    - quilt
created_at: 2026-11-11T23:08:08Z
updated_at: 2026-11-11T23:08:08Z
parent: modularity-gng8
---

Analysis of Quilt Loader 0.30.2 (sources jar of the table version), 2026-11-12.

The supported route is a Quilt loader plugin: QuiltPluginContext.addModLoadOption is public and runs during discovery. A mod can offer a plugin only through the experimental_quilt_loader_plugin field of quilt.mod.json, and V1ModMetadataImpl rejects that field unless the system property loader.experimental.allow_loading_plugins is set; it also logs that mod-provided plugins are for amusement only. A jar cannot set a loader system property before metadata is parsed.

The reflective route has the pieces: QuiltLoaderImpl keeps a mutable mods list and modMap, and EntrypointStorage.add is public. It fails at the container: InternalModContainerBase needs a QuiltPluginContext, which exists only inside a plugin.

Result: the docs/08 8.3 claim holds, now with the exact evidence. Guest parity on Quilt needs a loader version that exposes a mod-provided plugin. Files: docs/08-guest-mods.md 8.3. Check: none runs; this is a source reading.
