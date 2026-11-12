---
# modularity-w71g
title: 'anvil: client features'
status: scrapped
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:47:02Z
parent: modularity-arii
---

Files: new `anvil/client/**`.
Work: HUD through the render events, dynamic models through the model baking event, key mappings through the registration event, entity renderers through `EntityRenderersEvent`.
Check: the client smoke log and the screenshot show the HUD layer and the dynamic model.

## State 2026-11-11
`anvil/client/**` did not exist: the client features are per loader — `neoforge/client/NeoForgeClientHooks.java` and `forge/client/ForgeClientHooks.java` (key mappings, client payloads, entity renderers, HUD layers, client tick, reload listeners), with `NeoForgeDynamicBakedModel` and `ForgeDynamicBakedModel` for the dynamic models. `javac` exit 0. No Gradle run. Check: the client smokes render the HUD layer and the dynamic model; the item form of a dynamic-model block renders nothing.

## Tracker cleanup 2026-11-12

Scrapped as a duplicate, so the tracker has one owner for one piece of work. The client features did not land in `anvil`: the two loaders' client APIs differ, so the code is `neoforge/client/NeoForgeClientHooks` (`modularity-lf0u`) and `forge/client/ForgeClientHooks` (`modularity-hcru`).

The bean body stays as the record.
