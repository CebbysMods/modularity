---
# modularity-wovx
title: 'anvil: content registration'
status: scrapped
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:47:02Z
parent: modularity-arii
---

Files: new `anvil/loader/**`.
Work: `DeferredRegister` and `RegisterEvent` are the Forge-family route; put the registry and payload registration in one shared class and keep the event hook in the loader root.
Check: the smoke shows the block, item, block entity, tab, entity type, sound and channel of the guest mod.

## State 2026-11-11
The shared class did not land in `anvil`: registration runs through `Loaders.hooks().register(...)`, so the queue lives in each loader root — `neoforge/loader/NeoForgeLoaderHooks.java` and `forge/loader/ForgeLoaderHooks.java`, each a queue, a `RegisterEvent` replay and the creative tabs. Block entity types, payload types, sounds and channels use the same seam. `javac` exit 0 on both stacks. No Gradle run: AGENTS.md hard rule 1. Check: the smokes show the guest mod block, item, block entity, tab, entity type, sound and channel.

## Tracker cleanup 2026-11-12

Scrapped as a duplicate, so the tracker has one owner for one piece of work. Registration did not land in `anvil`: the queue lives in each loader root, `modularity-d5hk` (NeoForge) and `modularity-dti9` (Forge).

The bean body stays as the record.
