---
# modularity-lf84
title: Rename the IDE layer source sets readably
status: todo
type: task
priority: normal
created_at: 2026-11-09T13:23:35Z
updated_at: 2026-11-09T13:23:35Z
---

## Goal

The IDE-only source sets that index the layer folders are named `ideLayers0`, `ideLayers1`, ... This is stable but opaque. Choose a readable, deterministic name per layer root, so the IntelliJ project view and the Gradle model say what each set contains.

## What to research

- What identifiers `AbstractPlatformSetup.ideLayerSources` can build without a clash: `layersMinecraftCommon`, `layersMinecraftChanges1212plus`, `layersFabricCommon`, `layersFabricVersions211`, ... Include the loader root, the layer kind (`common`, `changes`, `versions`) and the version or range.
- Whether a longer name is still readable in IntelliJ, and whether uppercase and digits are allowed (they are, but check the Gradle name rules and the generated task names `compile<Name>Java`).
- Whether the name should be the same for every platform project (one project contains only one loader, so the loader segment may be redundant) or short and ordered.
- The effect on the `exclude` patterns and on `Layers.resolve`: the current index is also the precedence order, so keep the order visible, for example a numeric prefix or the array position in the description.
- A way to check that the names are unique and stable when `gradle/platforms.toml` adds a version or a layer root.

## Deliverable

Write the decision in `docs/07-multi-version-build.md` §7.4 (the `ideLayers*` paragraph) and apply it in `AbstractPlatformSetup.ideLayerSources`. Add a test or a log line that shows the names for one platform.

## Constraints

- The sets stay IDE-only: no compiled output, no dependency of another task.
- The names must not change when a layer folder is empty: skip the empty layer, do not renumber the other layers by content.
