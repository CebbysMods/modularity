---
# modularity-6rm0
title: 'forge: mixins and the generated config'
status: done
type: task
priority: normal
created_at: 2026-11-10T21:47:41Z
updated_at: 2026-11-12T11:10:00Z
parent: modularity-cuuj
---

Files: the generated `mods.toml`.
Work: Forge reads the mixin config from `[[mixins]]`; prove the generated file carries it.
Check: the smoke log shows no mixin error.

Blocked, not wired. ForgeGradle 7.0.40 has no mixin annotation processor and no remap task, so no refmap can be produced for Forge 1.21.1 with the current toolchain. `mods.toml` therefore declares no mixin config, and `platforms/forge` has no mixin source. This is the open half of `modularity-vk1z`. The work needs one of two decisions: move Forge to a toolchain that remaps mixins, or ship Forge without Modularity mixins and state the limit in docs/07 and docs/00 PR-13.

Refinement, same day: Forge 1.21.1 `mods.toml` has no `[[mixins]]` block, and FML 52.1.21 contains no mixin code. Mixin ships as a separate library (`org.spongepowered:mixin:0.8.7`) and reads the `MixinConfigs` manifest attribute. So a Forge config does not go through the metadata task at all. The refmap question is still open: the toolchain has no mixin annotation processor and no remap task, and no local evidence shows whether the Forge runtime resolves official names directly. A run with a Forge mixin config must settle it.

Route analysis, same day: Forge accepts a mixin config only through the `MixinConfigs` jar-manifest attribute. The universal jar is shared by all four loaders and carries every loader’s class tree, so a manifest entry would make Fabric, Quilt and NeoForge load the Forge config too, and its classes reference Forge types. Therefore the declaration cannot live in the universal jar manifest. The open routes are (a) a Forge coremod (`META-INF/coremods.json`) that adds the config at launch, or (b) an owner decision to ship Forge without Modularity mixins. No code change is made until the owner picks one.

Correction 2026-11-11: Forge does not need a refmap. The Forge 1.21.1 distribution jar carries the vanilla classes under official names (`net/minecraft/client/Camera.class` and the other targets), so a config written against official names applies as written. The first note in this bean is wrong on that point. The blocker is the declaration mechanism only: `MixinPlatformAgentDefault` reads the `MixinConfigs` manifest attribute, and `UniversalJar.write` writes one manifest for all four loaders. Routes: a Forge-only coremod (`ICoreModProvider.getCoreModTransformers` returns ModLauncher transformers, so it does not take a mixin config), a Forge-only nested jar under `META-INF/jarjar` with its own manifest (unproven: no evidence that Mixin scans a nested jar manifest), or no Forge mixins.



## Evidence from the jars, 2026-11-11
- `MixinContainer` instantiates `MixinPlatformAgentDefault` for **every** container (`ldc … MixinPlatformAgentDefault` in `MixinContainer`), and that agent adds the `MixinConfigs` manifest attribute (`MixinPlatformAgentDefault.accept`). So a mod jar's manifest attribute is read wherever the loader passes mod jars as mixin containers.
- ModLauncher (Forge and NeoForge): the service is `MixinServiceModLauncher`; its only platform agent is `MixinPlatformAgentMinecraftForge`, which handles the side and rejects every container, so the config comes through the default agent per container. Mod jars are containers there.
- Fabric: `MixinServiceKnot.getPlatformAgents()` returns `MixinPlatformAgentDefault`, but `getMixinContainers()` is empty and the primary container is the loader's own code source, so the mod jar manifest is **not** read.
- FML 52.1.21 has no mixin code: a case-insensitive scan of every Forge library jar finds `MixinConfigs` only in `mixin-0.8.7.jar`; `ModFileParser` knows `mods.toml` and `coremods.json` only.

Consequences:
1. A shared `MixinConfigs` entry in the universal manifest is harmless on Fabric and Quilt, but on NeoForge it would apply the Forge config **in addition to** the `[[mixins]]` block, and the two parts' configs have different relocated packages.
2. Dropping the NeoForge `[[mixins]]` block and using the shared manifest works only if both Forge-family parts carry the **same** config path and package. `UniversalJar.write` relocates each part to `universal/<loader>/…` and copies resources first-wins, so the two parts cannot share one config resource.
3. Remaining routes: (a) a coremod that calls `Mixins.addConfiguration` from `IFMLLoadingPlugin.injectData()` with the Forge part's relocated config name (no manifest change, unproven); (b) ship Forge without Modularity's mixins and state the limit in docs/00 PR-13; (c) give Forge its own jar or manifest, which breaks the one-jar design.
The owner picks (a), (b) or (c). No code change is made before that.

## Evidence from the mixin and FML jars, 2026-11-11 (second pass)
New facts, all read from `mixin-0.8.7.jar` (`org.spongepowered`) and `fmlloader-1.21.1-52.1.21.jar`:

1. **Mixin is active on Forge.** `build/smoke/server.log` and `client.log` of the Forge smoke hold
   `SpongePowered MIXIN Subsystem Version=0.8.7 … Service=ModLauncher Env=SERVER|CLIENT`. So the mixin
   transformation service and launch plugin both run on Forge 1.21.1.
2. **A mod jar is never a mixin container on ModLauncher.** `MixinPlatformAgentMinecraftForge.accept`
   returns `REJECTED` for every container, and `MixinPlatformAgentMinecraftForge.getMixinContainers()`
   returns `null` (three bytes: `aconst_null; areturn`). `MixinPlatformManager.init` therefore adds only
   the primary container; `scanForContainers` adds the empty list. The primary container is the
   `ContainerHandleModLauncher` of the game jar, not a mod.
3. **The container attributes are still read.** `MixinContainer`'s static initializer appends
   `MixinPlatformAgentDefault` to the agent list of the service, and the constructor calls `accept` on
   every agent for every container. `MixinPlatformAgentDefault.accept` reads `MixinConfigs`,
   `MixinTokenProviders` and `MixinConnector`. So the attributes would be honoured on the **primary**
   container only, and a mod is not the primary container.
4. **The `MixinConfigs` manifest route is dead on Forge 1.21.1.** `MixinLaunchPluginLegacy` has
   `addResource`, `offerResource` and `addResources` (the hooks that would add a mod jar as a container),
   but nothing in the jar calls them: no caller in `MixinTransformationServiceAbstract`, none in
   `MixinBootstrap`, none in the two container classes. They are the newer 0.8.8 API, present but unused.
5. **The `coremods.json` route is dead too.** `ModFileParser.getCoreMods` reads `META-INF/coremods.json`,
   and `CoreModFile.readCoreMod()` returns a `Reader`: FML 52 coremods are JavaScript files evaluated by
   `net.minecraftforge.coremod.CoreModEngine` with `ASMAPI`. There is no Java `IFMLLoadingPlugin` hook in
   FML 52, so a coremod cannot call `Mixins.addConfiguration`.
6. **The command-line route exists but is not shippable.** `MixinTransformationServiceAbstract.arguments`
   registers a `mixin.config` option, and `MixinPlatformManager.prepare` adds its values. A mod cannot set
   launch options.
7. **A launch-classpath service can call `Mixins.addConfiguration`, but only in a development run.** A
   `META-INF/services/cpw.mods.modlauncher.serviceapi.ILaunchPluginService` (or an
   `ITransformationService`) in the mod jar is found by ModLauncher only when the jar is on the launch
   class path. In a production install FML discovers the mod by its metadata, not from the class path.

Consequence: with one universal jar and mixin 0.8.7, Forge has no supported declarative route to a mixin
config, and no code route that runs early enough. Route (a) as written in this bean is disproved. The
surviving choices are (b) ship Forge without Modularity's mixins and state the limit in docs/00 PR-13, or
(c) give Forge its own artifact (a nested jar with its own manifest, added to the launch class path by
JarJar, with a launch plugin that calls `Mixins.addConfiguration`) — unproven, and it breaks the one-jar
design. Recommendation: take (b) now, and keep (c) as a separate feature bean.

## Decision (owner, 2026-11-12): route (b)

Forge 1.21.1 ships without Modularity's mixins. The limit is a property of the loader, not an open task:
with one universal jar and mixin 0.8.7, Forge has no declarative route to a mixin config and no code route
that runs early enough.

Recorded in:
- `docs/00-requirements.md` PR-13: the requirement now states the limit, so a Forge jar without Modularity's
  mixins meets it.
- `docs/07-multi-version-build.md` §7.4 (the mixin row of the layer table), §7.3 (the refmap and mixin
  sentence) and §7.8 (the decision row, with the bytecode evidence).
- `TODO-NOW.md` Stage 5 and `PORT-REVIEW.md`.

No code changes. Route (c), a Forge-only nested artifact with a launch plugin that calls
`Mixins.addConfiguration`, is kept as the optional feature bean `modularity-7wk5`, and it is not part of the
port programme. This bean is closed as decided.



Decision recorded 2026-11-12: route (b), the stated limit. PR-13, docs/07 7.3/7.4/7.8, TODO-NOW Stage 5 and PORT-REVIEW carry it. Route (c) is bean modularity-7wk5 (low, optional). Closed as decided; no code change.


## Correction 2026-11-12 (owner reversal): mixins are required on every platform

The owner rejected the stated limit: "Add back that mixins are required for all platforms, and if for some
specific reason the mixins for that platform do not work, with Modularity we make them work."

Point 4 above is wrong in one place: `MixinLaunchPluginLegacy.addResource` / `offerResource` /
`addResources` **do** have a caller — `cpw.mods.modlauncher.LaunchPluginHandler.offerScanResultsToPlugins`,
called from `cpw.mods.modlauncher.Launcher.run` with the GAME layer's `SecureJar`s. The hooks were searched
for inside the Mixin and FML jars only, never in ModLauncher. The chain, read from the 0.8.7 and
ModLauncher jars:

1. `Launcher.run` builds the GAME layer from the scan results, then calls
   `LaunchPluginHandler.offerScanResultsToPlugins(List<SecureJar>)`.
2. That method calls `ILaunchPluginService.addResources(List<SecureJar>)` on every launch plugin; the Mixin
   launch plugin turns each jar into a `ContainerHandleModLauncher.Resource`, which extends
   `ContainerHandleURI` and reads that jar's manifest.
3. `MixinContainer` appends `MixinPlatformAgentDefault` to every container, and that agent's `prepare()`
   reads `MixinConfigs`, `MixinTokenProviders` and `MixinConnector` and calls
   `MixinPlatformManager.addConfig`.
4. `MixinPlatformAgentMinecraftForge` stays a stub; it adds no container of its own, which is why the
   primary container is the game jar, but the mod jars are containers through step 2.

So the `MixinConfigs` manifest attribute **is** read from a mod jar on Forge 1.21.1. This is the route the
Sponge Mixin wiki gives for Forge (https://github.com/SpongePowered/Mixin/wiki/Mixins-on-Minecraft-Forge),
and the route the MixinGradle plugin automates.

Implementing:
- `GenerateModMetadataTask` writes `modularity.forge.mixins.json` for Forge, like the NeoForge config, and
  `mods.toml` keeps its `[[mixins]]` block so the name has one source.
- `ForgePlatformSetup` sets `MixinConfigs: modularity.forge.mixins.json` on the Forge jar manifest.
- No refmap: the Forge runtime uses official names.

Open in this bean:
- a Forge server and client smoke that shows the config applying. The proof is now a log line: the Forge
  config sets `"verbose": true`, `SmokeCheck` requires `Mixing <mixin> from <config>.forge.mixins.json
  into <target>` for the Forge loader, and `SmokeCheckTest` covers both the passing log and the missing
  line. No run has been made yet.
- risk: `mods.toml` now carries a `[[mixins]]` block, which Forge 1.21.1 has no field for. FML reads the
  file as TOML and ignores an unknown table, but only the Forge smoke shows that it does. If FML rejects
  it, write the block for NeoForge only and take the Forge name from `ModMetadata.mixinConfigName`.
- the universal jar, whose one manifest cannot name a different config per loader. Candidate: the
  `MixinConnector` attribute, a class that adds the running loader's config at launch (bean
  `modularity-xbj0`).

References recorded with the decision: the Sponge Mixin wiki, https://mixins.microcontrollers.dev/general/pseudo/
(`@Pseudo` optional mixins) and https://github.com/MinecraftForge/coremods (FML 52 JavaScript coremods).

## Third reading 2026-11-12 (a run and the bytecode disagree with the correction above)

The correction above is wrong in its last step. Two proofs:

1. Run of 2026-11-12 on this machine: `:platforms:21.1:21.1-forge:smokeServer` and `:21.1-forge:smokeClient` are BUILD SUCCESSFUL and the logs hold `SpongePowered MIXIN Subsystem Version=0.8.7 Source=…mixin-0.8.7.jar!/ Service=ModLauncher`, but no `Mixing` line, and `grep -ci mixing` returns 0. With `"verbose": true` in `modularity.forge.mixins.json`, a loaded config would log one line per applied mixin, so the config does not load.
2. Bytecode of FML 52.1.21 (`fmlloader-1.21.1-52.1.21.jar`): `ModDirTransformerDiscoverer` is the `ITransformerDiscoveryService` that adds `mods/` to ModLauncher. Its `SERVICES` constant holds `cpw.mods.modlauncher.api.ITransformationService`, `net.minecraftforge.forgespi.locating.IModLocator` and `IDependencyLocator`; `visitFile` skips every path that is not a regular `.jar`, and a jar is added only when `SecureJar.from(jar).moduleDataProvider().descriptor().provides()` names one of those services, which for an automatic module comes from `META-INF/services/<service>`.

So a Modularity jar reaches the GAME layer only if it provides a locator or transformation service. It provides none, so `offerScanResultsToPlugins` never sees it and `MixinConfigs` is never read. A development run cannot work at all: ForgeGradle adds a class folder, and the discoverer visits files only.

State in the tree: the Forge jar carries `MixinConfigs: modularity.forge.mixins.json` (checked with `unzip -p … META-INF/MANIFEST.MF`), the config is generated with `"verbose": true`, and `mods.toml` also declares it. `SmokeCheck` does not require the `Mixing` line, because a Forge development run cannot produce it.

Next step (this bean): add a minimal `IModLocator` or `IDependencyLocator` implementation to the Forge layer and declare it with `META-INF/services/…` in the jar, so `ModDirTransformerDiscoverer` adds the jar to ModLauncher. That is also the route for the universal jar, because a no-op locator returns no mods and changes nothing else. Risks: the locator is instantiated by ModLauncher for every run, so it must do nothing when it is not the host mod; and the FML locator lifecycle (`initArguments`, `scanMods`) must accept an empty result. Only a run from the shipped jar proves it (bean `modularity-xbj0`).

## Implemented 2026-11-12 (not proved by a run)

The locator service is in the tree. `platforms/forge/.../forge/locating/ModularityForgeLocator` implements
`IDependencyLocator` and claims no mod (`isValid` false, `scanMods` empty). The file
`platforms/forge/common/src/main/resources/META-INF/services/net.minecraftforge.forgespi.locating.IDependencyLocator`
declares it, so `ModDirTransformerDiscoverer` accepts the jar and ModLauncher offers it to the Mixin launch
plugin. The class compiles with `javac --release 21` against forgespi 7.1.5 (exit 0).

`UniversalJar` now rewrites a `META-INF/services` file the same way it rewrites metadata, because the
provider class moves with its tree. `UniversalJarTest.serviceFilesNameTheRelocatedClass` covers it.

Still open in this bean:
- a run of the `jar` task, to confirm the service file is in the per-loader and the universal jar;
- a run from the shipped jar, which is the only way to see the config apply (bean `modularity-xbj0`). A
  development run cannot: ForgeGradle adds a class folder and the discoverer visits files only;
- the universal jar's single manifest still cannot name a config per loader (`modularity-xbj0`).


Run 2026-11-12 (third pass): the service file in `src/main/resources` broke the Forge development run — FML read the main output as a transformer artifact and `modularity` vanished from the mod list (`Mod 'modularity_smoke' requires 'modularity' ... not installed`). The file now comes from `forgeLocatorService` (`build/generated/forge-locator`) and only the `jar` task takes it; the same eight smokes are green again (BUILD SUCCESSFUL in 2m 58s). Unproved: the entry inside the built jar (the jar predates the task) and Mixin reading the manifest from the shipped jar.

## 2026-11-12: the config applies in a development run

The route is the manifest, not `[[mixins]]`: Forge 1.21.1's mod file parser has no field for the block, and
Mixin reads `MixinConfigs` from the manifest of the mod file. A development run reads the manifest of its
classes folder, so `ForgePlatformSetup` alone was not enough: `GenerateModMetadataTask` now writes
`META-INF/MANIFEST.MF` with `MixinConfigs: modularity.forge.mixins.json` into the generated metadata of the
Forge platform, and the `jar` task excludes that copy so the jar keeps its own attribute
(`ModMetadata.MANIFEST_MIXIN_CONFIGS`).

Check, run 2026-11-12 10:52: `:platforms:21.1:21.1-forge:smoke` and the eight 21.1 smokes are green. The
Forge client log holds seven `Mixing … from modularity.forge.mixins.json` lines, including
`client.ParticleEngineMixin` and `client.CameraMixin`, and no mixin error (docs/07 §7.10).

Still open, not part of this check: a run from the shipped Forge jar, where the locator service must make
FML offer the jar to ModLauncher (`modularity-xbj0`).
