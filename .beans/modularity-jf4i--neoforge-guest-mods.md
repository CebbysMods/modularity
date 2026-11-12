---
# modularity-jf4i
title: 'neoforge: guest mods'
status: scrapped
type: task
priority: normal
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-10T22:30:00Z
parent: modularity-yrjc
---

Files: new `neoforge/guest/**`.
Work: no guest support today. NeoForge builds mod containers from metadata; decide the route in docs/08 first.
Check: the smoke log shows `Loaded guest mod smoke_guest`.

## Decision 2026-11-11 (stage 4)
Guest mods are not supported on NeoForge. NeoForge has no public API to add a mod after discovery, and the candidate scan (`GuestCandidates`) lives in `loom`, which NeoForge does not include; copying it would break the layering rule. `SmokeSetup` already marks guests for Fabric only, so the NeoForge smoke runs without the guest and expects neither guest line. Recorded in docs/08 section 8.3 and a docs/07 section 7.8 decision row. No Gradle run: AGENTS.md hard rule 1. Verification stays open in the verification bean.
