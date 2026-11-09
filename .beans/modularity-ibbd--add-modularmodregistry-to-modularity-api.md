---
# modularity-ibbd
title: Add ModularModRegistry to modularity-api
status: todo
type: task
priority: normal
tags:
    - api
created_at: 2026-11-09T12:45:52Z
updated_at: 2026-11-09T12:45:52Z
parent: modularity-h0j3
---

Add the API-side registry that generated loader adapters call: LinkedHashMap in registration order, register throws on duplicate id or after freeze, entries returns an immutable copy. Add Modularity.registerMod(modId, mod) as the stable entry point.
