---
# modularity-opii
title: Cache portable/native wrapper instances on hot paths
status: todo
type: feature
priority: normal
tags:
    - performance
    - api
created_at: 2026-11-08T12:03:37Z
updated_at: 2026-11-08T12:03:37Z
---

`Wrap` (platforms/minecraft/common/src/main/java/lv/cebbys/mcmods/modularity/platform/Wrap.java) allocates a new record for every conversion: `Wrap.block(Block)` returns `new McBlockRef(block)`, `Wrap.state(...)` returns `new McBlockStateRef(state)`, and so on. Requirement N6 in docs/02-requirements.md says "per-block lookups are cached", but no identity cache exists today.

Idea source: the abandoned Aug 2025 attempt had an `InstanceManager` with a bidirectional weak cache (native <-> portable). Do not copy it as-is -- its `has()` returns true after the weak value was collected, it has no synchronization, and its keys were value-equal records.

## Scope
- Measure wrapper allocation first (JMH or a testkit benchmark); only cache where it is material.
- If warranted, add an identity-based weak cache for the hottest conversions (likely `Block` and `BlockState`).
- Keep `Handle.unwrap` semantics unchanged; wrappers must stay cheap value objects.

## Acceptance
- [ ] Benchmark shows wrapper allocation matters on hot paths
- [ ] Cache is weak in both directions (no loader/level leaks)
- [ ] Thread-safe for client and server use
- [ ] testkit tests cover identity and GC behaviour
- [ ] docs/02 N6 updated if implemented
