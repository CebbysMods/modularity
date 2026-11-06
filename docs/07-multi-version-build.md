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
gradle/platforms.toml                      version table
build-logic/                               included build: the lv.cebbys.modularity plugins
platforms/fabric/
  common/src/main/{java,resources}         Fabric code shared by every version
  changes/<range>/src/main/{java,resources} one change, at the first version that needs it
  versions/<version>/                      project dir of platform fabric-<version>;
    src/main/{java,resources}              only truly single-version details
tools/smoke/                               smoke-test mod (same layering, see 7.6)
```

Range folder names: `1.21.5+` (that version and every later one) or `1.21.5-1.21.8` (inclusive).

## 7.3 Layering rules

For platform version V the sources are, lowest to highest:

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

## 7.9 Port log

What each version layer changes (oldest first). Version-specific calls sit behind the small classes of
`fabric/compat` (and a few small client classes), so a layer replaces those, not the large files using them.

| Layer | Files | What changed in Minecraft / Fabric API |
|---|---|---|
| `changes/1.21.2+` | `RegistryCompat`, `GameCompat`, `BaseEntity`, `ClientCompat`, `NamedValuesProperty`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel`, `client/ModularEntityRenderer` | block/item properties need their registry key (`setId`), `EntityType.Builder#build(ResourceKey)`, `EntityType#create(level, EntitySpawnReason)` (we use `TRIGGERED`); `InteractionResult` is a sealed interface (`SUCCESS` on both sides); `PiglinAi.angerNearbyPiglins` takes the server level; `hurt` is final, entities implement `hurtServer`/`hurtClient`; `Input` record for held keys; `Property` has a value list and `getInternalIndex`; `UnbakedModel#resolveDependencies`, no `ItemOverrides`; entity renderers draw from an `EntityRenderState` |
| 1.21.3 | none | builds and runs from the 1.21.2+ layer unchanged |
| `changes/1.21.4+` | `BaseEntity`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel` | `makeBoundingBox()` is final, entities override `makeBoundingBox(Vec3)`; block state models are `UnbakedBlockStateModel` baked with `ModelBaker#sprites`; Fabric Renderer API emits into a `QuadEmitter` with a cull test (no `RenderContext`); `BakedModel#isCustomRenderer` removed |
| `changes/1.21.5+` | `InterpolatedEntity`, `Nbt`, `EntityCompat`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel` | `lerpTo` gone: server updates go through the entity's `InterpolationHandler` (ours forwards them to the existing lerp code); NBT getters return `Optional` (`...Or` keeps the defaults); `moveTo` is `snapTo`; `BakedModel` replaced by `BlockStateModel` (parts) and `BlockStateModel.UnbakedRoot`; Fabric emits through `FabricBlockStateModel#emitQuads`; sprites need a debug name |
| `changes/1.21.6+` | `SavedEntity`, `SavedBlockEntity`, `Inventories`, `SolidEntity`; smoke `Screenshots` | save data through `ValueInput`/`ValueOutput` (our compound stored with `CompoundTag.CODEC`, same format); `ContainerHelper` works on value IO (tag-backed `TagValueInput`/`TagValueOutput`); `canBeCollidedWith(Entity)`; `Screenshot.grab` takes a downscale factor |
| 1.21.7 | none | builds and runs from the 1.21.6+ layers unchanged |
| 1.21.8 | none | builds and runs from the 1.21.6+ layers unchanged |
| `changes/1.21.9+` | `KeyMappings`, `SyncedData`, `client/ModularEntityRenderer`, `client/SubmitPoseOutput` (new), `client/BufferPoseOutput.java.removed`, `mixin/client/ClientLevelMixin` (new), `mixin/client/ParticleEngineMixin.java.removed` | key mappings take a registered `KeyMapping.Category`; no vanilla compound entity data serializer (ours, registered with `FabricTrackedDataRegistry` as `modularity:compound_tag`); entity renderers `submit` to a `SubmitNodeCollector` (custom geometry) instead of writing to buffers; hit particles spawn in `ClientLevel#addBreakingBlockEffect` (was `ParticleEngine#crack`) |
| 1.21.10 | none | builds and runs from the 1.21.9+ layers unchanged |
| `changes/1.21.11+` | `Ids`, `SyncedData`, `client/SubmitPoseOutput`, `client/ObjModelReloadListener` | `ResourceLocation` is `Identifier`; render types come from `rendertype.RenderTypes`; vanilla quaternion serializer typed `Quaternionfc` (ours, `modularity:quaternion`, keeps `Quaternionf`); Fabric resource loader v1 (`ResourceLoader#registerReloader`) replaces the v0 identifiable reload listeners |
| `changes/26.1+` | `UsableEntity`, `LootCompat`, `CreativeTabs`, `Payloads`, `RenderDataHolder`, `BlockViews`, `Storages`, `KeyMappings`, `SyncedData`, `Messages`, `client/HudHook`, `client/FabricHudContext`, `client/DynamicUnbakedModel`, `client/DynamicBakedModel`, `client/ModularEntityRenderer`, `client/SubmitPoseOutput`, `client/ObjModelReloadListener`, `mixin/client/CameraMixin` | unobfuscated Minecraft, Java 25, Loom without remapping; Fabric API uses official names (`blockgetter`, `creativetab`, `keymapping`, `ContainerStorage`, `FabricEntityDataRegistry`, `serverboundPlay`/`clientboundPlay`, `registerReloadListener`); HUD only through `HudElementRegistry` with a `GuiGraphicsExtractor`; `Entity#interact` gets the hit point; loot `TOOL` is an `ItemInstance`; player messages split into system/overlay; block models in `block.dispatch` with baked `Material`s and material flags (ours: the delegates'); `entityCutout(id, outline)` is the no-cull type; camera zoom in `Camera#alignWithEntity` |
| 26.1.1 | none | builds and runs from the 26.1+ layer unchanged |
