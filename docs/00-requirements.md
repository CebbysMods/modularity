# 0. Project requirements

What the build and the shipped artifacts must guarantee, and how each guarantee is checked. These are
the requirements an agent or a contributor must not contradict; the API's functional requirements are
in [02-requirements.md](02-requirements.md) and the build's design is in
[07-multi-version-build.md](07-multi-version-build.md).

Rules for this file:

- One requirement, one fact. If two documents state it, this one is the source.
- A requirement names the test, task or inspection that verifies it. A requirement without a check is a
  convention — mark it `convention` and expect it to need review.
- Change a requirement in the same change that alters the behaviour it describes.
- Pending work belongs in the bean tracker, not here. An unmet requirement says which bean closes it.

Status values: `met` — verified by its check; `partial` — true where checked, not yet everywhere;
`open` — not met yet; `convention` — stated, not machine-checked.

## A. Platforms and versions

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-1 | `gradle/platforms.toml` is the only place versions and loaders are defined. Code reads the table; no version or loader version is hardcoded in plugins, build scripts or sources. | Code review of the plugins; `TableAndMetadataTest` | met |
| PR-2 | The table's enabled platform rows are the supported set: every enabled row builds and its smoke passes. | `buildAll`, `smokeAll` | partial — 1.21.1-forge passed after the setup refactor; the other three 21.1 platforms have not been re-run |
| PR-3 | Every Minecraft version Modularity supports ships one universal jar that loads on Fabric, Quilt, NeoForge and Forge. Per-loader jars are build intermediates and are not published. | `:platforms:<version>:jar`; dev smokes; shipped-jar smoke (`modularity-xbj0`) | partial — dev-classpath smokes prove loading; the shipped jar has no client smoke yet |
| PR-4 | Parchment (parameter names, javadoc) layers over the official Mojang mappings wherever `[parchment]` has a version for the Minecraft version, on every loader of that version. It is compile-time metadata: class/member names, the remapped jar and the runtime are identical with or without it. A version without a release builds with official mappings alone. | `gradle/platforms.toml`; the loaders' mapping setup; Forge smoke resolves the `-parchment-` mavenizer artifact | met for 1.21.1 |
| PR-5 | Each platform compiles to the Java bytecode of its table row (`java =`), and the included build compiles for Java 25 because Loom 1.19 is Java 25 bytecode. | Toolchain and `options.release` in the plugins; CI with JDK 21 and 25 | met |

## B. Build

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-6 | Platform projects have no build script of their own: the settings/root/platform plugins configure them entirely from the table and the layer sources. | `ModularitySettingsPlugin`, `ModularityRootPlugin`, `ModularityPlatformPlugin`; layout in docs/07 §7.4 | met |
| PR-7 | Loader metadata (`fabric.mod.json`, the mixin config, `mods.toml`, `neoforge.mods.toml`) is generated from the table and the layered sources — never hand-edited — and declares the exact Minecraft version and the table's dependencies. | `GenerateModMetadataTask`, `TableAndMetadataTest` | met |

## C. Sources and layering

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-8 | Sources are assembled from the loader's layer roots in the documented order (common → changes ranges → the version), highest layer wins, `.removed` markers delete. The build fails on a malformed range, a file identical to the one it replaces, or a `.removed` without a target. | `LayersTest`, `AssembleLayersTask` | met |
| PR-9 | `modularity-api` has no Minecraft or loader dependency and compiles and tests on its own. | `modularity-api/build.gradle`, its tests | met |
| PR-10 | The universal jar carries one class set per mapping family, relocated under `.../universal/<loader>`, with every internal class reference rewritten consistently, so nothing fails with `NoClassDefFoundError` at load time. | `UniversalJarTest`; shipped-jar smoke (`modularity-xbj0`) | met for the relocation itself; not yet proven by loading the shipped jar |

## D. Runtime behaviour

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-11 | Modularity detects Minecraft version, loader and side on all four loaders, in development and from the shipped jar, without user configuration. | `SmokeCheck` (`[mvd]` / `[mpd]` lines); shipped-jar smoke (`modularity-xbj0`) | partial — proven for Forge after the refactor; the other loaders and the shipped jar are not yet re-run |
| PR-12 | On Fabric and Quilt, Modularity's `main`, `client`, `server` and `preLaunch` entry points all run; on NeoForge and Forge its `@Mod` constructor runs. | `SmokeCheck`; logged entry points (`modularity-up2d`) | partial — smokes check the platform line and the world join, not every entry point yet |
| PR-13 | On Forge and NeoForge, Modularity's own mixins apply, so the API's hooks work from the jar and not only from the dev class path. | smoke of the shipped jar | open — `modularity-vk1z`, `modularity-dals` |

## E. Quality gates

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-14 | `-p build-logic test` is green: the table, layering, metadata, version and universal-jar rules have unit tests. | `LayersTest`, `ModularityVersionPluginTest`, `TableAndMetadataTest`, `UniversalJarTest`, `VersionsTest` | met |
| PR-15 | CI builds every version (`buildAll collectJars`) and runs every smoke (`smokeAll`). A change is complete only when both are green. | `.github/workflows/build.yml`, `.github/workflows/smoke.yml` | configured; not verified green after the setup refactor |
| PR-16 | Decisions are recorded in docs/07 §7.8 and unfinished work in the bean tracker (`.beans`), so no requirement depends on unwritten history. | review | convention |

## Known limits — not requirements

- Guest mods (`modularity.mod.json`) load on Fabric only (docs/08).
- Native Quilt entry points (`quilt.mod.json`) are not wired: Quilt runs the Fabric entry points
  (`modularity-yfl7`).
- `mvd` and `mpd` are separate mods today; folding them into Modularity is `modularity-9oz9`.

## Where these are enforced

Tests and smokes are the enforcement; this file is the index of what they must cover. When you add a
requirement, add its check; when you add a check, reference it here. If a run is needed, the agent
rules in `AGENTS.md` decide whether you may start it.
