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

## One verification pass

The S3, S4 and S5 runs and the lifecycle checks (`modularity-8ywo`, `modularity-lw0r`) use the same
Gradle tasks. Run them after `modularity-r133` (NeoForge) and `modularity-g420` (Forge), so one pass
covers the platform-setup refactor and the lifecycle wiring. Until then, every Gradle check above
stays open.

## Allure test pyramid — open checks

Plan: `C:\Users\valen\.claude-ollama\plans\serene-puzzling-otter.md`. Three stages: S1 L1 unit tests,
S2 L2 build contract, S3 L3 in-game smoke. A stage stops for the owner's word; the build commands of
AGENTS.md hard rule 1 apply to every check. Tick a box only when the check passes.

### S1 — L1 unit tests

- [ ] `./gradlew help` — the Allure plugins 4.3.0 resolve on Gradle 9.8.2.
- [ ] `./gradlew -Pmodularity.platforms=none :modularity-api:test :modularity-testkit:test` — the 3.1.1
      adapter writes `build/allure-results` in both modules, with JUnit 5.10.3.
- [ ] `./gradlew -Pmodularity.platforms=none allureAggregateReport` — one report under
      `build/reports/allure-report/` with the L1 tests of both modules.

### Risks

- **Network at build time.** `downloadAllure` fetches the Allure 3 runtime on the first report run. The
  report stays out of `buildAll`, so offline builds keep working.
- **Plugin on Gradle 9.8.2.** The plugin tests against 9.6.1; the pair here is unproven until the first
  check above runs.
- **Adapter 3.1.1 with JUnit 5.10.3.** The adapter does not constrain Jupiter, but the combination is
  unproven. A linkage error needs the owner's decision on the 2.x adapter.
- **`withDependencies` clearing.** The aggregate plugin links to every project, which would resolve every
  platform class path; the root build clears those links and points the task at `build/allure-results`.
  Check the task's input log line (`Input directories for allureAggregateReport`) on the first run.
- **Included-build boundary (S2).** The aggregate plugin cannot see `build-logic`; a missing copy step
  silently leaves L2 out of the report.
- **Duplicate results (S3).** Smoke results use a deterministic `historyId` and the writer clears its
  directory; `allureCollectResults` is a `Sync`.

## NeoForge/Forge lifecycle — decisions moved to beans

The risks are now decisions and checks inside the tracker; they are no longer tracked here.

- Content registration timing → `modularity-m23h`.
- Registry location vs universal-jar relocation → recorded in `modularity-ibbd`.
- NeoForge dist classes, parallel dispatch, entry-point log lines → recorded in `modularity-r133`.
- Forge constructor injection, event-bus mismatch → recorded in `modularity-g420`.
- Lazy `LoaderInfo` → recorded in `modularity-co2j` and `modularity-v53p`.
- The runtime checks run with `modularity-8ywo` and `modularity-lw0r`.

## Quilt/Fabric separation (`loom`, `common`, native Quilt jar) — open checks

The loader facts and accepted costs are decisions in docs/07 §7.8; the refmap work stays in
`modularity-dals`. Tick a box only when the matching check passes.

- [ ] **Two metadata files in the universal jar.** Quilt's plugin must accept `quilt.mod.json`
      before Fabric reads `fabric.mod.json`; the shipped-jar smoke still must prove it.
