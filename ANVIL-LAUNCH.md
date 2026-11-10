# Anvil launch stage — investigation (for review)

Decision: **Option 1**, but the dispatch point is the loader `RegisterEvent`, not an FML lifecycle event. No FML
lifecycle event means "all mods are constructed".

## The question

The launch stage (`ModularityModLaunch`) runs on Fabric and Quilt from the `pre_launch` entry point. The Forge
family has no entry point that early. The stage must run on NeoForge and Forge after every mod is constructed and
registered in `ModularModRegistry`, and before the common stage (`ModularityCommon.init`) at `FMLCommonSetupEvent`.

Two options exist:

* **Option 1**: one listener on Modularity's own mod bus, for one loader event, with a `static` guard.
* **Option 2**: one call per mod, from the constructor of the generated per-mod `@Mod` adapter.

This document checks what the actual NeoForge 21.1.265 and Forge 52.1.21 artifacts do.

Jars and sources inspected (all under `C:\Users\valen\.gradle\caches\`):

| Tag | Path |
|---|---|
| NF-FML | `modules-2\files-2.1\net.neoforged.fancymodloader\loader\4.0.45\4a0968c2ec6234d58e0a9ed97acafc08cc8f33b5\loader-4.0.45.jar` |
| NF-UNIV | `modules-2\files-2.1\net.neoforged\neoforge\21.1.265\fd00dd19b597fbaf7ac14c9be1df1bdd1295660b\neoforge-21.1.265-universal.jar` |
| NF-SRC | `modules-2\files-2.1\net.neoforged\neoforge\21.1.265\44fcd05d3bf710cdf528c84826bffb88ee289d69\neoforge-21.1.265-sources.jar` |
| NF-USERDEV | `modules-2\files-2.1\net.neoforged\neoforge\21.1.265\355b59559efe77cf5ed7e8c08113ed203b2349f1\neoforge-21.1.265-userdev.jar` |
| F-FMLCORE | `modules-2\files-2.1\net.minecraftforge\fmlcore\1.21.1-52.1.21\7e30ea8997b13912e6784dc0c68170db0030de52\fmlcore-1.21.1-52.1.21.jar` |
| F-FMLLOADER | `modules-2\files-2.1\net.minecraftforge\fmlloader\1.21.1-52.1.21\f80da0e05010539ec1ecf64b15ee2b8e1a68e54\fmlloader-1.21.1-52.1.21.jar` |
| F-JAVAFML | `modules-2\files-2.1\net.minecraftforge\javafmllanguage\1.21.1-52.1.21\57d8a311637e2882ea10db3aa53695b9570bbdd5\javafmllanguage-1.21.1-52.1.21.jar` |
| F-UNIV | `minecraftforge\forgegradle\mavenizer\caches\maven\forge\net\minecraftforge\forge\1.21.1-52.1.21\forge-1.21.1-52.1.21-universal-srg.jar` |
| F-SRC | same directory, `forge-1.21.1-52.1.21-sources.jar` |
| F-USERDEV | same directory, `forge-1.21.1-52.1.21-userdev.jar` |

`javap -p -c` read the compiled classes. `unzip` extracted the source jars. No FML sources jar exists for either
loader; FML evidence is from compiled classes.

## Facts

### a. The four named events and the bus

1. **Both loaders contain all four named events.** NeoForge: `javap` lists
   `net.neoforged.fml.event.lifecycle.FMLConstructModEvent`, `InterModEnqueueEvent`, `InterModProcessEvent`,
   `FMLCommonSetupEvent` in NF-FML. Forge: F-SRC `net/minecraftforge/fml/core/ModStateProvider.java` imports the
   same four classes (lines 18-23) and wraps them in loading states. No event was removed in these versions.

2. **All four are mod bus events, not loader or game bus events.** `ModLifecycleEvent` implements `IModBusEvent`:
   NF-FML `ModLifecycleEvent` (`javap`: `extends Event implements IModBusEvent`), F-SRC
   `net/minecraftforge/fml/event/lifecycle/ModLifecycleEvent.java:20`. NeoForge's `ParallelDispatchEvent` extends
   `ModLifecycleEvent` (NF-FML `javap`). So `NeoForge.EVENT_BUS` and `MinecraftForge.EVENT_BUS` never receive them.

3. **An event reaches only the mod that owns the bus.** The loader posts each event to one container at a time:
   NeoForge `ModLoader.lambda$constructMods$3` creates `FMLConstructModEvent` and calls
   `ModContainer.acceptEvent` (NF-FML `javap`); Forge `ParallelTransition.lambda$eventFunctionStream$0` creates
   one `ParallelDispatchEvent` per `ModContainer` (F-UNIV `javap`). A listener on Modularity's bus never sees
   another mod's event and cannot answer "did every mod run".

4. **`FMLConstructModEvent` fires after one mod's own construction, and construction runs across mods in
   parallel.** NeoForge: `ModLoader.constructMods` dispatches one task per container through
   `dispatchParallelTask` (NF-FML `javap`, `ModLoader` method `constructMods`, offsets 22 and the
   `lambda$constructMods$3` that posts the event). Forge: `ModStateProvider.java:54-57` defines the CONSTRUCT
   state as a `ParallelTransition` with `FMLConstructModEvent`. Therefore `FMLConstructModEvent` does **not**
   guarantee that all mods are constructed. It cannot be the dispatch point.

5. **`InterModEnqueueEvent` and `InterModProcessEvent` run after `FMLCommonSetupEvent` on both loaders.** NeoForge:
   NF-SRC `net/neoforged/neoforge/internal/CommonModLoader.java:69-76` (`load`: "Common setup", "Sided setup",
   "Registration events") and `:79-84` (`finish`: "Enqueue IMC", "Process IMC", "Complete loading"). Forge:
   F-SRC `ModStateProvider.java:139` orders `CONFIG_LOAD, COMMON_SETUP, SIDED_SETUP, ENQUEUE_IMC, PROCESS_IMC`.
   Both events are too late.

6. **No named FML lifecycle event satisfies "all mods constructed and before common setup".** Facts 4 and 5
   remove `FMLConstructModEvent`, `InterModEnqueueEvent` and `InterModProcessEvent`. `FMLCommonSetupEvent` itself
   is the boundary, not a point before it. The check for a lifecycle event therefore fails.

### b. Registry completeness at the only qualifying event

7. **The loader `RegisterEvent` fires after all mods are constructed and before `FMLCommonSetupEvent` on both
   loaders.** NeoForge: NF-SRC `CommonModLoader.java:46-58` (`begin` calls `gatherAndInitializeMods`, then the
   "Registry initialization" task calls `GameData.postRegisterEvents()`), `CommonModLoader.java:69-72` (common
   setup later), `net/neoforged/neoforge/registries/GameData.java:80-92` (creates `RegisterEvent` per registry and
   posts it). Forge: F-SRC `net/minecraftforge/common/ForgeStatesProvider.java:20-30` (GATHER states after
   CONSTRUCT, `LOAD_REGISTRIES` calls `GameData.postRegisterEvents()`), `net/minecraftforge/registries/GameData.java:321,344`,
   `ModStateProvider.java:139` (GATHER completes before the LOAD phase). The Forge javadoc states it directly:
   "`FMLCommonSetupEvent` ... Called after `RegisterEvent` events have been fired" (F-SRC
   `net/minecraftforge/fml/event/lifecycle/FMLCommonSetupEvent.java:17`).

8. **`RegisterEvent` is a mod bus event on both loaders.** NF-SRC `net/neoforged/neoforge/registries/RegisterEvent.java:27`
   (`extends Event implements IModBusEvent`); F-SRC `net/minecraftforge/registries/RegisterEvent.java:28` (same).
   The loader posts it to each mod container in mod order: NF-SRC `GameData.java:92`
   (`ModLoader.postEventWrapContainerInModOrder`), F-SRC `GameData.java:344` (same call). Modularity's listener
   therefore runs on Modularity's own bus, for every registry.

9. **At the first `RegisterEvent`, every constructed Modularity mod is registered.** A mod registers itself from
   its `@Mod` constructor (`platforms/common/common/src/main/java/lv/cebbys/mcmods/modularity/platform/ModularityCommon.java:33-34`).
   `ModularModRegistry.freeze()` runs only in `ModularityCommon.init` (`ModularityCommon.java:38-39`), so the
   registry is still open and complete. `LoaderInfo.modularMods()` already documents "registration (mod
   construction) order on NeoForge and Forge" (`platforms/common/common/src/main/java/lv/cebbys/mcmods/modularity/platform/loader/LoaderInfo.java:24-26`).
   NeoForge already returns registration order (`platforms/neoforge/common/src/main/java/lv/cebbys/mcmods/modularity/neoforge/loader/NeoForgeLoaderInfo.java:40-46`).

10. **NeoForge constructs mods in their `@Mod` constructor.** `FMLModContainer.constructMod` iterates every class
    in `modClasses`, finds its single constructor and instantiates it (NF-FML `javap`). A mod that registers in
    its constructor is in the registry before construction ends.

### c. Repeat dispatch

11. **`RegisterEvent` fires once per registry, so a listener runs many times per start.** Both
    `GameData.postRegisterEvents` methods loop over registry keys and create one `RegisterEvent` per key
    (NF-SRC `GameData.java:80-92`, F-SRC `GameData.java:321-344`). A `static boolean` guard is required, not
    optional.

12. **Mod loading also starts in datagen, so the stage runs there too.** NeoForge: NF-SRC
    `net/neoforged/neoforge/data/loading/DatagenModLoader.java:47` calls `begin(() -> {}, true)`, which includes
    the registry task. Forge: F-SRC `net/minecraftforge/data/loading/DatagenModLoader.java:46` calls
    `gatherAndInitializeMods`, which runs the GATHER states, including `LOAD_REGISTRIES`. The guard also keeps the
    stage to one run per datagen process.

13. **No second start of mod loading was found in one process.** The dedicated server enters from `Main`
    (NF-USERDEV `patches/net/minecraft/server/Main.java.patch:46`; F-USERDEV
    `patches/net/minecraft/server/Main.java.patch:42`). The client enters from the `Minecraft` constructor
    (NF-USERDEV `patches/net/minecraft/client/Minecraft.java.patch:34`; F-USERDEV, same line). A `grep` of both
    source jars for `ServerModLoader.load` found only the `Main` patch and, on NeoForge, the JUnit entry class
    (`net/neoforged/neoforge/junit/JUnitMain.java:19`). The guard remains necessary because of fact 11.

### d. Side and production flag

14. **The physical side and the production flag are available at `RegisterEvent`.** `FMLEnvironment.dist` and
    `FMLEnvironment.production` are public static fields; `FMLLoader.isProduction()` and
    `FMLLoader.versionInfo()` are public static methods (NF-FML `javap` for NeoForge; F-FMLLOADER `javap` for
    Forge). `isProduction()` reads a static field and loads no Minecraft class. The repository already calls
    these during mod construction, before common setup:
    `platforms/neoforge/common/src/main/java/lv/cebbys/mcmods/modularity/neoforge/detect/NeoForgePlatformSource.java:35,38`,
    `platforms/forge/common/src/main/java/lv/cebbys/mcmods/modularity/forge/detect/ForgePlatformSource.java:35,38`,
    and `NeoForgeLoaderInfo.java:21`. `LaunchContext.side()` therefore works in the launch stage on the Forge
    family.

### e. Ordering against the current common stage

15. **The current common stage runs after the parallel common setup handlers.** `ModularityNeoForgeCommon`
    registers the common stage with `event.enqueueWork` (`platforms/neoforge/common/src/main/java/lv/cebbys/mcmods/modularity/neoforge/entrypoint/ModularityNeoForgeCommon.java:38-42`).
    `ParallelDispatchEvent.enqueueWork` queues work that the sync executor runs after the parallel phase (NF-FML
    `javap`, `ParallelDispatchEvent.enqueueWork` returns a `CompletableFuture`). `RegisterEvent` runs in `begin`,
    before `load` (fact 7), so it is strictly earlier than both the parallel handlers and the queued common
    stage.

16. **`RegisterEvent` dispatch is synchronous on the loading thread.** NeoForge runs the registry task with
    `ModLoader.runInitTask("Registry initialization", syncExecutor, ...)` (NF-SRC `CommonModLoader.java:51`).
    Forge runs `LOAD_REGISTRIES` as an inline GATHER state (`ForgeStatesProvider.java:24`), driven by the sync
    executor that `ServerModLoader` passes (`net/minecraftforge/server/loading/ServerModLoader.java:30-33`). The
    launch stage would run on the main thread, as on Fabric and Quilt
    (`platforms/loom/common/src/main/java/lv/cebbys/mcmods/modularity/loom/entrypoint/LoomEntrypoints.java:59`).

### The NeoForge three-class layout and the Forge one-class layout

17. **NeoForge keeps one `FMLModContainer`, one mod bus and one mod id for all three `@Mod` classes.** The
    language provider keeps only `@Mod` classes whose `value` equals the mod id and whose `dist` contains
    `FMLLoader.getDist()` (NF-FML `javap`, `FMLJavaModLanguageProvider.lambda$loadMod$0` and `lambda$loadMod$1`).
    `FMLModContainer.constructMod` then constructs every selected class on the one container, and the container
    has one `eventBus` field. A listener added in the common constructor is on the same bus as the client and
    server listeners, and it exists on the dedicated server. The current classes match this layout:
    `ModularityNeoForgeCommon.java:28,38`, `ModularityNeoForgeClient.java:17,21`,
    `ModularityNeoForgeServer.java:15,19`.

18. **Forge keeps one `FMLModContainer`, one `@Mod` class and one mod bus per mod id.**
    `FMLJavaModLanguageProvider` selects one class per annotation `value` (F-JAVAFML `javap`,
    `lambda$getFileVisitor$2`); the container holds one `modClass` and one `eventBus` (F-JAVAFML `javap`, fields).
    The constructor is either no-arg or takes one `FMLJavaModLoadingContext` (F-JAVAFML `javap`,
    `FMLModContainer.constructMod`, offsets 25-94); the bus is `FMLJavaModLoadingContext.get().getModEventBus()`
    (F-JAVAFML `javap`). The current Forge class takes no argument
    (`platforms/forge/common/src/main/java/lv/cebbys/mcmods/modularity/forge/ModularityForge.java:17-27`).

### Timing against Minecraft class loading

19. **Minecraft classes are already loaded before any mod code runs on both loaders.** NeoForge client: the
    `Minecraft` constructor calls `ClientModLoader.begin` (NF-USERDEV
    `patches/net/minecraft/client/Minecraft.java.patch:34`); NeoForge server: `ServerModLoader.load()` runs after
    `Bootstrap` (NF-USERDEV `patches/net/minecraft/server/Main.java.patch:46`). Forge: the same two call sites
    (F-USERDEV `Minecraft.java.patch:34`, `Main.java.patch:42`). Neither option can therefore run "before
    Minecraft loads". The Forge-family launch stage is later than the Fabric and Quilt `pre_launch` stage, and the
    API javadoc must say so.

### Cost and prerequisites

20. **Forge cannot run any Modularity stage today.** `ModularityForge` installs the version and platform detectors
    only (`ModularityForge.java:17-27`). `platforms/forge/common` contains only `ModularityForge` and `detect/`;
    there is no `loader/` package. `Loaders.install` requires four non-null objects
    (`platforms/common/common/src/main/java/lv/cebbys/mcmods/modularity/platform/loader/Loaders.java:18-23`), and
    `Loaders.info()` throws `IllegalStateException("Modularity loader seams are not installed")`
    (`Loaders.java:52`). A Forge launch stage needs the Forge seam first: `LoaderInfo`, `RenderDataAccess`,
    `StorageBackend`, `LoaderHooks`.

21. **The generated per-mod adapter does not exist.** A search of `platforms/neoforge`, `platforms/forge` and
    `build-logic/src` outside `build/` finds no adapter class and no adapter generator. The only `@Mod` classes
    are Modularity's four entry points. Option 2 needs new generator and template work before it can run at all.

22. **Option 2 would also need a dependency ordering guarantee.** A guest adapter constructor can only call the
    launch stage after the seams exist, so Modularity's own constructor must run first. That holds only if every
    guest mod declares a load-order dependency on `modularity`. Option 1 gets this for free, because the listener
    runs in the loader's registry task, long after all constructors.

## Option 1

One listener, added in Modularity's own `@Mod` constructor, for the loader `RegisterEvent`:

* NeoForge: `modBus.addListener((RegisterEvent event) -> ...)` in `ModularityNeoForgeCommon`.
* Forge: `FMLJavaModLoadingContext.get().getModEventBus().addListener(...)` in `ModularityForge`.
* The listener checks a `static boolean`, sets it, logs `EntryPoints.active(EntryPoint.LAUNCH, loader)` and calls
  `ModularityLaunch.init(loader)` directly. It does not use `enqueueWork`; `RegisterEvent` has no work queue, and
  dispatch is already on the sync thread (fact 16).
* No listener is added in the client or server entry points. The common constructor runs on both dists, so one
  listener covers the dedicated server and the client (fact 17).

Against the criteria:

| Criterion | Result |
|---|---|
| All mods constructed? | Yes (facts 7, 9, 10, 11). |
| Ordering before common setup? | Yes, strictly: `begin` before `load`; GATHER before the LOAD phase (facts 7, 15). |
| Before Minecraft classes load? | No, and impossible on the Forge family (fact 19). |
| Disabled or never-constructed mods? | They are absent from `ModularModRegistry`, so they get no launch stage. Same on Fabric and Quilt. |
| Failure reporting? | `ModularityCommon.run` wraps the exception as `IllegalStateException("Modularity mod '<id>' failed to launch", e)` (`ModularityCommon.java:61-65`); it leaves the registry task and stops startup. |
| Repeat runs? | Needs the `static` guard (facts 11, 12). |
| Code cost | Small on NeoForge; on Forge it forces the seam, which the Forge port needs anyway (fact 20). No generator work. |

## Option 2

Each guest mod gets a generated `@Mod` adapter. Its constructor registers the mod (as today), then calls a new
per-mod launch entry, for example `ModularityLaunch.run(modId, "neoforge")`.

Against the criteria:

| Criterion | Result |
|---|---|
| All mods constructed? | No. The call happens during construction, so other mods may not exist yet (fact 4). It does not need them, but the contract changes from "all mods" to "this mod". |
| Ordering before common setup? | Yes, but with no ordering between mods: adapters run on the parallel construction executor, in an order the loaders do not promise. |
| Before Minecraft classes load? | No (fact 19). |
| Disabled or never-constructed mods? | No adapter runs, so no stage. Same as Option 1. |
| Failure reporting? | Better attribution: the loader reports "Failed to create mod instance" for that mod id, with the cause chain. |
| Repeat runs? | None: one constructor per container per process. No guard needed. |
| Code cost | Large: the adapter generator and template do not exist (fact 21); every guest mod must have the adapter; Modularity's own constructor must run first (fact 22); a new per-mod entry point is needed in `ModularityLaunch`; and the Forge seam is still needed for the per-mod entry to resolve its mod instance and loader name, because `ModularityLaunch` reads `Loaders.info()` and `Loaders.install` is absent on Forge (fact 20). |

Option 2 is therefore blocked today by generator work, and it changes the stage contract from "every mod, in a
deterministic order" to "each mod, during parallel construction". It does not remove the Forge seam.

## Decision

Take **Option 1**, with `RegisterEvent` as the dispatch point and one `static` guard.

Reasons:

1. It is the earliest point at which every constructed mod is in `ModularModRegistry` (facts 7, 9). No FML
   lifecycle event qualifies (facts 4, 5, 6).
2. It is strictly before `FMLCommonSetupEvent` and before the queued common stage (facts 7, 15), and it runs
   synchronously on the loading thread (fact 16), so the order matches Fabric and Quilt.
3. It needs no generator work and no new contract with guest mods (facts 21, 22). The NeoForge half is about one
   listener. The Forge half needs the Forge seam, which the Forge port needs for every other stage as well
   (fact 20).
4. The only real cost is the per-registry repeat, and one boolean removes it (fact 11).

The decision is wrong if any of these conditions holds:

* The owner rejects a content-registration event as a lifecycle hook. Then use Option 2, after the adapter
  generator exists, and accept parallel, unordered launch stages.
* The Forge seam must not be built in this slice. Then ship the NeoForge half only and keep the Forge launch
  stage as a bean.
* The stage must run before mod construction order matters, or before Minecraft classes load. That is impossible
  on the Forge family (facts 4, 19), and the API text must change instead.
* The loaders remove `RegisterEvent` in a future version. Then re-check the event list at each table version
  bump; the dispatch point must be chosen again.

## Changes it implies

No code changes are made here. The change list, file by file:

* `platforms/common/common/src/main/java/lv/cebbys/mcmods/modularity/platform/ModularityLaunch.java`: add one
  guarded entry point for the Forge family, for example `initOnce(String loaderName)`, that checks a `static`
  boolean, logs the `launch` entry point and calls `init(loaderName)`. Update the class javadoc: "before the game
  starts" is true only for Fabric and Quilt.
* `platforms/neoforge/common/src/main/java/lv/cebbys/mcmods/modularity/neoforge/entrypoint/ModularityNeoForgeCommon.java`:
  add the `RegisterEvent` listener in the constructor and call the guarded entry point. No listener in the client
  or server classes.
* `platforms/forge/common/src/main/java/lv/cebbys/mcmods/modularity/forge/ModularityForge.java`: obtain the mod
  bus, add the same listener, call the guarded entry point with `"forge"`.
* `platforms/forge/common/src/main/java/lv/cebbys/mcmods/modularity/forge/loader/ForgeLoaderInfo.java` (new):
  `LoaderInfo` over the Forge API, mirroring `NeoForgeLoaderInfo`, including `modularMods()` from
  `ModularModRegistry.entries()`.
* `platforms/forge/common/src/main/java/lv/cebbys/mcmods/modularity/forge/loader/ForgeRenderData.java`,
  `ForgeStorageBackend.java`, `ForgeLoaderHooks.java` (new): the other three seam objects that
  `Loaders.install` requires. Without them the launch stage cannot read `Loaders.info()`.
* `platforms/neoforge/.../entrypoint/ModularityNeoForgeCommon.java` and `ModularityForge.java` together install
  the seam before the listener can fire; on Forge this moves `Loaders.install` into the constructor.
* `modularity-api/src/main/java/lv/cebbys/mcmods/modularity/api/ModularityModLaunch.java`: replace "on Fabric and
  Quilt only" and "before Minecraft loads" with the real contract per loader family.
* `docs/02-requirements.md` and `docs/00-requirements.md`: add or amend the launch-stage requirement and its
  check (log line on every platform, before the common line), and move PR-12 to the new state.
* `docs/07-multi-version-build.md`: record the decision in 7.8, with the reason that no lifecycle event qualifies
  and that `RegisterEvent` is the dispatch point.
* `build-logic/src/main/java/lv/cebbys/modularity/gradle/smoke/SmokeCheck.java`: add the launch entry-point line
  to the expectations, so a run proves the stage ran. The line format from
  `platforms/common/common/src/main/java/lv/cebbys/mcmods/modularity/platform/EntryPoints.java` is
  `[modularity] entrypoint launch active (<loader>)`.
* `.beans/`: one bean for the Forge seam, one for the launch-stage smoke check, if they are not part of this
  change.

## Verification

Do not run any command here. When allowed, a run must show these lines in `build/smoke/server.txt` for the
NeoForge and Forge platforms:

* `[modularity] entrypoint launch active (neoforge)` or `(forge)`, exactly once.
* The existing `(<side>) platform neoforge:...` or `forge:...` line, with the right Minecraft version and
  side.
* `[smoke] server ticked <n> times, stopping`, with no crash marker.

The launch line appears before the platform line on NeoForge, because the platform line is logged inside
`enqueueWork` (`ModularityNeoForgeCommon.java:38-42`). A check must not require a fixed order between them.

Commands, one platform per Gradle call:

```
./gradlew :platforms:21.1:21.1-neoforge:smokeServer
./gradlew :platforms:21.1:21.1-forge:smokeServer
```

`runClient`/`smokeClient` for the same projects must also show the line on the client, and the line must appear
once in a datagen run.

Negative checks a run must show, if a temporary test mod is used:

* A launch-stage failure stops startup with `Modularity mod '<id>' failed to launch` in the log.
* A mod with no launch stage does not fail.

## Risks

* `RegisterEvent` is a content-registration event. The listener registers no content. This is legal, but a future
  loader version may tighten the event. Re-check at each table version bump.
* The listener fires once per registry, many times. A missing or misplaced guard breaks repeat-safety; the guard
  must be a single `static` field next to the entry point.
* A failing launch stage aborts registry initialization, so later mods in `modularMods()` get no launch stage.
  Fabric and Quilt behave the same way, so this is consistent, but the requirement must state it.
* The Forge seam is a prerequisite, and it is larger than the launch change itself. Shipping the Forge half in
  the same change couples two risks.
* `FMLEnvironment.dist`, `FMLEnvironment.production` and `FMLLoader.isProduction()` are loader internals. The code
  already depends on them in `NeoForgePlatformSource` and `ForgePlatformSource`, so this adds no new dependency,
  but the risk remains.
* Datagen runs the launch stage with no game. A stage that needs a running client or server breaks datagen.
* The Forge-family stage runs after Minecraft classes load. A mod that expects Fabric `pre_launch` timing will
  behave differently, and the API text must say this plainly.
* The guard is a per-class-loader static. A loader that loaded Modularity twice in one process would dispatch
  twice; no such path was found, but the assumption is not tested.
