---
# modularity-koa2
title: 'quilt: networking'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:39Z
updated_at: 2026-11-12T08:12:56Z
parent: modularity-gng8
---

Files: `loom/compat/Payloads.java`, the payload methods of both hooks.
Work: audit both directions on Quilt Loader.
Check: the client receives the server payload and the server receives the client payload.


Check updated 2026-11-12 (not yet run): the smoke client now proves both directions in one round trip. `SmokeFeatures` registers `channel.onServer` (logs `Smoke network: server received 42`, answers with `channel.sendTo`), and the client stage registers `client.onMessage` (logs `Smoke network: client received 42`) plus a `client.onTick` first-tick send of 42 with `channel.sendToServer`. `SmokeCheck` expects both lines on the client side only (docs/07 7.6). A client smoke run closes this bean; a dedicated-server run proves neither direction.


Proved 2026-11-12 (third pass): `-p build-logic test` green (57 tests) and the eight 21.1 smokes green. The client log of every loader holds `Smoke network: server received 42` and `Smoke network: client received 42`, so the C2S and S2C directions and the codec are proved in one round trip. `SmokeCheck` requires both lines on the client side (docs/07 7.6, 7.10).
