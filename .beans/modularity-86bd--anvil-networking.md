---
# modularity-86bd
title: 'anvil: networking'
status: scrapped
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:47:02Z
parent: modularity-arii
---

Files: new `anvil/compat/**` payload classes.
Work: `PayloadRegistrar` and `RegisterPayloadHandlersEvent` are shared by NeoForge and Forge; the channel name comes from the mod id.
Check: both payload directions arrive in the smoke.

## State 2026-11-11
`anvil/compat/**` did not exist: the payload APIs of the two families differ, so the code is per loader — `neoforge/network/NeoForgePayloads.java` and `forge/network/ForgePayloads.java`, each one channel built at common setup, with `registerPayload`, `sendToPlayer` and `onServerPayload` in the loader hooks and `sendToServer` and `onClientPayload` in the client hooks. `javac` exit 0. No Gradle run. Check: both payload directions arrive in the smokes.

## Tracker cleanup 2026-11-12

Scrapped as a duplicate, so the tracker has one owner for one piece of work. The payload APIs of the two loaders differ, so the code is `neoforge/network/NeoForgePayloads` (`modularity-ra61`) and `forge/network/ForgePayloads` (`modularity-w922`).

The bean body stays as the record.
