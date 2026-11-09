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
