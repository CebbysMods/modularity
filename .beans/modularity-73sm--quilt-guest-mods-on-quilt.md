---
# modularity-73sm
title: 'quilt: guest mods on Quilt'
status: todo
type: task
priority: normal
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:47:20Z
parent: modularity-ecjq
---

Files: `quilt/entrypoint/ModularityQuiltLaunch.java`, docs/08.
Work: Quilt Loader has no Fabric Loader internals, so `LoaderInjection` cannot run. Keep the error line that names the guests and record the decision in docs/08: Quilt runs without guests until the owner picks a route.
Check: the Quilt smoke runs with no guest and the log shows no guest line.

## Tracker cleanup 2026-11-12

This bean is now a child of epic `modularity-ecjq`, which requires a loaded guest on every loader. The port limit written above no longer applies. The check is `Loaded guest mod smoke_guest` and `Guest mod smoke_guest: loaded=true initialized=true` in the smoke log of this loader. The loader evidence for Quilt is `modularity-4bm8`.

No run has proved a guest on this loader.
