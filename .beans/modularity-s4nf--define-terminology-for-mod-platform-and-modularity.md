---
# modularity-s4nf
title: Define terminology for mod, platform and Modularity mod
status: todo
type: task
priority: normal
tags:
    - docs
    - naming
    - architecture
created_at: 2026-11-09T12:48:59Z
updated_at: 2026-11-09T12:48:59Z
---

The word 'mod' has several meanings in the code and the docs. A reader cannot tell which meaning applies. Define one term for each concept and use the terms everywhere.

Current overloaded uses:
- Modularity itself: 'the mod', the one artifact that users install.
- A loader build of Modularity: the Forge, NeoForge, Fabric or Quilt platform jar and its project (for example modularity-forge-21.1). The platform setups and the table also call it a mod.
- A mod built on Modularity: ModularMod, ModContext, the 'modularity' entrypoint, the guest mod, and the smoke mod.
- Loader concepts that keep their own names: @Mod, mods.toml, ModContainer, mod file, mod id, modImplementation, companion mod.

Needed:
- A short glossary with one term, one definition and one example for each concept. Put it in a stable place that agents read (AGENTS.md or a doc that AGENTS.md points to).
- Decide the term for each concept. Suggested direction (decide, do not assume): Modularity for the product, loader build or platform for the per-loader artifact, Modularity mod for a mod that uses the API, loader mod for a native @Mod.
- State that loader API names such as @Mod, mods.toml and ModContainer keep their native names and are not renamed.
- List ambiguous code identifiers and plan renames where the change is small. Examples to review: PlatformTable.Mod, LoaderPluginsContext, companion mods, and the 'mod' words in comments and log lines.
- Add a short writing rule: use a defined term and do not use a bare 'mod' when the meaning is not clear.

Acceptance:
- The glossary exists and AGENTS.md points to it.
- New documents, comments and answers use the defined terms.
- A review list of existing ambiguous uses exists, with the small renames done or tracked.
