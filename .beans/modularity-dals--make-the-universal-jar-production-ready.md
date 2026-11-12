---
# modularity-dals
title: Make the universal jar production-ready
status: scrapped
type: task
priority: high
tags:
    - build
    - jar
    - mixin
created_at: 2026-11-08T19:48:16Z
updated_at: 2026-11-11T23:47:00Z
---

Follow-up to "Ship one universal jar per Minecraft version": make the artifact work in real, non-development loaders,
not just in dev runs.

## Known gaps
- No Mixin refmap is produced today. The Fabric jar ships `modularity.mixins.json` but no refmap (verified in
  platforms/fabric/versions/21.1/build/libs, 2026-11-08), so production mixins cannot resolve intermediary method
  names outside a development launch.
- Mixins are not wired on Forge/NeoForge at all (bean modularity-vk1z).
- The universal jar has never been loaded by a real loader install (no automated production-jar smoke yet).

## Scope
- Enable the Mixin annotation processor / Loom refmap for every remapped platform; rewrite refmap keys when the
  universal jar relocates the fabric class set.
- Load the built jar in real Fabric, Quilt, Forge and NeoForge 1.21.1 servers and clients.
- Automate it: a production-jar smoke mode reusing SmokeCheck (deferred from the universal-jar change).

## Acceptance
- [ ] Refmap present and correct in the universal jar
- [ ] Universal jar boots on all four loaders and Modularity initializes
- [ ] Smoke harness can verify a built jar (not just dev classes)

## Tracker cleanup 2026-11-12

Scrapped as a duplicate, so the tracker has one owner for one piece of work. Its three gaps are now three owners: no refmap is needed on any loader (docs/00 PR-13, docs/07 7.8), mixins are `modularity-9rzu` on NeoForge and `modularity-6rm0` on Forge, and a load from the shipped jar is epic `modularity-xbj0`.

The bean body stays as the record.
