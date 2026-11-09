---
# modularity-jujd
title: Quilt client smoke from the universal jar
status: todo
type: task
priority: normal
tags:
    - smoke
    - quilt
created_at: 2026-11-09T07:58:53Z
updated_at: 2026-11-09T07:59:04Z
parent: modularity-xbj0
blocked_by:
    - modularity-o6xs
---

Same tree and jars as Fabric, installed with the Quilt installer; proves the Fabric-compatibility path through
the relocated Fabric class set.

## Expected
- `[modularity] entrypoint main/client/preLaunch active (quilt)`
- `[mpd] Detected platform QUILT 0.30.2, CLIENT`
- core init line reporting Quilt (not Fabric) once the loader name comes from `McRuntime`
- `Starting integrated minecraft server version 1.21.1`
- preLaunch logs no guests; no guest jar in `mods/`

## Acceptance
- `:platforms:21.1:21.1-quilt:smokeJarClient` is green.
- No `(development)` marker in the mpd line (that would mean the dev environment was used).
