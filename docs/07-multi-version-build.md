# 7. Multi-version build (Fabric)

Modularity ships one Fabric platform jar per Minecraft version, from 1.21.1 to the latest stable
release. The code is written once and layered: Fabric code common to every version, then only the
changes per version range, then the few details of a single version. Our own Gradle plugin
(`lv.cebbys.modularity`, in `build-logic/`) assembles every platform from these layers.

## 7.1 Versions

Source of truth: `gradle/platforms.toml`. Values come from Fabric's metadata
(`meta.fabricmc.net/v2/versions/game`, `.../loader`), Fabric's Maven (`fabric-api` metadata, newest
build per exact Minecraft version) and Mojang's version manifest (`javaVersion.majorVersion`).
Checked on 2026-11-06.

| Platform | Minecraft | Fabric API | Java | Loom plugin |
|---|---|---|---|---|
| fabric-21.1 | 1.21.1 | 0.116.17+1.21.1 | 21 | remap (Mojang mappings) |
| fabric-21.2 | 1.21.2 | 0.106.1+1.21.2 | 21 | remap |
| fabric-21.3 | 1.21.3 | 0.114.1+1.21.3 | 21 | remap |
| fabric-21.4 | 1.21.4 | 0.119.4+1.21.4 | 21 | remap |
| fabric-21.5 | 1.21.5 | 0.128.2+1.21.5 | 21 | remap |
| fabric-21.6 | 1.21.6 | 0.128.2+1.21.6 | 21 | remap |
| fabric-21.7 | 1.21.7 | 0.129.0+1.21.7 | 21 | remap |
| fabric-21.8 | 1.21.8 | 0.136.1+1.21.8 | 21 | remap |
| fabric-21.9 | 1.21.9 | 0.134.1+1.21.9 | 21 | remap |
| fabric-21.10 | 1.21.10 | 0.138.4+1.21.10 | 21 | remap |
| fabric-21.11 | 1.21.11 | 0.141.6+1.21.11 | 21 | remap |
| fabric-26.1 | 26.1 | 0.145.1+26.1 | 25 | no remap (unobfuscated) |
| fabric-26.1.1 | 26.1.1 | 0.145.4+26.1.1 | 25 | no remap |
| fabric-26.1.2 | 26.1.2 | 0.155.3+26.1.2 | 25 | no remap |
| fabric-26.2 | 26.2 | 0.161.0+26.2 | 25 | no remap |
| fabric-26.3 | 26.3 | 0.165.2+26.3 | 25 | no remap |

Fabric Loader: 0.20.1 (latest stable) for every platform.

## 7.2 Layout

```
gradle/platforms.toml                      version table: one entry per loader and Minecraft version
build-logic/                               included build: the lv.cebbys.modularity plugins
platforms/
  minecraft/                               plain Minecraft code shared by every loader (see 7.3)
  fabric/ | neoforge/ | forge/ | quilt/    one layer root per loader, each with:
    common/src/main/{java,resources}         code shared by every version of the loader
    changes/<range>/src/main/{java,resources} one change, at the first version that needs it
    versions/<version>/                      project dir of platform <loader>-<version>;
      src/main/{java,resources}              only truly single-version details
tools/smoke/fabric | neoforge | forge      smoke-test mods (same layering, see 7.6); tools/smoke/guest
```

Range folder names: `1.21.5+` (that version and every later one) or `1.21.5-1.21.8` (inclusive).

Platforms are `<loader>-<version>` (`fabric-21.1`, `neoforge-21.1`), projects `:platforms:<loader>-<version>`.
`-Pmodularity.platforms=fabric-21.1,neoforge-21.1` selects platforms by id, a bare version (`21.1`) every
loader's platform of that version.

Each loader assembles a stack of layer roots, lowest first:

| Loader | Layer roots | Own jar |
|---|---|---|
| fabric | `minecraft`, `fabric` | yes |
| quilt | `minecraft`, `fabric`, `quilt` | no: runs the Fabric jar's code on Quilt Loader, to verify it there |
| neoforge | `minecraft`, `neoforge` | yes |
| forge | `minecraft`, `forge` | yes |

## 7.3 Layering rules

For platform version V the sources of each layer root are, lowest to highest (roots stack in the order of 7.2):

1. `common`
2. every `changes/<range>` that contains V, ordered by range start (then by range end, open last)
3. `versions/<V>`

A file in a higher layer replaces the file with the same relative path below it. A file without a
counterpart below is an addition. A file `X.removed` (empty) removes `X` coming from below.

The plugin fails the build when:

* a range folder name cannot be parsed, or two ranges are identical;
* a layer contains a file identical to the one it replaces (a copy instead of a change);
* a `.removed` marker names a file that does not exist below.

Rules for authors: put a change at the first version that needs it, as an open range (`X+`). Close a
range (`X-Y`) only when a later version changes the same file again, and then add `Y'+` for the new
form. Never copy common code into a version folder.

## 7.4 Plugin responsibilities

Two plugins in `build-logic/`:

* `lv.cebbys.modularity.settings` (settings plugin): reads `gradle/platforms.toml` and includes one
  project per platform, `:platforms:fabric-<version>` with project dir
  `platforms/fabric/versions/<version>`.
* `lv.cebbys.modularity` (project plugin), applied to each platform project:
  * applies Loom (`net.fabricmc.fabric-loom-remap` with Mojang mappings up to 1.21.11,
    `net.fabricmc.fabric-loom` without mappings from 26.1) and the Java target of the version;
  * assembles the layers into `build/layered/` (task `assembleLayers`) and compiles from there;
  * generates `fabric.mod.json` (exact Minecraft version, depends on `mvd` and `mpd`) and the mixin
    config from the mixin classes actually present after layering;
  * dependencies: Fabric Loader, Fabric API, `modularity-api` (nested), mvd and mpd (runtime, not
    nested);
  * tasks per platform: `runClient`, `runServer`, `smokeServer`, `smokeClient`;
  * root tasks: `buildAll`, `collectJars` (Sync into `build/libs-all`), `smokeAll`.

## 7.5 mvd and mpd

One jar each, compiled against 1.21.1 (Java 21 bytecode), declaring `"minecraft": ">=1.21.1"`. They
use only Fabric Loader API and no Minecraft classes, so the same jar loads on every version.

## 7.6 Smoke tests

The smoke mod lives in `tools/smoke/` and is layered like the platform. Each platform project gets a
`smoke` source set and two runs:

* `smokeServer`: dedicated server, flat world, stops itself after a few ticks; the task then checks
  the log: modularity, mvd and mpd loaded as top-level mods, mvd reports the exact version, mpd
  reports FABRIC.
* `smokeClient`: client joins the world written by the server run, takes a screenshot, quits; same
  log checks.

Every platform runs `smokeServer`; `smokeClient` runs for 21.1, 21.6, 21.11, 26.1 and the latest.

## 7.7 Risks

* One Loom version for the whole build: Loom 1.19 must build every version (it does: both plugins
  ship in one artifact; the mixed obfuscated/unobfuscated build bug, fabric-loom#1477, was fixed in
  1.15).
* Gradle must run on Java 25 for 26.x; 1.21.x targets Java 21 bytecode via toolchains.
* Big API jumps (1.21.2 items/recipes/render state, 1.21.4 item models, 1.21.5 data components and
  rendering, 1.21.6 HUD and rendering, 1.21.9, 26.1 names and render pipeline) need real porting
  work, not just recompilation.
* Compiling from `build/layered/` means compiler errors point at the layered copy; the error message
  includes the layer the file came from (`assembleLayers` writes `build/layered/origins.txt`).

## 7.8 Decisions

| Decision | Reason |
|---|---|
| Own plugin instead of Stonecutter | Matches the layered model (common + changes) instead of `//? if` comments; can grow to other loaders and to mods built on Modularity. |
| Loom 1.19.3, Gradle 9.8.2, Gradle on JDK 25 | Newest stable Loom; ships both the remap and the non-remap plugin; 26.x requires Gradle on Java 25. |
| Fabric Loader 0.20.1 everywhere | Latest stable; supports every version in the table, so one loader version keeps the table simpler. |
| Newest Fabric API build per exact version | Most fixes; Fabric API builds are tied to one Minecraft version. |
| Layers assembled into `build/layered/` | Java has no per-file source override; copying the winning file of each path is simple, cacheable and keeps the source layers clean. |
| Additions allowed, only removals must point at an existing file | On disk an override and an addition look the same; requiring every file to exist below would forbid new classes in newer versions. |
| Identical copies fail the build | Enforces "only the changes" mechanically. |
| Project dir = `platforms/fabric/versions/<version>` | The only per-version folder; created by the settings plugin if missing. |
| Loom loaded once in the root `plugins {}` (`apply false`), subprojects apply it without a version | One Loom classloader for the whole build; separate versions per project break Loom's shared services. |
| Utility folders pass their `gradle.properties` values to subprojects explicitly | Gradle 9 deprecates the implicit lookup in parent projects (an error in Gradle 10). |
| Smoke server without refabslab locally | The refabslab jar is only built in CI (`-PmodJar`); locally the slab placement lines report FAIL by design, the mod loading checks are what counts. |
| Three plugins: `.settings` (includes projects), `.root` (applies the platform plugin to every included platform, `buildAll`, `collectJars`, `smokeAll`), `lv.cebbys.modularity` (one platform) | Platform projects need no build file at all; the version folder only holds sources. |
| Mod metadata, nested module and runtime mods in `[mod]` of `platforms.toml` | The plugin stays free of Modularity paths and can later build mods on Modularity. |
| `enabled = false` per table entry until the version is ported; `-Pmodularity.platforms=21.1,26.3` selects platforms | All versions are recorded once; ports switch one flag; fast builds of a few platforms while porting. |
| Own small TOML reader (`MiniToml`) | The table uses a tiny subset; no extra plugin dependency or Gradle internal API. |
| Final smoke run one platform per Gradle call (daemon stopped in between) instead of one `smokeAll` | On the 32 GB dev machine a single `smokeAll` was killed for low system memory; the per-platform `smoke` tasks are the same checks. `smokeAll` stays the CI entry point. |
| Mod Menu in every platform's runs (`modmenu` per table entry, development only) | Shows the loaded mods, guest mods included, in every version; newest Mod Menu release per Minecraft version from Modrinth, checked on the Terraformers maven. |
| Platforms keyed by loader and version (`fabric-21.1`); one layer root per loader on top of a shared `minecraft` root | Most code is plain Minecraft code (60 of 81 Fabric files use no Fabric API); version changes in it are then made once for every loader. |
| Quilt runs the Fabric layers and publishes no jar | Quilt Loader runs Fabric mods and Fabric API; one Fabric jar serves both, the Quilt platform proves it. |
| Quilt through Fabric Loom with Quilt Loader as the loader dependency, no Quilt Loom | One Loom for the whole build. Loom reads loader libraries only from `fabric-installer.json`; the plugin adds the `common` and `development` libraries of Quilt Loader's own `quilt_installer.json`, copies Loom's `fabric.*` development properties in `launch.cfg` to Quilt's `loader.*` names, and sets Quilt's `KnotClient`/`KnotServer` main classes. |
| Quilt platforms compile against Fabric Loader (compile only) and exclude it at runtime | They compile exactly the Fabric jar's code (the guest loader uses Fabric Loader classes); at runtime only Quilt Loader may be present (both ship `net.fabricmc.loader.impl`). |
| `fabricloader >=0.16.0` in the Fabric jars (`fabric-loader-min`), built and run with 0.20.1 | Quilt Loader 0.30.2 reports Fabric Loader 0.19.5, so `>=0.20.1` refused the Fabric jar on Quilt. The loader API Modularity uses is old and stable; same floor as mvd and mpd. Only guest mods depend on 0.20.1 internals (docs/08). |
| Quilt, NeoForge and Forge only for 1.21.1 for now | Scope set by the project owner. Quilt was also tried on every other version: 1.21.2 to 1.21.11 passed on Quilt Loader 0.30.2; 26.x needs Quilt Loader 0.31.0-beta.6 (0.30.2 loads the mods there but never calls their entrypoints) and then passed too. |
| No guest mods on Quilt yet | The guest loader uses Fabric Loader internals that Quilt Loader does not have; on Quilt it logs an error and loads no guests. Quilt smoke tests run without the guest. |
| NeoForge with ModDevGradle 2.0.148, Forge with ForgeGradle 7.0.40, both on the shared plugin classpath next to Loom | The plugins and versions of NeoForge's and Forge's own 1.21.1 MDKs, both on Gradle 9. ModDevGradle's legacy Forge plugin cannot build Forge 1.21.1 and ForgeGradle 6 needs Gradle 8. All three plugins in one build were tried first in a throwaway build. |
| NeoForge/Forge metadata (`neoforge.mods.toml`, `mods.toml`) generated from the same `[mod]` values; Minecraft exact, the loader from its line on (`[21.1,)`, `[52,)`), `>=X` ranges as `[X,)` | One description of the mod for every loader. |
| `[mod] mods` lists the Fabric modules; NeoForge and Forge use their `-neoforge` / `-forge` sibling | The separate mods are the same mods per loader. |
| Smoke mods layered like the platforms (`tools/smoke/minecraft` + the loader's root) | The screenshot code is plain Minecraft code and its version changes apply to every loader. |
| NeoForge smoke source set: own configurations extending `main`'s, plus `addModdingDependenciesTo` | ModDevGradle adds its dev launcher to the source set's configurations; a plain file class path misses it. |
| Forge runs named `server` / `client`, smoke tasks `runSmokeServer` / `runSmokeClient` | ForgeGradle 7 fills a run from Forge's run configuration of the same name and registers one task per source set (`run<SourceSet><Name>`) with that source set's class path; a run's `classpath` option replaces the launcher's own class path. |
| Forge: resources compiled into the classes folder (`main`, `smoke`) | Forge's development discovery takes each class path folder as a mod file; `mods.toml` must sit next to the classes. |
| Forge jar-in-jar written by the plugin (`META-INF/jarjar` + `metadata.json`, same format as ModDevGradle) | ForgeGradle 7 has no jarJar; Forge 52 reads that format (`JarInJarDependencyLocator`). Loading the nested API from a production jar is still to be checked in a real Forge install. |
| `pack.mcmeta` (pack format 34) in the Forge layer, smoke mod and utility mods | Forge 1.21.1 warns for mods without it; the value is the one of Forge's own 1.21.1 MDK. |
| NeoForge/Forge utility mods: the bundled core is not also a runtime dependency | Both load mods as Java modules; the core's package may come from one module only (Forge refused the split package). |
| Forge smoke "nested" check by jar path (`META-INF/jarjar/`) | Forge's mod file API names no parent file (NeoForge's does). |
| `build-logic` compiled for Java 25 | Loom 1.19.3 is published as Java 25 bytecode, so plugins depending on it must be too. |
| Mixin config `compatibilityLevel` JAVA_21 for every platform | The mixins use no newer language features; one value every shipped Mixin knows. |
| Mixins listed from `@Mixin` classes in the assembled sources, `client.*` under `client` | A version layer can add or remove a mixin without editing a config. |
| Mixin names sorted as strings | Windows sorts paths case-insensitively; the generated config must be identical on every OS. |
| 21.1 move verified by jar diff | Every class in the 21.1 jar is byte-identical before and after the move; only `fabric.mod.json` (exact `minecraft`, `fabricloader >=0.20.1`) and the order of two client mixins changed. |
| `assembleLayers` inputs are only `*/src/<set>/**` of the layers | `versions/<v>` is also the project dir; its `build/` and `run/` must not be inputs. |
| Smoke tests built before the ports (goal step 7 moved before step 5) | Every port commit is verified with the same `smoke` task for that version and all earlier ones. |
| Smoke mod as source set `smoke` of each platform, own Loom runs in `build/smoke/run` | Same classpath and mappings as the platform; no second build, no maven round trip; each source set is its own mod (Loom `mods {}`). |
| Fresh flat world per server run; the client opens that world with `--quickPlaySingleplayer` | The client test needs no world creation UI; the world matches the version. |
| Log checks in `SmokeCheck` (unit tested): the 3 mods loaded and not nested, `[mvd] Detected version <exact>`, `[mpd] Detected platform FABRIC … <SIDE>`, end marker, no crash or failed mixin | The goal's verification, done mechanically; matched lines go to `build/smoke/{server,client}.txt`. |
| `tools/smoke-fabric-1.21.1` kept | It is the refabslab integration test (double slabs, KleeSlabs) in CI, not a platform test. |
| CI: JDK 21 and 25, `buildAll collectJars`, `smokeAll` under xvfb | Loom 1.19 needs Gradle on 25; 1.21.x toolchains need 21. |
| mvd/mpd compiled against 1.21.1 | They use no Minecraft classes; the oldest target gives one jar for all. |
| Version calls behind small classes in `fabric/compat` (seams), added to `common` in their own commits before the port that needs them | A layer then replaces a 20-line seam instead of a large file, so "only changes per version" holds and big classes stay in `common` once. |
| Entity base as a chain, one concern per class (`SavedEntity` ← `DamageEntity` ← `UsableEntity` ← `SolidEntity` ← `BoundsEntity` ← `InterpolatedEntity`) | Saving, damage, use, collision, bounds and interpolation changed in different versions; one class per concern means one replaced file per change, without copying the others. |
| `.removed` markers used for classes a version no longer needs (1.21.9 `BufferPoseOutput`, `ParticleEngineMixin`; 26.2 `EntityAccessor`) | A leftover mixin targeting a missing member fails at run time; removing it is the change. |
| Open ranges stack (`1.21.2+`, `1.21.4+`, … `26.3+` all apply to 26.3); no closed range was needed | Each change starts where Minecraft changed and stays; a file changed twice simply has the newer copy in the later range. |
| Client smoke run for every newly ported version, not only the five in the table | Two mixin targets (1.21.9 `ParticleEngine#crack`, 26.1 `Camera#setup`) compiled but failed at run time; only a client start shows it. |
| Remap tasks limited to 2 at a time (`RemapService`), daemon heap 4 GB | A cold `buildAll` of 11 obfuscated versions in parallel ran out of heap; the limit keeps the build parallel elsewhere. |
| 26.2 part ids reserved from `ServerLevel`'s counter, only on the server | The counter moved from `Entity` to `ServerLevel` (which skips ids in use); client entities take their id from the spawn packet. |
| One icon for every loader: `[mod] icon` in `platforms.toml`, copied to `assets/<id>/icon.png` | Generated with the metadata, so Fabric (`icon`) and NeoForge/Forge (`logoFile`) point at the same file in every jar; the 256×256 source is `images/modularity.png`. |
| Shared helpers instead of copies in the 1.21.1 base (`GameCompat.result`, `NbtData.encode/decode/writeAll/readAll`, `ModularityFabric.forEachMod`, handle records, `PartShape.slab`, `Props.with`) | Each copy existed once per caller; one helper is one place to fix and one place for the later shared Minecraft layer. Tolerances and error texts kept, so no behaviour change. |

## 7.9 Port log

What each version layer changes (oldest first). Version-specific calls sit behind the small classes of
`fabric/compat` (and a few small client classes), so a layer replaces those, not the large files using them.

| Layer | Files | What changed in Minecraft / Fabric API |
|---|---|---|
| `changes/1.21.2+` | `RegistryCompat`, `GameCompat`, `compat/DamageEntity`, `ClientCompat`, `NamedValuesProperty`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel`, `client/ModularEntityRenderer` | block/item properties need their registry key (`setId`), `EntityType.Builder#build(ResourceKey)`, `EntityType#create(level, EntitySpawnReason)` (we use `TRIGGERED`); `InteractionResult` is a sealed interface (`SUCCESS` on both sides); `PiglinAi.angerNearbyPiglins` takes the server level; `hurt` is final, entities implement `hurtServer`/`hurtClient`; `Input` record for held keys; `Property` has a value list and `getInternalIndex`; `UnbakedModel#resolveDependencies`, no `ItemOverrides`; entity renderers draw from an `EntityRenderState` |
| 1.21.3 | none | builds and runs from the 1.21.2+ layer unchanged |
| `changes/1.21.4+` | `compat/BoundsEntity`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel` | `makeBoundingBox()` is final, entities override `makeBoundingBox(Vec3)`; block state models are `UnbakedBlockStateModel` baked with `ModelBaker#sprites`; Fabric Renderer API emits into a `QuadEmitter` with a cull test (no `RenderContext`); `BakedModel#isCustomRenderer` removed |
| `changes/1.21.5+` | `InterpolatedEntity`, `Nbt`, `EntityCompat`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel` | `lerpTo` gone: server updates go through the entity's `InterpolationHandler` (ours forwards them to the existing lerp code); NBT getters return `Optional` (`...Or` keeps the defaults); `moveTo` is `snapTo`; `BakedModel` replaced by `BlockStateModel` (parts) and `BlockStateModel.UnbakedRoot`; Fabric emits through `FabricBlockStateModel#emitQuads`; sprites need a debug name |
| `changes/1.21.6+` | `SavedEntity`, `SavedBlockEntity`, `Inventories`, `SolidEntity`; smoke `Screenshots` | save data through `ValueInput`/`ValueOutput` (our compound stored with `CompoundTag.CODEC`, same format); `ContainerHelper` works on value IO (tag-backed `TagValueInput`/`TagValueOutput`); `canBeCollidedWith(Entity)`; `Screenshot.grab` takes a downscale factor |
| 1.21.7 | none | builds and runs from the 1.21.6+ layers unchanged |
| 1.21.8 | none | builds and runs from the 1.21.6+ layers unchanged |
| `changes/1.21.9+` | `KeyMappings`, `SyncedData`, `client/ModularEntityRenderer`, `client/SubmitPoseOutput` (new), `client/BufferPoseOutput.java.removed`, `mixin/client/ClientLevelMixin` (new), `mixin/client/ParticleEngineMixin.java.removed` | key mappings take a registered `KeyMapping.Category`; no vanilla compound entity data serializer (ours, registered with `FabricTrackedDataRegistry` as `modularity:compound_tag`); entity renderers `submit` to a `SubmitNodeCollector` (custom geometry) instead of writing to buffers; hit particles spawn in `ClientLevel#addBreakingBlockEffect` (was `ParticleEngine#crack`) |
| 1.21.10 | none | builds and runs from the 1.21.9+ layers unchanged |
| `changes/1.21.11+` | `Ids`, `SyncedData`, `client/SubmitPoseOutput`, `client/ObjModelReloadListener` | `ResourceLocation` is `Identifier`; render types come from `rendertype.RenderTypes`; vanilla quaternion serializer typed `Quaternionfc` (ours, `modularity:quaternion`, keeps `Quaternionf`); Fabric resource loader v1 (`ResourceLoader#registerReloader`) replaces the v0 identifiable reload listeners |
| `changes/26.1+` | `UsableEntity`, `LootCompat`, `CreativeTabs`, `Payloads`, `RenderDataHolder`, `BlockViews`, `Storages`, `KeyMappings`, `SyncedData`, `Messages`, `client/HudHook`, `client/FabricHudContext`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel`, `client/ModularEntityRenderer`, `client/SubmitPoseOutput`, `client/ObjModelReloadListener`, `mixin/client/CameraMixin` | unobfuscated Minecraft, Java 25, Loom without remapping; Fabric API uses official names (`blockgetter`, `creativetab`, `keymapping`, `ContainerStorage`, `FabricEntityDataRegistry`, `serverboundPlay`/`clientboundPlay`, `registerReloadListener`); HUD only through `HudElementRegistry` with a `GuiGraphicsExtractor`; `Entity#interact` gets the hit point; loot `TOOL` is an `ItemInstance`; player messages split into system/overlay; block models in `block.dispatch` with baked `Material`s and material flags (ours: the delegates'); `entityCutout(id, outline)` is the no-cull type; camera zoom in `Camera#alignWithEntity` |
| 26.1.1 | none | builds and runs from the 26.1+ layer unchanged |
| 26.1.2 | none | builds and runs from the 26.1+ layer unchanged |
| 26.2 | `changes/26.2+`: HudHook, EntityIds + ServerLevelAccessor (EntityAccessor `.removed`); smoke Screenshots | hidden state and screen moved to `Minecraft.gui`; entity id counter moved to `ServerLevel`; main render target in `GameRenderer` |
| 26.3 | `changes/26.3+`: InterpolatedEntity, PoseOutput, ClientLevelMixin, PushReactions | InterpolationHandler is an interface made by `createInterpolationHandler`; `PoseStack.mulPose(Quaternionf)` replaced by `rotate`; breaking particles in `ClientLevel#addBreakingParticles`; `PushReaction` values renamed (`PUSH_PULL`, `POPPED`, `IMMOVEABLE`, `IGNORE_ENTITY`, `PUSH`), found by running refabslab on 26.3 (the smoke mod registers no blocks) |

## 7.10 Verification (2026-11-06)

`./gradlew buildAll collectJars --rerun-tasks` passes; `build/libs-all` holds `modularity-fabric-<version>` for all 16 versions plus `mvd-fabric-4.0.0` and `mpd-fabric-1.0.0`. Plugin tests: 27 passed. Layer checks (no identical copies, every `.removed` names a file below) pass on every platform. Fabric meta re-checked: 26.3 is the newest stable.

Files per layer (`.removed` markers in brackets): common 77; `1.21.2+` 8; `1.21.4+` 3; `1.21.5+` 5; `1.21.6+` 4; `1.21.9+` 5 (2); `1.21.11+` 4; `26.1+` 18; `26.2+` 3 (1); `26.3+` 3; no `versions/<v>` has sources. Smoke mod: common 4, `1.21.6+` 1, `26.2+` 1.

Smoke log lines (`build/smoke/{server,client}.txt`):

| Version | Mods loaded (not nested) | `[mvd] Detected version` | `[mpd] Detected platform` | Server end | Client |
|---|---|---|---|---|---|
| 21.1 | 3/3 | 1.21.1 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 21.2 | 3/3 | 1.21.2 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.3 | 3/3 | 1.21.3 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.4 | 3/3 | 1.21.4 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.5 | 3/3 | 1.21.5 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.6 | 3/3 | 1.21.6 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 21.7 | 3/3 | 1.21.7 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.8 | 3/3 | 1.21.8 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.9 | 3/3 | 1.21.9 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.10 | 3/3 | 1.21.10 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 21.11 | 3/3 | 1.21.11 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 26.1 | 3/3 | 26.1 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
| 26.1.1 | 3/3 | 26.1.1 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 26.1.2 | 3/3 | 26.1.2 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 26.2 | 3/3 | 26.2 | FABRIC 0.20.1 SERVER | ticked 40 | - |
| 26.3 | 3/3 | 26.3 | FABRIC 0.20.1 SERVER | ticked 40 | joined, screenshot, done |
