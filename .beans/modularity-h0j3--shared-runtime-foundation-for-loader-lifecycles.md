---
# modularity-h0j3
title: Shared runtime foundation for loader lifecycles
status: completed
type: feature
priority: normal
tags:
    - lifecycle
    - build
created_at: 2026-11-09T12:45:47Z
updated_at: 2026-11-09T12:50:42Z
parent: modularity-osig
---

Mapping-neutral code that both new loaders use: the ModularMod registry, the shared server init, the reload listener helper, and the LoaderInfo contract.

## Summary of Changes
All five tickets are implemented: registry, register/freeze, shared server init, reload listener helper, loader order javadoc. 
