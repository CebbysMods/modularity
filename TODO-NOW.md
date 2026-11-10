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

- [x] `./gradlew help` — the Allure plugins 4.3.0 resolve on Gradle 9.8.2.
- [x] `./gradlew -Pmodularity.platforms=none :modularity-api:test :modularity-testkit:test` — the 3.1.1
      adapter writes `build/allure-results` in both modules, with JUnit 5.10.3.
- [x] `./gradlew -Pmodularity.platforms=none allureAggregateReport` — one report under
      `build/reports/allure-report/` with the L1 tests of both modules.
- [x] The report shows a link to pull request 4 on every test. Run on 2026-11-10: 50 tests, all passed.
- [ ] Four-level tree and steps. The test run of 2026-11-10 17:34 passed (50 tests) and the raw results
      hold the four labels and 176 steps; the report run failed because the HTTP server held the report
      directory. Run `./gradlew -Pmodularity.platforms=none allureAggregateReport`, then check that the
      tree is Modularity &gt; L1 Unit Tests &gt; API area &gt; behaviour &gt; test, that each test shows a
      sentence, and that each test shows its `@Step` list.

### Risks

- **Network at build time.** `downloadAllure` fetches the Allure 3 runtime on the first report run. The
  report stays out of `buildAll`, so offline builds keep working.
- **Plugin on Gradle 9.8.2.** The plugin tests against 9.6.1. Both runs above passed, so the pair works.
- **Stale report.** The aggregate plugin reads the results in the task body and does not declare them as
  an input, so Gradle reported the task up-to-date after the results changed. Closed: the task always
  runs. Keep this in mind when a report shows old results.
- **Allure 3 CLI and an existing output directory.** The CLI writes nothing and exits with 0 when the
  output directory exists. Closed: the report task runs with `clean = true`.
- **Duplicate results in a module.** The adapter never clears its directory, so a second run left two
  results per test. Closed: each `test` task deletes its `allure-results` first.
- **Adapter 3.1.1 with JUnit 5.10.3.** The adapter does not constrain Jupiter, but the combination is
  unproven. A linkage error needs the owner's decision on the 2.x adapter.
- **AspectJ weaver with the 3.1.1 adapter.** The steps need `aspectjWeaver = true`, and the owner recalled
  a break between `@Step` and the weaver. If the run reports no steps, the fallback is `Allure.step(...)`
  in the test code, which needs no weaver.
- **Custom label name.** `@Functionality` writes the label `functionality`; the report groups by that name.
  A rename must change both `Functionality.LABEL` and `allurerc.json`.
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
