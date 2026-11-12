---
# modularity-hc5n
title: Forge lifecycle wiring
status: completed
type: feature
priority: normal
tags:
    - lifecycle
    - forge
created_at: 2026-11-09T12:45:47Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-osig
---

Install the seams and run common, client and dedicated-server init on Forge 52.1.21.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p modularity-gradle test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). The seams install and common, client and server init run on Forge.
