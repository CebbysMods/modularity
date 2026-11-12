---
# modularity-wo32
title: Move the smoke mods into a modularity-smoke Gradle module
status: completed
type: epic
priority: normal
tags:
    - build
    - smoke
created_at: 2026-11-10T10:22:19Z
updated_at: 2026-11-11T23:47:02Z
---

Move the smoke test mod from tools/smoke into a real Gradle module, modularity-smoke, next to
modularity-api and modularity-testkit.

Plan: C:\Users\valen\.claude-ollama\plans\serene-puzzling-otter.md

Target: the guest mod compiles as the module (java-library against modularity-api, jar fixed to
smoke_guest.jar); the layered roots (minecraft, loom, fabric, quilt, neoforge, forge) move unchanged
and are still assembled per platform by AssembleLayersTask.

Acceptance:
- :modularity-smoke is in settings.gradle, builds at release 17, and is not published.
- :modularity-smoke:jar produces smoke_guest.jar with modularity.mod.json and SmokeGuest.class only.
- SmokeSetup assembles from modularity-smoke/ and passes the module jar as modularity.guestMods.
- The Fabric smoke log shows the guest loaded and initialized; the Quilt smoke expects no guest line.
- Docs and AGENTS.md name modularity-smoke; tools/smoke is gone.

Risks are tracked in TODO-NOW.md.

## Tracker cleanup 2026-11-12

Closed as completed: every child bean is closed and the exit check ran green on 2026-11-12 (`-p build-logic test`, the eight 21.1 smokes, docs/07 7.10). The module is in `settings.gradle` and every smoke assembles from it.
