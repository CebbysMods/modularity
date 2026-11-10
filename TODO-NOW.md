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
- [ ] **Layer base dir.** Check that `AssembleLayersTask` assembles the smoke mod from
      `modularity-smoke/` and that the smoke log shows `modularity_smoke` loaded.
- [ ] **`buildAll` scope.** Check that `buildAll` builds `:modularity-smoke` without a failure.

Closed on 2026-11-10:
- Root build split: `build.gradle` applies `maven-publish` in the second `configure` block, which
  holds `modularity-api` and `modularity-testkit` only.
- Java level: `javac --release 17` compiled `modularity-smoke/src/main/java` against the API classes
  with no error.

## One verification pass

The S3, S4 and S5 runs and the lifecycle checks (`modularity-8ywo`, `modularity-lw0r`) use the same
Gradle tasks. Run them after `modularity-r133` (NeoForge) and `modularity-g420` (Forge), so one pass
covers the platform-setup refactor and the lifecycle wiring. Until then, every Gradle check above
stays open.

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

- [ ] **No service files.** Check that the universal jar contains no
      `META-INF/services/lv.cebbys.mcmods.*` entry.

Run notes for the item above (task `modularity-f5e2`):

- `UniversalJarTask` is cacheable. Do not inspect a jar that is older than the merge.
- `:platforms:21.1:jar` builds all four platforms, so the service check runs alone.

Closed on 2026-11-10:
- Quilt loader version: `:platforms:21.1:21.1-quilt:smokeServer` logged
  `[modularity] platform QUILT 0.30.2, Minecraft 1.21.1, SERVER` in the line format of the time,
  which matches `quilt-loader` in the table. The format changed on 2026-11-10; the next smoke run
  must show `(server) platform quilt:0.30.2, minecraft:1.21.1`. The run is recorded in
  docs/07 §7.10.
- Public contract: the only `install` calls outside the API and its tests are in
  `ModularityForge`, `LoomEntrypoints` and `ModularityNeoForgeCommon`.
- Java level: `javac --release 17` compiled `modularity-api/src/main/java` and its tests with no
  error, and the 28 tests ran on that output.
- Version semantics: `VersionTest` lines 63 to 86 parse `0.20.1+build.42` and `unknown`, and assert
  that a `MinecraftVersion` and a `PlatformVersion` are not equal. All 28 tests pass.
- Nested module split: no file in `modularity-api/src/main/java` imports a loader, Minecraft or
  platform class.
- Forge side: `ForgePlatformSource.detect` loads the marker class with `initialize` set to `false`,
  and it calls `FMLLoader.isProduction()` as a method, not as a static field.
- Tests without Gradle: the four marker stubs are in `modularity-api/src/test/java` (`net/fabricmc`,
  `net/minecraftforge`, `net/neoforged`, `org/quiltmc`), and the tests pass on a plain class path.

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
