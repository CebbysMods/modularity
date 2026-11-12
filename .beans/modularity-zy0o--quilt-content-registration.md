---
# modularity-zy0o
title: 'quilt: content registration'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:39Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-gng8
---

Files: `loom/loader/ModularityLoomLoaderHooks.java`, `loom/compat/{BlockViews,CreativeTabs}.java`.
Work: no Quilt-specific code; audit that registration runs on Quilt Loader, which implements the Fabric API interfaces Modularity compiles against.
Check: the smoke shows the guest mod block, item, block entity and tab on Quilt.


2026-11-11: `SmokeFeatures` declares a block, an item and a channel through the `"modularity"` entrypoint in
`quilt.mod.json`; the Quilt smoke check expects its two log lines.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The Quilt log holds both `Smoke features:` lines with `present=true`.
