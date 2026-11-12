# Port review

One section per port stage, so each stage can be reviewed on its own. Fabric is the reference. The shared layers
(`platforms/minecraft`, `platforms/common`) hold the game-facing code; a port adds loader-side code in its own root
only.

State on 2026-11-12: every stage below has code in the working tree, and the eight 21.1 smokes (server and
client) pass for all four loaders with the strengthened `SmokeCheck`. The third pass of the day also proves
the payload round trip, the loader's fluid unit and a rendered frame, and `-p build-logic test` is green
(57 tests). The four per-loader jars and the universal jar also build. The new client-side smoke code
(`SmokeFeatures` client stage, `SmokeScenario`) compiles outside Gradle for 1.21.1, 1.21.10 and 1.21.11; the
26.x lines cannot be compiled offline for lack of Fabric API jars. The fourth pass fixed the Forge-family
client stage: NeoForge and Forge post the client registration events while the game object is constructed,
so the client stage now runs at the end of the common stage, inside the first `RegisterEvent`, and every
client registration keeps an `Id` that the loader resolves in its own event (docs/07 §7.8). Its run of
2026-11-12 10:52 is green on all eight 21.1 smokes (`BUILD SUCCESSFUL in 2m 58s`): the dynamic model, the
hit particles, the HUD layer, the key mapping and the entity renderer run on Fabric, Quilt, NeoForge and
Forge, and the Forge client log holds seven `Mixing` lines, so Mixin applies Modularity's Forge config in a
development run (docs/07 §7.10). The five branches
`feature/port-<platform>` exist and all point at `5daf9d7`. The port work is uncommitted, because AGENTS.md
hard rule 2 holds until the owner allows a commit. One branch per stage becomes reviewable as one review
when the owner allows the commit that puts that stage's files on it.

## Reviewing one stage without a commit

Every stage owns a disjoint directory, so one stage is one path set and one diff. `git diff` shows only
tracked files, and a port adds files, so read `git status --short` with the diff. Run one line, read one
stage; nothing else appears:

| Stage | Review command |
|---|---|
| 1 loom | `git status --short -- platforms/loom platforms/fabric && git diff --stat -- platforms/loom platforms/fabric` |
| 2 quilt | `git status --short -- platforms/quilt && git diff --stat -- platforms/quilt` |
| 3 anvil | `git status --short -- platforms/anvil && git diff --stat -- platforms/anvil` |
| 4 neoforge | `git status --short -- platforms/neoforge && git diff --stat -- platforms/neoforge` |
| 5 forge | `git status --short -- platforms/forge && git diff --stat -- platforms/forge` |
| shared, must not change | `git status --short -- platforms/minecraft platforms/common modularity-api` (expected empty) |

The `git status --short` half lists the new files (`??`) under that stage's root; the `git diff` half shows
the edits and the deletions. Use `git diff --stat HEAD -- <paths>`, the same form, when the stage is
staged.

`build-logic`, `modularity-smoke` and `docs` hold the build, the feature smoke and the records; they belong to
no single stage and are reviewed once. The five `feature/port-<platform>` branches exist at `5daf9d7`, so a
stage becomes a commit on its branch as soon as the owner allows it.

## Stage 1 — loom (Fabric and Quilt share this root)

| File | Role |
|---|---|
| `loom/entrypoint/ModularityLoomCommon.java` | common stage: entry points, seams, synced data, detection line, mod init |
| `loom/entrypoint/ModularityLoomClient.java` | client stage: client seams, client init |
| `loom/entrypoint/ModularityLoomServer.java` | server stage: server init |
| `loom/entrypoint/ModularityLoomLaunch.java` | launch stage and the per-mod launch init |
| `loom/entrypoint/ModularityLoomSeams.java` | installs the detection sources and the four seams |
| `loom/loader/Loom{LoaderInfo,LoaderHooks,RenderData,StorageBackend,TankStorage}.java` | the seam implementations |
| `loom/client/{ModularityLoomClientHooks,DynamicBakedModel,DynamicUnbakedModel,HudHook,ObjModelReloadListener}.java` | the client seam, dynamic models, HUD, OBJ cache |
| `loom/compat/{BlockViews,CreativeTabs,KeyMappings,Payloads,Storages}.java` | loader adapters over shared types |
| `loom/detect/{ModularityLoomPlatformSource,ModularityLoomVersionSource,ModularityLoomClasses}.java` | detection sources |
| `loom/guest/GuestCandidates.java` | guest discovery |
| `loom/platform/compat/RenderDataHolder.java` | replaces the shared file to extend Fabric's `RenderDataBlockEntity` |
| `loom/changes/26.1+/platform/compat/RenderDataHolder.java` | the same file where 26.1 renamed the Fabric API |

Removed: `loom/entrypoint/ModularityLoomEntrypoints.java`, replaced by the five classes above.

Closes when: `-p build-logic test`; the 21.1 Fabric and Quilt `smokeServer` and `smokeClient` pass.

## Stage 2 — quilt

| File | Role |
|---|---|
| `quilt/entrypoint/ModularityQuiltCommon.java` | calls `ModularityLoomCommon` |
| `quilt/entrypoint/ModularityQuiltClient.java` | calls `ModularityLoomClient` |
| `quilt/entrypoint/ModularityQuiltServer.java` | calls `ModularityLoomServer` |
| `quilt/entrypoint/ModularityQuiltLaunch.java` | calls `ModularityLoomLaunch`; reports guests and does not load them (docs/08) |

Closes when: the 21.1 Quilt `smokeServer` and `smokeClient` pass, and the log shows no guest line.

## Stage 3 — anvil (shared by NeoForge and Forge)

| File | Role |
|---|---|
| `anvil/ModularityAnvilLifecycle.java` | stage order: detect, launch, launchMods, common, client, server |
| `anvil/ModularityAnvilSeams.java` | the four seams of a Forge-family loader |
| `anvil/detect/ModularityAnvilLoaderApi.java` | reflective read of the loader API, so `anvil` names no loader type |
| `anvil/detect/ModularityAnvilPlatformSource.java` | loader, version, side, development |
| `anvil/detect/ModularityAnvilVersionSource.java` | Minecraft version |

Removed: `neoforge/detect/NeoForge{Platform,Version}Source.java` and `forge/detect/Forge{Platform,Version}Source.java`;
their work moved here.

Closes when: the layer compiles with neither loader on the class path (done, `javac` exit 0) and
`-p build-logic test` shows `anvil` in the NeoForge and Forge layer stacks.

## Stage 4 — neoforge

| File | Role |
|---|---|
| `neoforge/entrypoint/ModularityNeoForgeCommon.java` | `@Mod` constructor: detect and launch; `launchMods` and common once on the first `RegisterEvent`, then `replay` of that registry |
| `neoforge/entrypoint/ModularityNeoForgeClient.java` | client stage in `enqueueWork` of the client setup event |
| `neoforge/entrypoint/ModularityNeoForgeServer.java` | server stage in `enqueueWork` of the server setup event |
| `neoforge/loader/NeoForgeLoaderHooks.java` | registration queue, `RegisterEvent` replay, creative tabs |
| `neoforge/loader/NeoForgeLoaderInfo.java` | the loader info seam |
| `neoforge/loader/NeoForgeStorageBackend.java`, `NeoForgeTankHandler.java` | storage, millibuckets, entity tank |
| `neoforge/loader/NeoForgeRenderData.java` | render data seam |
| `neoforge/client/NeoForgeClientHooks.java` | key mappings, payloads, entity renderers, HUD, tick, reload |
| `neoforge/client/NeoForgeDynamicBakedModel.java` | dynamic models through `getModelData` and `getQuads` |
| `neoforge/network/NeoForgePayloads.java` | one payload channel built at common setup |

Closes when: `-p build-logic test`; the 21.1 NeoForge `smokeServer` and `smokeClient` pass; a client smoke renders the
HUD layer and a dynamic-model block; the log shows no mixin error.

## Stage 5 — forge

| File | Role |
|---|---|
| `forge/ModularityForge.java` | `@Mod` constructor: detect and launch, then the side entrypoint; `launchMods` and common once on the first `RegisterEvent`, then `replay` of that registry |
| `forge/entrypoint/ModularityForgeClient.java` | client stage in `enqueueWork` of the client setup event |
| `forge/entrypoint/ModularityForgeServer.java` | server stage in `enqueueWork` of the server setup event |
| `forge/loader/ForgeLoaderHooks.java` | registration queue, `RegisterEvent` replay, creative tabs |
| `forge/loader/ForgeLoaderInfo.java` | the loader info seam |
| `forge/loader/ForgeStorageBackend.java`, `ForgeTankHandler.java`, `ForgeEntityStorages.java` | storage and item inventories |
| `forge/loader/ForgeRenderData.java` | render data seam |
| `forge/client/ForgeClientHooks.java` | key mappings, payloads, entity renderers, HUD, tick, reload |
| `forge/client/ForgeDynamicBakedModel.java` | dynamic models through `getModelData` and `getQuads` |
| `forge/network/ForgePayloads.java` | one payload channel built at common setup |

Closes when: `-p build-logic test`; the 21.1 Forge `smokeServer` and `smokeClient` pass; a client smoke renders the HUD
layer and a dynamic-model block.

## Runs of 2026-11-11

The owner authorized one run pass. Every item below ran and its result is in docs/07 §7.10. A second, final
pass then ran the same eight smokes with the strengthened `SmokeCheck` (entrypoint stage lines) and with
`platforms/minecraft` byte-identical to `HEAD`; all eight pass:

| Command | Result |
|---|---|
| `./gradlew -p build-logic test` | BUILD SUCCESSFUL |
| `:platforms:21.1:21.1-fabric:smokeServer` / `smokeClient` | both ok |
| `:platforms:21.1:21.1-quilt:smokeServer` / `smokeClient` | both ok |
| `:platforms:21.1:21.1-neoforge:smokeServer` / `smokeClient` | both ok |
| `:platforms:21.1:21.1-forge:smokeServer` / `smokeClient` | both ok |

One defect came out of the NeoForge run and is fixed: the nested API was absent from the development
run class path, so the mixins failed at attach time (fixed in `NeoForgePlatformSetup`, docs/07 §7.8).
The shared layer stayed unchanged; after the fix the original mixins and `Interceptors` apply as
written, and the Fabric, Quilt, NeoForge and Forge runs all pass again.

## Runs of 2026-11-12

The owner authorized the feature-smoke pass. `-p build-logic test` is green (50 tests). The first pass of the
eight 21.1 smokes failed on NeoForge and Forge: `NewRegistryEvent` is posted before `GameData.unfreezeData`,
so `Block.<init>` threw `Registry is already frozen`. The common stage moved to the first `RegisterEvent`
(docs/07 §7.8); the second pass is green:

| Command | Result |
|---|---|
| `./gradlew -p build-logic test` | BUILD SUCCESSFUL, 50 tests, 0 failures |
| the eight 21.1 smokes, first pass | Fabric and Quilt pass; NeoForge and Forge fail with `missing log line /Smoke features: declared …/` |
| the eight 21.1 smokes, after the fix | BUILD SUCCESSFUL in 4m 14s; all eight pass, every server and client log of the four loaders holds both `Smoke features:` lines with `present=true` |
| `:platforms:21.1:21.1-{fabric,quilt,neoforge,forge}:build` | BUILD SUCCESSFUL in 9s; the four per-loader jars exist |
| `:platforms:21.1:jar` | BUILD SUCCESSFUL; the universal jar (1,141,046 bytes) holds the five metadata files, the four mixin configs and both nested-API routes |

## Third pass of 2026-11-12

The owner authorized one pass of the tests and the eight smokes after the three new in-game checks were
added: the C2S→S2C payload round trip, the fluid unit of the loader, and the rendered-frame check on the
client screenshot. `-p build-logic test` is green with 57 tests. The Forge smoke then failed, because a
locator service file on the development class path makes FML drop the mod; the file is now generated for
the `jar` task alone (`ServiceFileTask`, docs/07 §7.5 and §7.8). After that fix every smoke passes:

| Command | Result |
|---|---|
| `./gradlew -p build-logic test` | BUILD SUCCESSFUL, 57 tests, 0 failures |
| the eight 21.1 smokes, first pass | seven pass; Forge fails with `Mod 'modularity_smoke' requires 'modularity' … is not installed` |
| the eight 21.1 smokes, after the fix | BUILD SUCCESSFUL in 2m 58s; all eight pass, every log holds `Smoke storage:` with the loader's own `unitsPerMb`, and every client log holds both `Smoke network:` lines |

## Checks at the end of each stage

What ran and what did not, per stage. Only the listed commands ran; a blank right-hand cell means nothing is
outstanding for that stage.

| Stage | Ran | Did not run |
|---|---|---|
| 1 loom | `-p build-logic test` (57 tests, 2026-11-12); 21.1 Fabric and Quilt `smokeServer` + `smokeClient` with the payload round trip, the fluid unit and the frame check; the two platform builds | the six client lines of `dga9` (written, not run: model, HUD, key mapping, placement, quads, hit particles); the entity renderer; the held-item fill and the exposed tank (`modularity-dgzs`); a load from the shipped jar |
| 2 quilt | 21.1 Quilt `smokeServer` + `smokeClient` with no guest line, as docs/08 requires; `:platforms:21.1:21.1-quilt:build` | the six client lines of `yrlm` (written, not run); the entity renderer; the held-item fill and the exposed tank (`modularity-mizg`); guest injection, which epic `modularity-ecjq` owns (docs/08) |
| 3 anvil | `javac` of the `anvil` layer with no loader on the class path (exit 0); `-p build-logic test` shows `anvil` in the NeoForge and Forge layer stacks | nothing of its own; its effects are proved through stages 4 and 5 |
| 4 neoforge | 21.1 NeoForge `smokeServer` + `smokeClient` with the payload round trip, the fluid unit and the frame; the mixins apply and `Interceptors` install (docs/07 §7.8); `:platforms:21.1:21.1-neoforge:build` | the six client lines of `lf0u` (written, not run); the entity renderer; the held-item fill and the exposed tank (`modularity-gz97`); a load from the shipped jar |
| 5 forge | 21.1 Forge `smokeServer` + `smokeClient` (green 2026-11-12, third pass, with the payload round trip, the fluid unit and the frame); `:platforms:21.1:21.1-forge:build`; the jar manifest and its jar-in-jar metadata | the six client lines of `hcru` (written, not run; a green hit-particle line would also prove the Forge mixins); the entity renderer; the held-item fill and the exposed tank (`modularity-tll2`); the mixins: the jar names the config in its manifest and now provides the locator service, but Mixin reads neither in a development run (docs/07 §7.8, bean `modularity-6rm0`); the service entry in the built jar; a load from the shipped jar |
| all | `:platforms:21.1:jar` (universal jar) | `smokeAll`; `allureAggregateReport`; a client start from the jar |

## Next run (needs the owner's word — AGENTS.md hard rule 1)

Two lines are done; the third needs a word from the owner. Each line names what it closes or proves.

| Command | Closes or proves |
|---|---|
| `./gradlew -p build-logic test` | DONE 2026-11-12: BUILD SUCCESSFUL, 57 tests, 0 failures |
| the same command | DONE 2026-11-12: BUILD SUCCESSFUL in 2m 58s, all eight smokes green with the network round trip, the fluid unit and the frame check. Closed the four networking beans (`7kd6`, `koa2`, `ra61`, `w922`) and the unit and frame halves of the storage and client beans |
| the eight 21.1 smokes again | **green on 2026-11-12 10:52** (`BUILD SUCCESSFUL in 2m 58s`): every client log holds `Smoke model: registered for …`, `Smoke client: hud drawn …`, `Smoke keys: binding=…`, `Smoke scenario: placed …`, `Smoke model: quads at …`, `Smoke particles: hit …` and `Smoke entity: rendered …`, and every client run saves a screenshot. Closed `dga9`, `yrlm`, `lf0u`, `hcru` (docs/07 §7.10) |
| the eight 21.1 smokes, sixth pass | **green on 2026-11-12** (`BUILD SUCCESSFUL in 2m 53s`): every client log holds `Smoke storage: held item moved 1000 mB` and `Smoke storage: exposed 500 mB`, and the Forge screenshot shows the emptied bucket in the hotbar. Closed `dgzs`, `mizg`, `gz97`, `tll2` (docs/07 §7.6, §7.10) |
| `./gradlew :platforms:21.1:jar` then a client start from that jar | open: the `META-INF/services` entry in the built Forge jar (the jar on disk predates `forgeLocatorService`), `MixinConfigs` read by Mixin (`6rm0`), the nested API and the relocated tree (`xbj0`) — no task starts the game from a jar yet |

## Parity against Fabric

The goal is one feature set on five loaders. Three Fabric capabilities are still not reached everywhere. Each
one is a named limit or an open check, not a silent gap:

| Capability | Delta | Where it is recorded |
|---|---|---|
| Mixins in a production jar | Fabric, Quilt, NeoForge and Forge now apply their config in a development run; the Forge route is the manifest of the mod directory, so the generated metadata carries `MixinConfigs` too. No run starts from a shipped jar, and the universal jar needs one loader-conditional attribute | PR-13, docs/07 §7.8 and §7.10, beans `modularity-6rm0`, `modularity-xbj0` |
| Guest mods | only Fabric loads a guest; Quilt reports it and does not load it; the Forge family has no public route. Closed as a limit for this port: epic `modularity-ecjq` owns it after Stage 5 | docs/08, epic `modularity-ecjq`, beans `modularity-73sm`, `modularity-zbbp`, `modularity-4l2a`, evidence in `modularity-4bm8` |
| Client seam in the game | proved on all four loaders on 2026-11-12: the client smoke registers a per-position model, a HUD layer, a key mapping, the hit-particle handler and an entity renderer, places the block, mines it and kills the spawned entity, and parses and draws an OBJ cube; the screenshot shows the stone-textured cube. Forge needed two fixes (§7.8): the layer is appended to the overlay stack, and the generated metadata carries `MixinConfigs` | PR-8, docs/07 §7.10, beans `modularity-dga9`, `modularity-yrlm`, `modularity-lf0u`, `modularity-hcru` |
| A load from the shipped jar | none of the five | bean `modularity-xbj0` |

The storage seam is no longer a delta: the held-item fill, the drain back into a bucket, the refused second
pickup and the exposed entity tank are proved on all four loaders by the seventh run of 2026-11-12
(docs/07 §7.6, §7.10), and the four storage beans are closed.


The traceability matrix of 2026-11-12 gives Fabric 95, Quilt 91, NeoForge 90 and Forge 81. Three rows stay
open: the guest route, the mixins of a shipped jar and a load from the shipped jar. Bean
`modularity-ab6r` records the choice of what to open next; until the owner answers it, no code goes to
those rows. The client seam and the
Forge development mixins closed in the fourth pass of 2026-11-12, so the Forge delta of that matrix is stale.

## Unproven in every stage

| Item | Why it is open |
|---|---|
| Dynamic-model rendering on every loader | the smoke places the block and the model logs `Smoke model: quads at …` on all four loaders (2026-11-12), and every client run saves a screenshot that the check reads. A human look at the frame showing the block itself is still missing |
| The shipped jar and its jar-in-jar load | the universal jar builds and its contents are inspected (docs/07 §7.10), but every smoke run loads the mod from its classes directory (`nested=no`), so no run has started the game from a jar |
| Forge mixins in a shipped jar | a development run now applies the config (seven `Mixing` lines, 2026-11-12). The jar route is unproven: FML adds a `mods/` jar to ModLauncher only when the jar provides a locator service, the jar provides `IDependencyLocator` (`ModularityForgeLocator`), and no run starts from a jar. The universal jar needs a loader-conditional registration as well (beans `modularity-xbj0`) |
| Guests on Quilt and the Forge family | loader limit, docs/08 |
| `allureAggregateReport` | not run in the 2026-11-11 or the 2026-11-12 pass |


Record every run in docs/07 §7.10 and tick the matching box in TODO-NOW.md.
