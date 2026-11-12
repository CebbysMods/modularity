---
# modularity-a6mw
title: 'Java formatting: pick and wire a Gradle formatter plugin'
status: todo
type: feature
priority: normal
tags:
    - formatting
    - build
created_at: 2026-11-09T11:08:12Z
updated_at: 2026-11-09T11:08:12Z
---

The project has no source formatter: style is by hand. Pick a formatter plugin for Gradle and wire it so the
formatting rules are configured once, in the build, rather than per editor.

## Research
- Candidates: Spotless (`com.diffplug.spotless`) with `googleJavaFormat`, `palantirJavaFormat` or `eclipse`,
  the `fmt` plugin, and any Gradle-native option that exists for Gradle 9.8.2 / Java 25.
- For each: Gradle 9 + configuration-cache compatibility, whether it can run offline, how it is configured
  (AOSP vs Google vs Palantir vs Eclipse style knobs: indent, line length, import order, javadoc), and how it
  handles a project with no formatter config yet (one-off reformat diff).
- Check that every candidate supports the layout here: a multi-project build plus the included build
  `modularity-gradle`, sources that are *generated or copied* (`build/layered`, `build/generated`, `tools/smoke`
  layers) must be excluded, and version parents/`platforms` have no sources of their own.

## Implementation
- Configure the chosen plugin in the modularity-gradle convention plugins so every Java project (modules,
  utilities, platforms, smoke, `modularity-gradle` itself) is covered with one config.
- Tasks: a `format`/`spotlessApply`-style task per project and an aggregate root task; a `check`-bound verify
  task that fails on unformatted code.
- CI: run the verify task in the existing workflow(s) once the tree is reformatted, so the first green run is
  the reformatted baseline.
- Editor: document how to run it (`./gradlew format`) and, if the plugin supports it, the IntelliJ/Eclipse
  settings it exports; do not commit a plugin config that duplicates the Gradle rules.

## Decisions to record
- Which formatter and style, and why (diff size on the current tree is a factor).
- Whether the one-off reformat lands as its own commit (it should, so review stays readable).
- Exclusions and generated-source handling.

## Acceptance
- `./gradlew format` reformats every hand-written Java source and touches nothing under `build/`.
- A verify task fails when a file is unformatted, and the CI job runs it.
- `docs/` (build docs, likely `07-multi-version-build.md`) mentions the formatter and its task names.
