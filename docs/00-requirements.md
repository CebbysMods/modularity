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
| PR-2 | The table's enabled platform rows are the supported set: every enabled row builds and its smoke passes. | `buildAll`, `smokeAll` | partial — the four 21.1 platforms build and their eight server and client smokes pass (2026-11-12, third pass, with the network, storage and frame checks); `smokeAll` itself has not run, and the other versions in the table have not been re-run |
| PR-3 | Every Minecraft version Modularity supports ships one universal jar that loads on Fabric, Quilt, NeoForge and Forge. Per-loader jars are build intermediates and are not published. | `:platforms:<version>:jar`; dev smokes; shipped-jar smoke (`modularity-xbj0`) | partial — dev-classpath smokes prove loading; the shipped jar has no client smoke yet |
| PR-4 | Parchment (parameter names, javadoc) layers over the official Mojang mappings wherever `[parchment]` has a version for the Minecraft version, on every loader of that version. It is compile-time metadata: class/member names, the remapped jar and the runtime are identical with or without it. A version without a release builds with official mappings alone. | `gradle/platforms.toml`; the loaders' mapping setup; Forge smoke resolves the `-parchment-` mavenizer artifact | met for 1.21.1 |
| PR-5 | Each platform compiles to the Java bytecode of its table row (`java =`), and the included build compiles for Java 25 because Loom 1.19 is Java 25 bytecode. | Toolchain and `options.release` in the plugins; CI with JDK 21 and 25 | met |

## B. Build

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-6 | Platform projects have no build script of their own: the settings/root/platform plugins configure them entirely from the table and the layer sources. | `ModularitySettingsPlugin`, `ModularityRootPlugin`, `ModularityPlatformPlugin`; layout in docs/07 §7.4 | met |
| PR-7 | Loader metadata (`fabric.mod.json`, `quilt.mod.json`, the mixin config, `mods.toml`, `neoforge.mods.toml`) is generated from the table and the layered sources — never hand-edited — and declares the exact Minecraft version and the table's dependencies. | `GenerateModMetadataTask`, `TableAndMetadataTest` | met |

## C. Sources and layering

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-8 | Sources are assembled from the loader's layer roots in the documented order (common → changes ranges → the version), highest layer wins, `.removed` markers delete. The build fails on a malformed range, a file identical to the one it replaces, or a `.removed` without a target. | `LayersTest`, `AssembleLayersTask` | met |
| PR-9 | `modularity-api` has no Minecraft or loader dependency and compiles and tests on its own. | `modularity-api/build.gradle`, its tests | met |
| PR-10 | The universal jar carries one class set per mapping family, relocated under `.../universal/<loader>`, with every internal class reference rewritten consistently, so nothing fails with `NoClassDefFoundError` at load time. | `UniversalJarTest`; shipped-jar smoke (`modularity-xbj0`) | met for the relocation itself; not yet proven by loading the shipped jar |

## D. Runtime behaviour

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-11 | Modularity detects Minecraft version, loader and side on all four loaders, in development and from the shipped jar, without user configuration. | `SmokeCheck` (`(<side>) platform ...` line); shipped-jar smoke (`modularity-xbj0`) | partial — the development run is proven on all four loaders (2026-11-11 server and client smoke runs); the shipped jar is not yet run |
| PR-12 | On Fabric and Quilt, Modularity's `main`, `client`, `server` and launch entry points all run (Fabric `preLaunch`, Quilt `pre_launch`); on NeoForge and Forge its mod entry points run: mod construction, then common initialization, then client or dedicated-server initialization for the side. | `SmokeCheck`; logged entry points (`modularity-up2d`) | met — the server and the client smoke pass on all four loaders (2026-11-11); each log holds the `(<side>) platform …` line and, on the client, the world join |
| PR-13 | Modularity's own mixins apply on **every** loader — Fabric, Quilt, NeoForge and Forge — so the API's hooks work from the jar and not only from the dev class path. Where a loader has no metadata field for a mixin config, Modularity supplies the route itself. | smoke of every platform and of the shipped jar (`modularity-6rm0`, `modularity-xbj0`) | partial — Fabric, Quilt and NeoForge declare the config in their metadata and apply it in a development run (2026-11-11 and 2026-11-12 server and client smoke runs). Forge 1.21.1 has no metadata field, so its jar names the config in the jar manifest (`MixinConfigs`, the route the Sponge Mixin wiki gives for Forge; docs/07 §7.8) and its config sets `"verbose": true`. That route is not yet read: FML offers a mod jar to ModLauncher only when the jar provides a locator service, so Mixin never sees the manifest of a Modularity jar. The Forge server and client smokes are green and hold no `Mixing` line, which shows the gap. The jar now provides that service: an empty `IDependencyLocator` in
`META-INF/services/net.minecraftforge.forgespi.locating.IDependencyLocator`, generated for the `jar` task only (a copy on the development class path makes FML drop the mod, docs/07 §7.10), so the discoverer accepts the jar. A development run cannot use it; the proof needs a run from the shipped jar (beans `modularity-6rm0`,
`modularity-xbj0`). No loader needs a refmap: each config is written in the namespace its loader runs in. The shipped jar is unproven |

## E. Quality gates

| ID | Requirement | Verified by | Status |
|---|---|---|---|
| PR-14 | `-p modularity-gradle test`, `:modularity-api:test` and `:modularity-testkit:test` are green: the table, layering, metadata, version, universal-jar, detector and API rules have unit tests. | `LayersTest`, `ModularityVersionPluginTest`, `TableAndMetadataTest`, `UniversalJarTest`, `VersionsTest`, `VersionTest`, `PlatformDetectorTest`, `VersionDetectorTest`, the testkit tests | met |
| PR-15 | CI builds every enabled platform in a parallel job (`buildAll` with `-Pmodularity.platforms`), collects the universal jar of every version in a final job (`collectJars`), and runs each platform's smoke tests in a parallel job (`smoke`, or `smokeServer` when the table sets no client). A change is complete only when both workflows are green. | `.github/workflows/build.yml`, `.github/workflows/smoke.yml` | configured; not verified green after the CI matrix rewrite |
| PR-16 | Decisions are recorded in docs/07 §7.8 and unfinished work in the bean tracker (`.beans`), so no requirement depends on unwritten history. | review | convention |
| PR-17 | One Allure report per workflow shows the test pyramid: L1 unit tests and L2 build-contract tests from the JUnit engine, L3 in-game smoke runs as one test per platform and side with the log, the summary and the screenshot attached. Every test carries its layer as a label, and CI publishes the report as an artifact. | `allureAggregateReport`; `build.yml` and `smoke.yml` report jobs | partial — L1 is wired; L2 and L3 are not yet |

## Known limits — not requirements

- Guest mods (`modularity.mod.json`) load on Fabric only (docs/08).
- Quilt uses Fabric API as its library; QSL is not used (it is alpha-only for 1.21.1 and lacks the
  transfer, lookup, client-event and model-loading APIs Modularity needs).

## Where these are enforced

Tests and smokes are the enforcement; this file is the index of what they must cover. When you add a
requirement, add its check; when you add a check, reference it here. If a run is needed, the agent
rules in `AGENTS.md` decide whether you may start it.
