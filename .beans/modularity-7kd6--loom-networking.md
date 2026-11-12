---
# modularity-7kd6
title: 'loom: networking'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:44:12Z
updated_at: 2026-11-12T08:12:55Z
parent: modularity-q4hh
---

Files: `loom/compat/Payloads.java`, `ModularityLoomLoaderHooks`, `ModularityLoomClientHooks`.
Work: audit payload registration, both directions and the channel codec binding.
Check: the smoke client receives the server payload and the server receives the client payload.

Audit 2026-11-10: `compat.Payloads.registerPlay` registers C2S and S2C; `ModularityLoomLoaderHooks` and `ModularityLoomClientHooks` send and receive through Fabric networking. No stub.



2026-11-10 (branch feature/port-loom): stage 1 audit. The work is in the tree. `grep` finds no `UnsupportedOperationException` and no `notImplemented` under `platforms/loom`. The Fabric root now holds only `fabric/entrypoint` and `fabric/guest`, so each functionality lives in one place. `javac` compiled the loom, fabric and quilt sources against the 1.21.1 mapped jar and the Fabric, Quilt and Allure class paths with no error. No Gradle run (hard rule 1), so every smoke check below stays open: `-p build-logic test`, `-Pmodularity.platforms=fabric-21.1 :platforms:21.1:smoke`, `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`.
Specific: `ModularityLoomLoaderHooks.registerPayload`, `sendToPlayer` and `onServerPayload` bind both directions through `Payloads`.


Check updated 2026-11-12 (not yet run): the smoke client now proves both directions in one round trip. `SmokeFeatures` registers `channel.onServer` (logs `Smoke network: server received 42`, answers with `channel.sendTo`), and the client stage registers `client.onMessage` (logs `Smoke network: client received 42`) plus a `client.onTick` first-tick send of 42 with `channel.sendToServer`. `SmokeCheck` expects both lines on the client side only (docs/07 7.6). A client smoke run closes this bean; a dedicated-server run proves neither direction.


Proved 2026-11-12 (third pass): `-p build-logic test` green (57 tests) and the eight 21.1 smokes green. The client log of every loader holds `Smoke network: server received 42` and `Smoke network: client received 42`, so the C2S and S2C directions and the codec are proved in one round trip. `SmokeCheck` requires both lines on the client side (docs/07 7.6, 7.10).
