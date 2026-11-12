---
# modularity-w922
title: 'forge: networking'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-12T08:12:56Z
parent: modularity-cuuj
---

Files: new `forge/loader/**`, `forge/client/**`.
Work: `RegisterPayloadHandlersEvent` and `SimpleChannel`, both directions.
Check: both directions arrive in the smoke.

Done in the tree (no run yet). `ForgePayloads` builds one channel for the mod: `ChannelBuilder.named("modularity:payloads").networkProtocolVersion(1).optional().payloadChannel()` gives a `PayloadConnection`; `play().bidirectional()` gives a `PayloadFlow`, and every registered type is added with `addMain` inside `FMLCommonSetupEvent` (`enqueueWork`). Sends call `build()` first and then `channel.send(payload, PacketDistributor.PLAYER.with(player))` or `PacketDistributor.SERVER.noArg()`. Risk: `addMain` needs `StreamCodec<RegistryFriendlyByteBuf, P>`, so the raw `StreamCodec` is cast in one non-lambda helper. `javac` crashed on the same cast inside a lambda, so do not move it back into one. Check stays open: a smoke run must show a server-to-client and a client-to-server payload.


Check updated 2026-11-12 (not yet run): the smoke client now proves both directions in one round trip. `SmokeFeatures` registers `channel.onServer` (logs `Smoke network: server received 42`, answers with `channel.sendTo`), and the client stage registers `client.onMessage` (logs `Smoke network: client received 42`) plus a `client.onTick` first-tick send of 42 with `channel.sendToServer`. `SmokeCheck` expects both lines on the client side only (docs/07 7.6). A client smoke run closes this bean; a dedicated-server run proves neither direction.


Proved 2026-11-12 (third pass): `-p modularity-gradle test` green (57 tests) and the eight 21.1 smokes green. The client log of every loader holds `Smoke network: server received 42` and `Smoke network: client received 42`, so the C2S and S2C directions and the codec are proved in one round trip. `SmokeCheck` requires both lines on the client side (docs/07 7.6, 7.10).
