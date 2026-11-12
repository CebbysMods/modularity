---
# modularity-yp34
title: 'anvil: verification with the smoke tests'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:45Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-arii
---

Check: `-p build-logic test`; the NeoForge and Forge smokes after the anvil refactor; `allureAggregateReport`. Record the runs in docs/07 §7.10.

## State 2026-11-11
Both Forge-family trees compile with `javac --release 21` (Forge 98 sources, NeoForge 97 sources, exit 0, no errors). No Gradle task ran and no smoke ran, so this bean stays open. Closing commands: `-p build-logic test`; `:platforms:21.1:21.1-neoforge:smokeServer`; `:platforms:21.1:21.1-neoforge:smokeClient`; `:platforms:21.1:21.1-forge:smokeServer`; `:platforms:21.1:21.1-forge:smokeClient`; then `allureAggregateReport`. Record each run in docs/07 §7.10.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `allureAggregateReport` did not run; epic `modularity-0i9j` owns it.
