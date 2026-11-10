# TODO-NOW

Notes that must be resolved **before the open features can be closed** — the platform-setup refactor
and the NeoForge/Forge lifecycle wiring. "Now" is not "this session" — it is "not left behind". Tick a
box only when the change is in and verified.

## S3 — NeoForge first

*Implemented and committed (`834d24f`); the verify run below is outstanding.*

Order: NeoForge before Forge, because `NeoForgeSetup` owned `loaderModule`/`coreModule` (shared by
both) and because it left the plugin's legacy body holding Forge alone.

- [ ] Verify: `-p build-logic test`, `:platforms:21.1:21.1-neoforge:smokeClient`.

## S4 — Forge last (and it finishes the job)

*Implemented and committed (`cc256b1`); the verify run below is outstanding.*

- [ ] Verify: `-p build-logic test`; `:platforms:21.1:21.1-forge:build` and inspect
      `META-INF/jarjar/metadata.json` + the nested jars; `:platforms:21.1:21.1-forge:smokeClient`.

## S5 — sweep and close

- [ ] Run `smokeAll` across the four loaders and `:platforms:21.1:jar`.

## Build and smoke command review

- [ ] Review [BUILD-SMOKE-COMMANDS.md](BUILD-SMOKE-COMMANDS.md) before closing: confirm that
      `-Pmodularity.platforms=<version> buildAll` and `smokeAll` are the two commands to keep, and that
      the selector test, the `smokeAll` wording and the doc changes are accepted.

## Anvil launch stage

- [ ] Review [ANVIL-LAUNCH.md](ANVIL-LAUNCH.md) before closing: confirm option 1 (one listener on
      Modularity's own mod bus for the loader `RegisterEvent`, behind a `static` guard) before the
      Forge-family launch stage or its docs are written. The document marks the judgements it could
      not prove, and the Forge seam is a prerequisite for the stage on Forge.

## Smoke module move (`modularity-smoke`) — open checks

Risks of the move of `tools/smoke` into the `:modularity-smoke` module. Tick a box only when its
check passes.

- [ ] **Module jar path.** `modularity.guestMods` is a fixed path string; check that
      `modularity-smoke/build/libs/smoke_guest.jar` exists after `:modularity-smoke:jar`.
- [ ] **Run-task ordering.** Check that a clean checkout runs the smoke with the guest jar present,
      because the run tasks depend on `:modularity-smoke:jar`.
- [ ] **Root build split.** Check that only `modularity-api` and `modularity-testkit` publish.
- [ ] **Layer base dir.** Check that `AssembleLayersTask` assembles the smoke mod from
      `modularity-smoke/` and that the smoke log shows `modularity_smoke` loaded.
- [ ] **Java level.** Check that the guest compiles at release 17 against `modularity-api`.
- [ ] **`buildAll` scope.** Check that `buildAll` builds `:modularity-smoke` without a failure.

## One verification pass

The S3, S4 and S5 runs and the lifecycle checks (`modularity-8ywo`, `modularity-lw0r`) use the same
Gradle tasks. Run them after `modularity-r133` (NeoForge) and `modularity-g420` (Forge), so one pass
covers the platform-setup refactor and the lifecycle wiring. Until then, every item above stays open.

## NeoForge/Forge lifecycle — decisions moved to beans

The risks are now decisions and checks inside the tracker; they are no longer tracked here.

- Content registration timing → `modularity-m23h`.
- Registry location vs universal-jar relocation → recorded in `modularity-ibbd`.
- NeoForge dist classes, parallel dispatch, entry-point log lines → recorded in `modularity-r133`.
- Forge constructor injection, event-bus mismatch → recorded in `modularity-g420`.
- Lazy `LoaderInfo` → recorded in `modularity-co2j` and `modularity-v53p`.
- The runtime checks run with `modularity-8ywo` and `modularity-lw0r`.

## mvd/mpd merge (`modularity-km3b`) — open checks

These are the risks of the merge plan. Remove an item only when its check passes.

- [ ] **Public contract.** `api.detect` is supported API. Check that the SPI stays minimal and that
      only a platform installs a source.
- [ ] **Java level.** `modularity-api` compiles at release 17. Check the sealed `Version` hierarchy,
      records and pattern matching against Java 17.
- [ ] **Version semantics.** A `MinecraftVersion` and a `PlatformVersion` must never compare equal,
      and `PlatformVersion` must accept `+build` suffixes and `unknown` without throwing.
- [ ] **Nested module split.** Loader sources reference only `api.detect` types and their own loader
      API. Check that `modularity-api` imports no loader class.
- [ ] **Quilt loader version.** Check that the `quilt_loader` container reports the version from
      `gradle/platforms.toml` on Quilt Loader 0.30.2.
- [ ] **Forge side.** Check that `FMLLoader.isProduction()` stays a method call and that the Forge
      constructor does not force a static initializer.
- [ ] **Tests without Gradle.** The moved tests keep the marker class stubs, or
      `MarkerClassSource` resolves the real loader on the test class path.
- [ ] **No service files.** Check that the universal jar contains no
      `META-INF/services/lv.cebbys.mcmods.*` entry.

## Quilt/Fabric separation (`loom`, `common`, native Quilt jar) — open checks

The loader facts and accepted costs are decisions in docs/07 §7.8; the refmap work stays in
`modularity-dals`. Tick a box only when the matching check passes.

- [ ] **Two metadata files in the universal jar.** Quilt's plugin must accept `quilt.mod.json`
      before Fabric reads `fabric.mod.json`; the shipped-jar smoke still must prove it.

Closed on 2026-11-09:
- Fabric Loom loads a QMJ mod in development: Quilt server and client smoke passed; both report
  `QUILT 0.30.2` and every Quilt entrypoint runs.
- Nested jars: `QuiltJarMetadata` adds `quilt_loader.jars` from the entries Loom nests; the unit test
  and the built Quilt jar both show `META-INF/jars/modularity-api-<version>.jar`.
- Split `platform.loader` package: the platform build compiles `common` and `minecraft` roots into one
  source set without an error.
