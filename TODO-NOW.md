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

## One verification pass

The S3, S4 and S5 runs and the lifecycle checks (`modularity-8ywo`, `modularity-lw0r`) use the same
Gradle tasks. Run them after `modularity-r133` (NeoForge) and `modularity-g420` (Forge), so one pass
covers the platform-setup refactor and the lifecycle wiring. Until then, every item above stays open.

## NeoForge/Forge lifecycle — decisions moved to beans

The risks are now decisions and checks inside the tracker; they are no longer tracked here.

- Content registration timing → `modularity-m23h`.
- Registry location vs universal-jar relocation → recorded in `modularity-ibbd`.
- NeoForge dist classes, parallel dispatch, entry-point log lines → recorded in `modularity-r133`.
- Forge constructor injection, event-bus mismatch → recorded in `modularity-g420`.
- Lazy `LoaderInfo` → recorded in `modularity-co2j` and `modularity-v53p`.
- The runtime checks run with `modularity-8ywo` and `modularity-lw0r`.

## Quilt/Fabric separation (`loom`, `common`, native Quilt jar) — risks

Planned, not yet verified. Tick a box only when the matching check passes.

- [ ] **Fabric Loom with a `quilt.mod.json` mod in dev runs.** Loom exposes `build/resources/main`;
      Quilt Loader must scan it for the QMJ. Proved by the Quilt smoke run.
- [ ] **Quilt Loader 0.30.2 entrypoint keys.** `main`/`client`/`server` must dispatch to the Fabric
      interfaces and `pre_launch` to `org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint`. If the
      smoke fails, inspect the resolved 0.30.2 jar.
- [ ] **Two metadata files in the universal jar.** Quilt's plugin must accept `quilt.mod.json`
      before Fabric reads `fabric.mod.json`; the shipped-jar smoke still must prove it.
- [ ] **Nested jar name.** `quilt.mod.json` `jars` must match Loom's `META-INF/jars/<archive name>`;
      the unit test must fail early on a rename.
- [ ] **Mixins per relocated tree.** Each `universal/<loader>/` tree needs its own refmap; remains
      `modularity-dals` work.
- [ ] **26.x Quilt.** `remap()` is false; the QMJ writer must fail clearly until the
      Mojang-mapping path is designed.
- [ ] **Universal jar size.** The Quilt class set duplicates the shared `common`/`loom`/`minecraft`
      classes. Accepted cost of real separation; sharing the Fabric tree would leak Quilt overrides.
- [ ] **Split package across roots.** `platform.loader` has files in both `common` and `minecraft`;
      this is allowed because every root assembles into one source set. Verify with the build.
