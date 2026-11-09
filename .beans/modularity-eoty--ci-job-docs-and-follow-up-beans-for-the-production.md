---
# modularity-eoty
title: CI job, docs and follow-up beans for the production smoke
status: todo
type: task
priority: normal
tags:
    - ci
    - docs
created_at: 2026-11-09T07:59:00Z
updated_at: 2026-11-09T07:59:04Z
parent: modularity-xbj0
blocked_by:
    - modularity-o6xs
    - modularity-jujd
    - modularity-8bjy
    - modularity-jzqc
---

## CI
- New job `jars` in `.github/workflows/smoke.yml`: JDK 21 + 25 via `actions/setup-java`,
  `xvfb-run -a -s "-screen 0 1280x720x24"`, `LIBGL_ALWAYS_SOFTWARE=1`,
  `./gradlew smokeJars --continue --max-workers=2`, `actions/cache` on the launcher cache, artifact upload of the
  summaries/logs/screenshots, timeout 180 min for the first cold run.

## Docs
- `docs/07-multi-version-build.md` §7.6 subsection "Production jar smoke": cache layout, installer CLIs, launch
  flags per loader, marker, outputs. Decisions-table row.
- Note the open gaps (`modularity-dals`, `modularity-vk1z`, `modularity-vgs5`).

## Follow-ups
- Tick `modularity-dals` / `modularity-vgs5` / `modularity-vk1z` with whatever the four runs prove.
- Record the Forge/NeoForge duplicate-`@Mod` outcome in `modularity-vgs5`.

## Acceptance
- The new CI job is green on a cold cache and reuses the cache afterwards.
- Artifacts contain four `client.txt` summaries and their logs.
