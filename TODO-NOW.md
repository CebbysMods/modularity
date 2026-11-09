# TODO-NOW

Notes that must be resolved **before this feature (the platform-setup refactor) can be closed**.
"Now" is not "this session" — it is "not left behind". Tick a box only when the change is in and
verified.

## Review notes — worth knowing, not bugs yet

- [x] **Unreachable `default -> throw` in `ModularityPlatformPlugin`.** Removed with the legacy body
      in S4; `apply` no longer has a switch of its own.
- [x] **`LAYER_BASE` is declared twice.** Only `AbstractPlatformSetup.LAYER_BASE` remains; the
      plugin's public copy is gone.
- [x] **`layered(Project, TaskProvider<AssembleLayersTask>, String)` is duplicated.** The plugin's
      copy is gone; `AbstractPlatformSetup` owns it.
- [x] **The legacy body of `ModularityPlatformPlugin.apply` duplicates the shared stages.** Deleted in
      S4: `apply` is now the table read plus `new PlatformSetup().apply(project, table, platform)`.
- [ ] **`parchmentRepository` is a `public static` on a `Plugin` class.** It is public only so the
      `gradle.setup` package can reach it; the natural home is `AbstractPlatformSetup` (or a small
      repository helper next to it). Move it when the plugin's own copy is deleted (S5).
- [x] **Empty setup classes are silently valid.** Every loader now implements the abstract stages
      (`loaderPlugins`, `dependencies`, `runs`); a setup that leaves an optional stage empty (Forge's
      `smokeDependencies`, Fabric's `runtimeConfigurations`) is a deliberate no-op, not a gap.
- [ ] **`ModularityPlatformPlugin.parchmentRepository` / `modJarTask` remain the public API of a
      `Plugin` class.** `ModularityVersionPlugin` calls `modJarTask`; keep it public wherever it
      ends up.
- [x] **`ModularityPlatformPlugin.java` was committed with CRLF line endings** while the rest of the
      repository is LF. Rewritten with LF as part of the S4 rewrite.

## Open design question — fold the companion mods into Modularity?

Decide before closing this feature: the setup code exists only to wire these mods in, so the answer
either deletes that code or keeps it.

- [ ] **Review whether `mvd` and `mpd` should become part of Modularity's core instead of staying
      separate mods.** They are never used outside Modularity, so the separate mod ids, jars and
      publications may be pure overhead.
      - What folding in means: the detection code moves into the Modularity layers (`minecraft`/loader
        layers, or `modularity-api` where it has no Minecraft dependency); `utilities/mvd/{mvd-core,
        mvd-fabric, mvd-neoforge, mvd-forge}` and the `mpd` equivalents (eight projects) disappear, and
        with them `companion-mods` in `gradle/platforms.toml`, `PlatformTable.Mod.companionProject` /
        `companionApi`, `AbstractPlatformSetup.companionProject` and the companion blocks in
        `AbstractLoomPlatformSetup` / `AbstractForgeFamilyPlatformSetup`.
      - What else changes: the `fabric.mod.json` / `mods.toml` `"depends"` entries for `mvd`/`mpd`, the
        `META-INF/services` wiring in `mvd-fabric`, `docs/06-utility-mods.md` (whole document),
        `docs/08` where it describes guest/companion mods, the smoke expectations (`[mvd]` / `[mpd]`
        lines in `SmokeCheck`), and the publishing of the `-fabric`/`-neoforge`/`-forge` artifacts.
      - What is gained: no version skew between Modularity and its detectors, no separate downloads,
        no split packages or `-core` shading workaround, a much smaller build graph.
      - What is lost: standalone `mvd`/`mpd` builds (only useful if someone else consumes them),
        independent versioning, and a smaller per-concern jar.
      - Do this as its own feature, not inside the setup refactor; the refactor should only make sure
        nothing hidden depends on them.

## S3 — NeoForge first

Order: NeoForge before Forge, because `NeoForgeSetup` owns `loaderModule`/`coreModule` (shared by
both) and because it leaves the plugin's legacy body holding Forge alone.

- [x] Companion mods are named once in the table (`companion-mods`); `PlatformTable.Mod.companionProject`
      and `companionApi` derive the loader's build and its `-core` API, and `AbstractPlatformSetup.companionProject`
      checks the derived project exists. `ModModules` is gone.
      *(In the working tree; not compiled yet — the S3 verify run below is still outstanding.)*
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

*Implemented in the working tree as of the Forge pass; the verify run below is outstanding, so the boxes stay open.*

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
- [x] Add the Forge line to `.beans/modularity-2a9k` (it named `ForgeSetup`): now points at
      `ForgePlatformSetup` and `AbstractLoomPlatformSetup`.
- [ ] Run `smokeAll` across the four loaders and `:platforms:21.1:jar`.
- [x] Review `docs/07-multi-version-build.md` for anything that described the old plugin layout: §7.4 now
      names the per-loader setups and the shared stage order instead of "applies Loom …".
