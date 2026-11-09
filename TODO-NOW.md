# TODO-NOW

Notes that must be resolved **before this feature (the platform-setup refactor) can be closed**.
"Now" is not "this session" — it is "not left behind". Tick a box only when the change is in and
verified.

## Review notes — worth knowing, not bugs yet

- [ ] **Unreachable `default -> throw` in `ModularityPlatformPlugin`.** After the
      `usesLoom`/`List.of(FABRIC, QUILT)` early return, the second switch can only see `NEOFORGE`
      and `FORGE`, so `default -> throw new GradleException("unexpected loader …")` is dead. Drop it
      when the legacy body goes (S5).
- [ ] **`LAYER_BASE` is declared twice** — `ModularityPlatformPlugin.LAYER_BASE` (public, still used
      by the legacy NeoForge path) and `AbstractPlatformSetup.LAYER_BASE` (package-private). One
      constant, one home, after S5.
- [ ] **`layered(Project, TaskProvider<AssembleLayersTask>, String)` is duplicated** in
      `ModularityPlatformPlugin` and `AbstractPlatformSetup`. Same reasoning: it survives in the
      plugin only for the NeoForge legacy path.
- [ ] **The legacy body of `ModularityPlatformPlugin.apply` duplicates the shared stages.** Lines
      ~44–117 re-implement `javaSetup` (maven-publish, archive name, toolchain, sources jar, compile
      options, `compile` alias), `sources` (`assembleLayers`, `generateModMetadata`, source dirs) and
      `publication`. Every change to a shared stage currently has to be made twice. Removed in S5
      once NeoForge is migrated.
- [ ] **`parchmentRepository` is a `public static` on a `Plugin` class.** It is public only so the
      `gradle.setup` package can reach it; the natural home is `AbstractPlatformSetup` (or a small
      repository helper next to it). Move it when the plugin's own copy is deleted (S5).
- [ ] **Empty setup classes are silently valid.** `NeoForgePlatformSetup` and `ForgePlatformSetup`
      extend the scaffold with no bodies; if the plugin were routed to one before its hooks are
      implemented, the project would configure "successfully" and build nothing. Either implement
      them (S3/S4) or make the unimplemented stages fail loudly — decide before closing.
- [ ] **`ModularityPlatformPlugin.parchmentRepository` / `modJarTask` remain the public API of a
      `Plugin` class.** `ModularityVersionPlugin` calls `modJarTask`; keep it public wherever it
      ends up.
- [ ] **`ModularityPlatformPlugin.java` is committed with CRLF line endings** while the rest of the
      repository is LF (a scripting edit wrote it back with Windows endings). Normalise it in one
      whitespace-only commit so later diffs stay readable.

## S3 — NeoForge first

Order: NeoForge before Forge, because `NeoForgeSetup` owns `loaderModule`/`coreModule` (shared by
both) and because it leaves the plugin's legacy body holding Forge alone.

- [x] Companion mods are named once in the table (`companion-mods`); `PlatformTable.Mod.companionProject`
      and `companionApi` derive the loader's build and its `-core` API, and `AbstractPlatformSetup.companionProject`
      checks the derived project exists. `ModModules` is gone.
- [ ] Add `AbstractForgeFamilyPlatformSetup` (package-private, `gradle.setup`):
      - `loaderPlugins` → `java-library` + `loaderPluginId()`
      - `dependencies` → `prepareSourceSet(main)` → `loaderDependencies()` → shared `api` deps for
        `mod().nested()` → `packageNestedModules()` → shared companion-mods block →
        `platformDependencies()`
      - `runs` → `SmokeSetup.apply` with `prepareSourceSet(smoke)`, the shared smoke class-path
        wiring, `smokeDependencies()`, then `registerSmokeRuns()`
      - hooks: `loaderPluginId`, `loaderDependencies`, `packageNestedModules`,
        `registerSmokeRuns`; optional: `prepareSourceSet`, `platformDependencies`,
        `smokeDependencies`
      - `tasks()` deliberately not overridden here (Forge's jar-in-jar is its own stage)
- [ ] Implement `NeoForgePlatformSetup` on the base: ModDevGradle plugin, `NeoForgeExtension`
      version + `parchment {}` + Parchment repository, mod registration for `main`,
      `jarJar` for nested, `addModdingDependenciesTo(smoke)` + `modularity_smoke` registration, the
      two `runs.register(...)` blocks.
- [ ] Delete `NeoForgeSetup.java`.
- [ ] Route NeoForge through `PlatformSetup`: early return becomes
      `platform.loader() != Loader.FORGE`, drop the now-dead `default -> throw` from the second
      switch, update the comment to "Forge keeps the legacy path until S4".
- [ ] Verify: `-p build-logic test`, `:platforms:21.1:21.1-neoforge:smokeClient`.

## S4 — Forge last (and it finishes the job)

- [ ] Implement `ForgePlatformSetup` on the base:
      - `loaderPluginId()` → `net.minecraftforge.gradle`
      - `prepareSourceSet` → `oneOutputDir(sourceSet)`
      - `loaderDependencies` → `minecraft.mappings("official"/"parchment", …)` **including the
        `Closure<Object> doCall(Object)` attribute pinning, copied verbatim with its comment**,
        mavenizer repositories, `minecraft.dependency(...)` → `addProvider("implementation")`
      - `packageNestedModules` → create the `modularityNested` configuration and its project deps
      - `registerSmokeRuns` → ForgeGradle `runs.register("server"/"client")` with per-run
        `mods.register(...)`, returning `SmokeSetup.RunTasks.SMOKE`
- [ ] `tasks()` → register `jarJarMetadata`, wire `jar.from(jarJarMetadata)` and
      `jar.from(nested, into("META-INF/jarjar"), rename(nestedJarName))`; take the configuration by
      name and the paths from `ctx.mod().nested()` so nothing crosses stages.
- [ ] Extract `JarJarMetadataTask` to its own public class next to `AssembleLayersTask`.
- [ ] Delete `ForgeSetup.java`.
- [ ] `ModularityPlatformPlugin`: make the early return unconditional and delete the whole legacy
      body (~lines 44–117), the second switch, `LAYER_BASE` and `layered()`; `apply` becomes table
      read + `new PlatformSetup().apply(project, table, platform)`.
- [ ] Verify: `-p build-logic test`; `:platforms:21.1:21.1-forge:build` and inspect
      `META-INF/jarjar/metadata.json` + the nested jars; `:platforms:21.1:21.1-forge:smokeClient`.

## S5 — sweep and close

- [ ] Tick every review note above that S3/S4 retired (`default -> throw`, `LAYER_BASE`,
      `layered()`, the duplicated legacy stages) and fix the rest: move `parchmentRepository` to
      its final home, decide the "empty setup is silently valid" item (every loader now has a body —
      does any stage still need to fail loudly?).
- [ ] Add the Forge line to `.beans/modularity-2a9k` (it still names `ForgeSetup`).
- [ ] Run `smokeAll` across the four loaders and `:platforms:21.1:jar`.
- [ ] Review `docs/07-multi-version-build.md` for anything that described the old plugin layout.
