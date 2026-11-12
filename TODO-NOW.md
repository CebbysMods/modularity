# TODO-NOW

Notes that must be resolved **before the open features can be closed** — the platform-setup refactor
and the NeoForge/Forge lifecycle wiring. "Now" is not "this session" — it is "not left behind". Tick a
box only when the change is in and verified.

## S3 — NeoForge first

*Implemented and committed (`834d24f`); the verify run below is outstanding.*

Order: NeoForge before Forge, because `NeoForgeSetup` owned `loaderModule`/`coreModule` (shared by
both) and because it left the plugin's legacy body holding Forge alone.

- [x] Verify: `-p build-logic test` and `:platforms:21.1:21.1-neoforge:smokeClient` both pass (2026-11-12).

## S4 — Forge last (and it finishes the job)

*Implemented and committed (`cc256b1`); the verify run below is outstanding.*

- [x] Verify: `-p build-logic test` and `:platforms:21.1:21.1-forge:build` pass (2026-11-12). The Forge jar
      holds `META-INF/jarjar/metadata.json` and the nested API jar, and its manifest carries
      `MixinConfigs: modularity.forge.mixins.json`. `:platforms:21.1:21.1-forge:smokeClient` passes.

## S5 — sweep and close

- [ ] Run `smokeAll` across the four loaders. `:platforms:21.1:jar` ran on 2026-11-12 and passed (the
      universal jar is in docs/07 §7.10: 1,141,046 bytes, five metadata files, four mixin configs, both
      jar-in-jar routes). The eight 21.1 smokes of the four loaders also ran on 2026-11-12, but with
      explicit task names and not through `smokeAll`, so the aggregate task is still open.

## Feature parity against Fabric (risk)

The goal is one feature set on five loaders. The matrix of 2026-11-12 gives Fabric 95, Quilt 91, NeoForge
90 and Forge 81. The delta is four named rows, each with a bean (PORT-REVIEW, "Parity against Fabric"):

- Forge mixins: required on every platform by the owner (2026-11-12). **A development run applies them
  now:** the generated metadata of the Forge platform carries `META-INF/MANIFEST.MF` with `MixinConfigs`
  (a development run reads the manifest of its classes folder, not of the jar), and the 2026-11-12 Forge
  client smoke holds seven `Mixing … from modularity.forge.mixins.json` lines, including
  `client.ParticleEngineMixin` and `client.CameraMixin` (docs/07 §7.8 and §7.10, bean `modularity-6rm0`).
  Still open: no run starts from a shipped jar, so the manifest of the production jar is unproven, and
  one attribute cannot name a different config for four loader trees in the universal jar
  (`modularity-xbj0`). Risk for the shipped jar: FML's `ModDirTransformerDiscoverer` offers a `mods/*.jar`
  to ModLauncher only when the jar provides a locator service; the locator is generated
  (`forgeLocatorService`) but no run has loaded the jar. `mods.toml` also carries a `[[mixins]]` block that
  Forge 1.21.1 has no field for; FML reads the file as TOML and keeps one source for the name, and every
  green run shows that FML accepts it.
- Guest mods load on Fabric only; Quilt reports a guest and the Forge family has no route (docs/08). This row
  is **closed as a limit for the port**: the epic `modularity-ecjq` owns the guest route on the other
  loaders and starts after Stage 5. Its children are `modularity-73sm` (Quilt), `modularity-zbbp` and
  `modularity-4l2a` (the Forge family); `modularity-4bm8` holds the loader evidence.
- No run has rendered a dynamic-model frame on any loader.
- No run has started a loader from the shipped jar (`modularity-xbj0`).
- The choice of what to open next — the storage proofs, the shipped-jar smoke or the guests — is bean
  `modularity-ab6r`, waiting for the owner.

Do not close a platform epic until the owner accepts its delta rows as limits or names the next route.

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

- [x] Re-run the eight smokes for 21.1 after the feature smoke and the Forge-family common-stage change
      (2026-11-12). The first pass failed on NeoForge and Forge (`Registry is already frozen` in
      `Block.<init>`, because `NewRegistryEvent` precedes `GameData.unfreezeData`); after the common stage
      moved to the first `RegisterEvent`, the second pass is green: `BUILD SUCCESSFUL in 4m 14s`, all eight
      smokes pass, and every server and client log of the four loaders holds both `Smoke features:` lines
      (docs/07 §7.10, bean `modularity-4uol`).

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
- [x] Four-level tree and steps. The first report showed two defects: two stories owned the version
      tests, and most behaviour groups held one test. The tests were regrouped (story = the API concern,
      `Version Model API`, a behaviour label only for two or more tests, no object parameters in the
      steps). Run of 2026-11-10: `:modularity-api:test` and `:modularity-testkit:test` passed (50 tests),
      and `allureAggregateReport` wrote the tree Modularity &gt; Unit Tests &gt; API concern &gt;
      behaviour &gt; test. No group holds one test, and no step parameter shows an object identity string.

### S2 — L2 build contract

- [x] `./gradlew -p build-logic test` — the 3.1.1 adapter writes `build-logic/build/allure-results`, 46
      tests pass, and no test fails on the new `@DisplayName`/label imports. Run of 2026-11-10:
      BUILD SUCCESSFUL, 46 results, all passed.
- [x] `./gradlew -Pmodularity.platforms=none allureAggregateReport` — `build/allure-results` holds results
      from three producers, and the tree shows `Integration Tests` with the five stories and the two unit
      concerns (`Version Ranges`, `Smoke Log Checks`) next to the unit modules. Run of 2026-11-10:
      BUILD SUCCESSFUL, the report holds 96 tests, `Unit Tests` 65 and `Integration Tests` 31. The feature
      names carry no layer number, and `allurerc.json` sorts every level by name.
- [x] A second root report run does not duplicate a test: `allureCollectResults` is a `Sync`, and the
      `build-logic` test task clears its own results first.

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

## Port programme — open checks

Goal: port every Fabric feature to `loom`, `quilt`, `anvil`, `neoforge` and `forge`, one stage per
platform. Epics: `modularity-q4hh` (loom), `modularity-gng8` (quilt), `modularity-arii` (anvil),
`modularity-yrjc` (neoforge), `modularity-cuuj` (forge). Branches: `feature/port-<platform>`. The five branches
exist (2026-11-11) and all point at `5daf9d7`; the port work is uncommitted in the working tree, because AGENTS.md
hard rule 2 holds until the owner allows a commit. A stage stops for the owner's word; no Gradle run without it
(AGENTS.md hard rule 1). Tick a box only when its check passes.

[PORT-REVIEW.md](PORT-REVIEW.md) lists, per stage, the loader files and the checks that close it, so a stage can be
reviewed on its own while the work is uncommitted.

### Parity audit of the five lifecycles (2026-11-11, inspection only)

The stage order of `Loom`, `Anvil` (for NeoForge and Forge) and the Fabric entrypoints matches Fabric's:

- launch: `EntryPoints.active(LAUNCH)` then the seams; Fabric then loads guests and calls
  `ModularityLaunch.init`; Quilt reports guests and calls it; the Forge family runs it in the constructor and
  calls `launchMods` on the first `RegisterEvent` (bean `modularity-4uol`).
- common: `EntryPoints.active(COMMON)`, seams, `SyncedData.init`, `DetectionLog.log`, `ModularityCommon.init`.
- client: `EntryPoints.active(CLIENT)`, `Loaders.installClient`, `ModularityClient.init`.
- server: `EntryPoints.active(SERVER)`, `ModularityServer.init`.

Two intended differences: the Forge family installs the detection sources in `ModularityAnvilLifecycle.detect()`, which runs
before mod construction, and its client and server stages run in `enqueueWork` of the setup events. Guests run on
Fabric only (docs/08). Every seam interface method has an implementation on all three code paths, and `javac` proves
it for Forge and NeoForge. No discrepancy found; the per-platform checks above stay open because none of them ran.

### Risks

- **Anvil did not exist (resolved 2026-11-11).** `platforms/anvil` now holds `ModularityAnvilLifecycle`,
  `ModularityAnvilSeams`, `ModularityAnvilLoaderApi`, `ModularityAnvilPlatformSource` and `ModularityAnvilVersionSource`, and `Loader.java`
  stacks `minecraft, common, anvil, neoforge` and `minecraft, common, anvil, forge`. The layer compiles
  with `javac` and neither loader on the class path.
- **Guest injection is loader internals.** `fabric/guest/LoaderInjection` uses Fabric Loader private
  fields; Quilt Loader has no such classes, so the Quilt stage must decide the guest route, not copy it.
- **Registration order.** Fabric registers into open registries during init; NeoForge and Forge register
  in mod-bus or deferred events. A shared `anvil` class must take the event, or the order differs.
- **A mod jar could not register on the Forge family (found 2026-11-11; second fix in the tree, unproven;
  bean `modularity-4uol`).** `ModularityNeoForgeCommon` and `ModularityForge` used to call
  `ModularityAnvilLifecycle.common` in Modularity's own constructor, and `ModularityCommon.init` freezes
  `ModularModRegistry` there. No other mod was constructed yet, so a later `Modularity.registerMod` threw.
  The first fix moved the common stage to `NewRegistryEvent`, and the run of 2026-11-12 disproved it:
  `NewRegistryEvent` fires before `GameData.unfreezeData`, so every vanilla registry is still frozen there
  and `Block.<init>` threw `IllegalStateException: Registry is already frozen` (NeoForge and Forge server
  runs; the Fabric and Quilt runs of the same pass logged the two feature lines and passed). The common
  stage now runs on the first `RegisterEvent`, guarded by a `boolean`; the entrypoint then calls
  `replay(event)` on that loader's hook, because its listener has already run for that registry. The
  registration of a dependent mod in its own constructor still holds, because `RegisterEvent` is posted
  after every constructor. `SmokeCheck` expects the feature lines on every loader. Not proved yet: the
  re-run of the eight smokes.
- **Fabric API must not leak.** `loom` compiles against Fabric API; the Forge family must not depend on
  a class from it. A shared file moves to `anvil` only when it names no Fabric type.
- **Layering.** A port adds files under the loader root and `changes/<range>` only. A copied shared file
  breaks the layering check in `LayersTest`.
- **Proved by the runs of 2026-11-12 (third pass).** `-p build-logic test` is green (57 tests) and the
  eight 21.1 smokes pass on the four loaders with the new checks: the payload round trip, the fluid unit
  and a rendered frame (docs/07 §7.10).
- **Payload and tank checks (proved 2026-11-12).** Both payload directions and the loader's fluid unit are
  green on all four loaders: Fabric and Quilt report `unitsPerMb=81`, NeoForge and Forge `unitsPerMb=1`,
  and every client log holds `Smoke network: server received 42` and `Smoke network: client received 42`
  (docs/07 §7.10). The four networking beans (`7kd6`, `koa2`, `ra61`, `w922`) close on that run.
- **Storage seam (proved 2026-11-12, third run of the day).** Every client log of the four loaders holds
  `Smoke storage: held item moved 1000 mB` and `Smoke storage: exposed 500 mB`: the server player's water
  bucket is emptied into a tank, and the spawned entity's tank answers the lookup of the loader. The four
  storage beans (`dgzs`, `mizg`, `gz97`, `tll2`) close on that run. The pass found and fixed four defects:
  Quilt refuses an entrypoint class that names client packages, the Fabric tank storage threw on a null
  transaction, Forge needed `addGenericListener`, and Forge moved no fluid from a bucket (docs/07 §7.8,
  §7.10). The seventh run of 2026-11-12 adds the full cycle with three right-clicks — fill, take out, and a
  second empty bucket that must take nothing — through the new seam method
  `StorageBackend.drainToHeldItem`; it also found that both Forge-family tank handlers read the fluid after
  emptying the tank and reported 0 mB, and that Quilt's water fluid yields a blank bucket variant.
- **Rendered frame (proved 2026-11-12).** The client check reads the screenshot and fails an empty,
  unreadable, undersized or flat image; all four client smokes passed it, which proves that `SmokeSetup`
  passed `<run dir>/screenshots/smoke.png` and that a frame rendered.
- **Client seam in the smoke (proved 2026-11-12 on all four loaders).** The client stage of `SmokeFeatures`
  calls `client.blockModel`, `client.hud` and `client.keyBinding`, and logs `Smoke model: registered for
  <block>`, `Smoke client: hud drawn <w>x<h>` and `Smoke keys: binding=<name> down=<true|false>`.
  `SmokeCheck` requires all three on every client run. The fourth run of 2026-11-12 holds all three lines on
  Fabric, Quilt, NeoForge and Forge, so the client beans (`dga9`, `yrlm`, `lf0u`, `hcru`) close on that run.
  The fifth run adds the OBJ model path: the smoke parses `modularity_smoke:models/obj/smoke_cube.obj`
  (`Smoke obj: parsed … faces=6 groups=1`), the entity renderer draws its six faces (`Smoke obj: drawn …
  faces=6`), and the screenshot shows the stone-textured cube. The path is shared code, so the anvil root
  needs no copy (docs/07 §7.8, §7.10).
- **World scenario in the client smoke (run 2026-11-12; green on Fabric and Quilt).** `SmokeScenario`
  places the block at head height, three blocks in front of the player, releases the mouse and holds the
  mine button through `Minecraft.continueAttack(true)` from tick 30 to tick 110, re-aiming the player on
  every mining tick. The model logs `Smoke model: quads at <pos>` on its first render pass, the
  hit-particle handler logs `Smoke particles: hit <block> at <pos>`, and the placement logs
  `Smoke scenario: placed <block> at <x, y, z>`. `SmokeCheck` requires all three on every client run.
  The third run of 2026-11-12 proved on Fabric and Quilt that `Minecraft.continueAttack` is the path that
  spawns the hit particles (`ParticleEngine.crack` has no other caller) and that the placement must run on
  the server thread.
  **Risk:** the method names `setBlockAndUpdate`, `continueAttack`, `getSingleplayerServer`,
  `releaseMouse`, `setYRot` and `setXRot` are not checked by a build. An offline `javac` of the smoke
  sources against the named Loom jars compiles 1.21.1, 1.21.10 and 1.21.11 without errors (1.21.11 needs
  the JDK 21 `javac`; the JDK 25 `javac` crashes in its own flow analysis). 26.1 and 26.3 cannot be
  checked offline, because their Loom caches hold no Fabric API jars. The first client smoke of each
  version shows whether it has the six methods, and the mining also proves that the Forge-family mixins
  apply, because only `ParticleEngine.crack` (patched by the mixin) calls the handler.
- **Entity chain in the client smoke (proved 2026-11-12 on all four loaders).** The common stage
  registers an entity type, the block spawns it when a player breaks it, and the client logs
  `Smoke entity: registered for <id>`, `Smoke entity: spawned <id> at <pos>` and `Smoke entity: rendered
  <id>`. `SmokeCheck` requires all three (`SmokeCheckTest.clientNeedsTheEntitySeam`). The fourth run of
  2026-11-12 holds all three lines on Fabric, Quilt, NeoForge and Forge.
- **Forge-family client stage (fixed and run 2026-11-12).** NeoForge and Forge post their client
  registration events while `Minecraft` is constructed, before `FMLCommonSetupEvent` and
  `FMLClientSetupEvent`, so the client stage waited too long: the third run of 2026-11-12 crashed with
  `NullPointerException … "entityrenderer" is null` at `EntityRenderDispatcher.shouldRender`. The client
  stage now runs at the end of the common stage, inside the first `RegisterEvent`, and a client
  registration keeps an id that the loader resolves in its own event (`ClientHooks.entityRenderer`,
  `ClientHooks.blockModels`, `ClientRegistry`); docs/07 §7.8 records the decision.
  The first run of that fix failed the Forge client on two lines and both causes are fixed: the HUD layer
  is appended with `ForgeLayeredDraw.add(id, layer)` (Forge 52 has no `minecraft:hotbar` layer to anchor
  on), and the generated Forge metadata carries `MixinConfigs`, so a development run applies the config
  and `client.ParticleEngineMixin` patches the hit particles. The fourth run of 2026-11-12 is green on all
  eight smokes and the Forge client log holds seven `Mixing` lines (docs/07 §7.10).
  **Open note:** the Forge client log holds one suppressed error, `Failed to handle packet …
  ServerboundPlayerActionPacket … Can not retrieve LootModifierManager until resources have loaded once.`;
  the block still broke and the scenario completed. No action is set until the owner decides.
- **The locator service file is jar-only (2026-11-12).** A copy in `platforms/forge/common/src/main/resources`
  made the Forge development run lose Modularity: FML reads a class-path entry that names a locator service
  as a transformer artifact, so the main output stopped being a mod and the smoke failed with
  `Mod 'modularity_smoke' requires 'modularity' … not installed`. The `forgeLocatorService` task writes the
  file into `build/generated/forge-locator` and only the `jar` task takes it (docs/07 §7.8, §7.10). The
  Forge jar of 2026-11-12 predates the task, so one `:platforms:<version>:jar` run must confirm the entry.

### Stage 1 — loom

All nine beans hold an audit note from 2026-11-11 (the files, what is shared and what the port still
needs). Fabric and Quilt share `loom`, so most of the stage is already in the tree. The 2026-11-11 run
pass proves the stage in development: the Fabric and the Quilt server and client smokes pass.

- [x] `modularity-pnv1` mod lifecycle and entrypoints: split `ModularityLoomEntrypoints`. The split is in the tree
      (`ModularityLoomCommon`, `ModularityLoomClient`, `ModularityLoomServer`, `ModularityLoomLaunch`, `ModularityLoomServices`) and the 2026-11-12 run
      is green on Fabric and Quilt, whose smoke logs hold the launch, common and side stage lines that the
      check requires (docs/07 §7.10).
- [x] `modularity-xt51` detection and the log line: the 2026-11-12 logs hold it on all four loaders.
- [x] `modularity-8yym` content registration: both `Smoke features:` lines, `present=true`.
- [x] `modularity-7kd6` networking: the 2026-11-12 client smokes of all four loaders log the server and
      the client side of the round trip (docs/07 §7.10).
- [x] `modularity-dgzs` storage: the unit conversion, the portable view, the held-item fill and the exposed
      tank, 2026-11-12 (docs/07 §7.10).
- [ ] `modularity-dga9` client features. Proved: a rendered frame. Written and awaiting a run: the
      model, HUD and key mapping lines of the client smoke. Open: the dynamic block model quads, the entity
      renderer and the hit particles.
- [x] `modularity-bggb` mixins and the generated config: the Fabric and Quilt smokes hold no mixin error.
      The shipped-jar half is `modularity-xbj0`.
- [x] `modularity-p999` guest mods on Fabric. The 2026-11-12 Fabric server and client smokes are green
      and the check requires `Loaded guest mod smoke_guest` and `Guest mod smoke_guest: loaded=true
      initialized=true` (docs/07 §7.10), so the guest route works in the development run. The shipped-jar
      case is `modularity-xbj0`.
- [x] `modularity-l2mx` verification: `-p build-logic test` passes (57 tests, 2026-11-12) and the 21.1
      Fabric and Quilt server and client smokes pass with the strengthened check.

### Stage 2 — quilt

- [x] `modularity-gng8` every loom area on Quilt, including the guest decision (docs/08, bean
      `modularity-nwv9`). The 2026-11-12 Quilt server and client smokes pass with the feature lines and no
      guest line.

### Stage 3 — anvil

- [x] `modularity-arii` create the shared Forge-family root. In the tree: `anvil` with the stage order,
      the seam holder and the reflective detection. The 2026-11-12 NeoForge and Forge server and client
      smokes pass with the feature lines, so the layer runs on both loaders.

### Stage 4 — neoforge

- [x] `SmokeCheck` now expects the entrypoint stage lines for every loader (`Entrypoint launch/common
      active (<loader>)`, plus `mod-constructor` on the Forge family, plus the side line). The nine
      `SmokeCheckTest` cases pass under `javac` with a plain runner, and the 2026-11-11 run pass
      confirms that the real logs of all eight smokes hold the lines. See docs/07 §7.6 and §7.10.

- [x] Proved for this stage (2026-11-12): the common stage runs on the first `RegisterEvent`, and the
      NeoForge server and client smokes log both `Smoke features:` lines with `present=true`
      (`modularity-4uol`). The first attempt on `NewRegistryEvent` failed with `Registry is already frozen`;
      that failure is the record behind the decision in docs/07 §7.8.

In the tree, `javac` exit 0 on the anvil and neoforge sources:
`ModularityNeoForgeCommon`/`Client`/`Server` call `ModularityAnvilLifecycle`; `NeoForgePayloads`,
`NeoForgeLoaderHooks` (registration queue + `RegisterEvent` replay + creative tabs),
`NeoForgeClientHooks` (key mappings, payloads, entity renderers, HUD), `NeoForgeStorageBackend` with
`NeoForgeTankHandler` (entity capabilities, millibuckets, `FluidUtil.tryEmptyContainerAndStow`) and
`NeoForgeRenderData`. `neoforge.mods.toml` now declares `[[mixins]]` and the mixin config is written.
The run pass of 2026-11-11 is recorded below.

- [x] NeoForge lifecycle `0m6q`, detection `dom8`, registration `d5hk`, mixins `9rzu` and verification
      `bbk3` are closed by the 2026-11-12 run. Guests `jf4i` stay not supported (docs/08, epic
      `modularity-ecjq`). Storage `gz97` and client `lf0u` are closed by the runs of 2026-11-12, as are the
      fluid unit, the rendered frame and the networking round trip.
- [x] NeoForge server and client smoke pass (2026-11-11): `smoke SERVER 1.21.1: ok` and
      `smoke CLIENT 1.21.1: ok` for `:platforms:21.1:21.1-neoforge`. The log holds
      `(server) platform neoforge:21.1.265, minecraft:1.21.1`, `(client) platform neoforge:…`,
      `Client joined world`, `Screenshot: Saved screenshot as smoke.png`, `Client done`.
- [ ] NeoForge dynamic block models are implemented and compile, but no run has rendered them:
      `NeoForgeDynamicBakedModel` resolves the emitted states in `getModelData` and returns their quads. The item
      form of such a block renders nothing. A client smoke must confirm it.
- [x] Mixins on NeoForge apply (2026-11-11): the server and client smokes show no attach error, so FML reads
      `modularity.neoforge.mixins.json`. The shared layer is unchanged: the first failure had one cause, and
      it was in the build, not in the mixins.
      - A development run could not see the API at all: ModDevGradle leaves project dependencies off the launch
        class path and only adds registered mods to the mod folders, so the nested `modularity-api` was absent.
        The mixins, written against the API, then failed at attach time. `NeoForgePlatformSetup` now adds the
        nested modules to each run's additional runtime class path (docs/07 §7.8). With that fix the original
        shared mixins and `Interceptors` apply as written; they are restored to their committed form and the
        four loaders pass again.

### Stage 5 — forge

In the tree, `javac --release 21` exit 0 on 97 sources from `platforms/minecraft/common`,
`platforms/common/common`, `platforms/anvil/common` and `platforms/forge/common`:
`ModularityForge` runs the detection and the launch stage in the `@Mod` constructor, runs `launchMods` and
the common stage on the first `RegisterEvent`, then registers the client or the server entrypoint for the
distribution. `ForgeLoaderInfo`, `ForgeRenderData`,
`ForgeStorageBackend` with `ForgeTankHandler` and `ForgeEntityStorages`, `ForgeLoaderHooks` (queue +
`RegisterEvent` replay + creative tabs), `ForgePayloads` (one payload channel built at common setup)
and `ForgeClientHooks` (key mappings, entity renderers, reload listeners, client tick, HUD layers)
mirror the NeoForge seams. The run pass of 2026-11-11 is recorded below.

- [x] Proved for this stage (2026-11-12): same change as Stage 4 (`modularity-4uol`). The Forge server and
      client smokes log both `Smoke features:` lines with `present=true`, and the stage order
      (`mod-constructor`, `launch`, `common`) is in `build/smoke/server.log`.
- [x] Forge lifecycle `05um`, detection `j2w5`, registration `dti9` and verification `h5vz` are closed by
      the 2026-11-12 run. Storage `tll2`, client `hcru` and mixins `6rm0` are closed by the fourth and
      sixth runs of 2026-11-12; open: the shipped jar (`modularity-xbj0`) and guests `4l2a`
      (epic `modularity-ecjq`).
- [ ] Forge dynamic block models are implemented and compile, but no run has rendered them:
      `ForgeDynamicBakedModel` resolves the emitted states in `getModelData` and returns their quads. The item form
      of such a block renders nothing. Risk recorded in bean `hcru`.
- [ ] Mixins on Forge: required, not optional (owner, 2026-11-12). The earlier "stated limit" reading was
      wrong in one respect, and the reversal of 2026-11-12 was wrong in another: ModLauncher does pass the
      GAME layer's jars to Mixin, but FML adds a `mods/` jar to that layer only when the jar provides
      `ITransformationService`, `IModLocator` or `IDependencyLocator` (`ModDirTransformerDiscoverer.SERVICES`).
      A Modularity jar provides none, so the `MixinConfigs` attribute of the Forge jar is not read. The
      2026-11-12 Forge server and client smokes are green and the log holds Mixin 0.8.7 with no `Mixing`
      line. The jar now provides a locator service: `ModularityForgeLocator` claims no mod and is declared in
      `META-INF/services/net.minecraftforge.forgespi.locating.IDependencyLocator`, which
      `forgeLocatorService` writes for the `jar` task alone. A development run cannot use it; only a
      shipped-jar run proves the config applies (`modularity-xbj0`). The file in the built jar is not yet
      inspected: the Forge jar predates the task.
- [x] Forge server and client smoke pass (2026-11-11): `smoke SERVER 1.21.1: ok` with
      `(server) platform forge:52.1.21, minecraft:1.21.1` and `smoke CLIENT 1.21.1: ok` with
      `Client joined world` and the screenshot. This proves the entry points, the payload channel and the
      capability handlers in a development run. The run of 2026-11-12 repeats it with the `MixinConfigs`
      wiring in place and still holds no `Mixing` line, which is the evidence for the locator-service row
      above.

## NeoForge/Forge lifecycle — decisions moved to beans

The risks are now decisions and checks inside the tracker; they are no longer tracked here.

- Content registration timing → `modularity-d5hk` (NeoForge) and `modularity-dti9` (Forge).
- Registry location vs universal-jar relocation → recorded in `modularity-ibbd`.
- NeoForge dist classes, parallel dispatch, entry-point log lines → recorded in `modularity-r133`.
- Forge constructor injection, event-bus mismatch → recorded in `modularity-g420`.
- Lazy `LoaderInfo` → recorded in `modularity-co2j` and `modularity-v53p`.
- The runtime checks run with `modularity-8ywo` and `modularity-lw0r`.

## Quilt/Fabric separation (`loom`, `common`, native Quilt jar) — open checks

The loader facts and accepted costs are decisions in docs/07 §7.8; the refmap work stays in
`modularity-vgs5` and `modularity-xbj0`. Tick a box only when the matching check passes.

- [ ] **Two metadata files in the universal jar.** Quilt's plugin must accept `quilt.mod.json`
      before Fabric reads `fabric.mod.json`; the shipped-jar smoke still must prove it.
