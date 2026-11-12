---
# modularity-ra61
title: 'neoforge: networking'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-12T08:12:56Z
parent: modularity-yrjc
---

Files: `neoforge/loader/NeoForgeLoaderHooks.java`, `neoforge/client/NeoForgeClientHooks.java`.
Work: the four payload methods are in the stub set above; the client send and receive are wired.
Check: both directions arrive in the smoke.

## Audit 2026-11-11 (stage 4)
In the tree: `NeoForgePayloads` queues the codecs and installs one `RegisterPayloadHandlersEvent` listener (protocol version "1"); every payload is bidirectional and the handler looks up the listener for the receiving side when the payload arrives, on the game thread through `enqueueWork`. `sendToPlayer` uses `ServerPlayer.connection`; the client sends through `Minecraft.getInstance().getConnection()`. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.


Check updated 2026-11-12 (not yet run): the smoke client now proves both directions in one round trip. `SmokeFeatures` registers `channel.onServer` (logs `Smoke network: server received 42`, answers with `channel.sendTo`), and the client stage registers `client.onMessage` (logs `Smoke network: client received 42`) plus a `client.onTick` first-tick send of 42 with `channel.sendToServer`. `SmokeCheck` expects both lines on the client side only (docs/07 7.6). A client smoke run closes this bean; a dedicated-server run proves neither direction.


Proved 2026-11-12 (third pass): `-p modularity-gradle test` green (57 tests) and the eight 21.1 smokes green. The client log of every loader holds `Smoke network: server received 42` and `Smoke network: client received 42`, so the C2S and S2C directions and the codec are proved in one round trip. `SmokeCheck` requires both lines on the client side (docs/07 7.6, 7.10).
