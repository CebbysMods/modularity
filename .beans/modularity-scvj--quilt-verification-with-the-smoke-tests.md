---
# modularity-scvj
title: 'quilt: verification with the smoke tests'
status: completed
type: task
priority: high
created_at: 2026-11-10T21:47:40Z
updated_at: 2026-11-11T23:46:53Z
parent: modularity-gng8
---

Check: `-p build-logic test`; `-Pmodularity.platforms=quilt-21.1 :platforms:21.1:smoke`; then `allureAggregateReport`. Record the run in docs/07 §7.10.

## Run 2026-11-11 (owner authorized the run)
- `./gradlew -p build-logic test`: BUILD SUCCESSFUL.
- `:platforms:21.1:21.1-quilt:smokeServer` and `smokeClient`: both ok, `(server) platform quilt:0.30.2, minecraft:1.21.1` and `(client) platform quilt:0.30.2, minecraft:1.21.1`, world join, screenshot and `Client done`.
- Re-run with the strengthened `SmokeCheck` (entrypoint stage lines) and `platforms/minecraft` byte-identical to `HEAD`: both pass.
Open: the shipped jar (`nested=no` in every dev run) and `allureAggregateReport`.


Verification, 2026-11-11 (feature smoke): `SmokeCheck` also expects the two `Smoke features:` lines on Quilt. The
smoke mod is registered through the `"modularity"` entrypoint in `quilt.mod.json`, so a Quilt smoke run now proves
block, item and channel registration. No Gradle run has done so yet.

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `allureAggregateReport` did not run; epic `modularity-0i9j` (stage S3) owns the report and its beans.
