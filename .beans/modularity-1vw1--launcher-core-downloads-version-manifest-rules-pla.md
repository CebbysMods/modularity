---
# modularity-1vw1
title: 'Launcher core: downloads, version manifest, rules, placeholders, arguments, libraries'
status: todo
type: feature
priority: high
tags:
    - launcher
    - modularity-gradle
created_at: 2026-11-09T07:58:39Z
updated_at: 2026-11-09T07:58:39Z
parent: modularity-xbj0
---

Pure, unit-tested half of the hand-rolled launcher in `modularity-gradle/src/main/java/lv/cebbys/modularity/gradle/`
(one package, matching the existing style). No launcher library exists on Maven Central.

## Classes
- `LauncherPaths` — cache at `build/smoke-jar/cache`, installs at `cache/installs/<loader>-<mc>-<loaderVersion>`,
  run dir `<platform project>/build/smoke-jar/run`.
- `Downloads` — HTTP GET with User-Agent, timeouts, 3 retries with backoff, SHA-1 verification, atomic temp-file
  move; `GradleException` naming URL and HTTP status on failure.
- `MojangVersion` / `VersionManifest` — typed vanilla JSON (`id`, `mainClass`, `arguments.game/jvm`, `libraries`,
  `downloads.client/server`, `assetIndex`, `javaVersion`, `inheritsFrom`); recursive load parents-first, child wins
  for `mainClass`, libraries and arguments merge.
- `Rules` — Mojang semantics: no rules => allowed; otherwise last matching rule wins, default deny. Match
  `os.name` (windows/osx/linux), `os.arch` (x86/x86_64), `os.version` regex, and every `features` entry.
- `Placeholders` — `${...}` expansion (`library_directory`, `classpath_separator`, `natives_directory`,
  `version_name`, ...); unknown key is an error, not silently left in.
- `LaunchArguments` — apply rules + placeholders to `arguments.game` / `arguments.jvm`. Feature map:
  `has_custom_resolution`, `has_quick_plays_support`, `is_quick_play_singleplayer` true; everything else false.
  (Without the feature rules no `--quickPlaySingleplayer` is emitted and the client stays on the title screen.)
- `Libraries` — parse `group:artifact:version[:classifier][@ext]`, drop disallowed entries, dedupe child-first by
  `group:artifact:classifier[:ext]`, flag libraries whose `name` carries a `natives-<os>` classifier (1.21.1 has
  no `natives` map).

## Acceptance
- Tests with inline 1.21.1 fixtures: `VersionManifestTest`, `RulesTest`, `LaunchArgumentsTest`, `LibrariesTest`.
- `./gradlew -p modularity-gradle test` is green.
- Existing dev smoke (`smokeAll`) is unaffected.
