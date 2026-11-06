# 8. Guest mods

A **guest mod** is a mod written only against the Modularity API and shipped as one jar with no loader
metadata: no `fabric.mod.json`, no `neoforge.mods.toml`, no per-loader or per-version submodule. It
carries a `modularity.mod.json` instead. Modularity turns it into a real mod of the running loader at
startup, so the same jar works on every loader and Minecraft version Modularity supports.

Players install the guest jar next to `modularity-fabric-<version>`, mvd and mpd, all in `mods/`.

## 8.1 `modularity.mod.json`

```json
{
  "id": "refabslab",
  "version": "1.1.0",
  "name": "Refabricated Slabs",
  "description": "Mix and match slabs as you want!",
  "authors": ["CebbyS"],
  "contact": {"homepage": "https://...", "sources": "https://..."},
  "license": "CC0-1.0",
  "icon": "assets/refabslab/icon.png",
  "entrypoints": ["lv.cebbys.mcmods.refabslab.RefabslabMod"],
  "minecraft": ">=1.21.1",
  "modularity": "*",
  "depends": {},
  "recommends": {},
  "suggests": {"kleeslabs": "*"},
  "breaks": {}
}
```

| Field | Required | Meaning |
|---|---|---|
| `id`, `version` | yes | Mod id and version. |
| `entrypoints` | yes | `ModularMod` classes (one string or a list). |
| `minecraft`, `modularity` | no (default `*`) | Version ranges the mod runs on. |
| `name`, `description`, `authors`, `contributors`, `contact`, `license`, `icon`, `environment` | no | Shown in mod lists. |
| `depends`, `recommends`, `suggests`, `breaks`, `conflicts` | no | Other mods, by id and version range. |

Ranges use the loader's syntax (Fabric: `>=1.21.1`, `~1.21.1`, `1.21.x`, `*`).

## 8.2 No mixins

Guest mods have no mixins and no loader or Minecraft classes. When a mod needs something the API
lacks, the API grows: the feature is added to `modularity-api` and implemented in each platform's
layers (docs/07), where version changes are handled once for every mod.

## 8.3 Fabric

`GuestMods` (`fabric/guest`) is Modularity's `preLaunch` entrypoint. It runs after Fabric discovered
and resolved all mods, before Minecraft starts:

1. Finds candidates: every `*.jar` in `mods/` and every path in the system property
   `modularity.guestMods` (separated by the path separator; jars or folders, for development). A
   candidate is a guest when it has a `modularity.mod.json` and no `fabric.mod.json`.
2. Generates the `fabric.mod.json` the mod would have shipped (`FabricGuestJson.of`, from the shared `GuestDescriptor`):
   metadata copied, `entrypoints.modularity` = the descriptor's entrypoints, `depends` = `minecraft`,
   `modularity` and the descriptor's `depends`.
3. Adds the mod (`LoaderInjection`): Fabric's own parser reads the generated JSON, the jar joins the
   game class path, a mod container joins the loader's mod list and its entrypoints are registered.
   From then on Fabric treats it like any other mod: `FabricLoader.isModLoaded`, mod lists, crash
   reports, its assets as a resource pack, its data as a data pack, and its `modularity` entrypoint
   called by `ModularityFabric` with every other Modularity mod.
4. Checks `depends` and `breaks` with Fabric's version predicates (Fabric resolved the other mods
   before the guests existed) and stops the game with a clear message when one fails. Two mods with
   the same id are an error too.

Fabric logs `Found 1 non-fabric mod: <jar>` during discovery, before Modularity runs. It is expected.

`LoaderInjection` is the only class that uses Fabric Loader internals (no public API can add a mod
after discovery). Checked against Fabric Loader 0.20.1. A loader version that changes them needs a
new `LoaderInjection` only, and the smoke test of every platform shows it at once.

## 8.4 Verification

The smoke tests of every platform (docs/07, 7.6) load a guest mod, `tools/smoke/guest` (Modularity
API only, `modularity.mod.json`, not on the run class path). The checks require
`[modularity] Loaded guest mod smoke_guest` and, once the server started,
`[smoke] guest mod smoke_guest: loaded=true initialized=true`.

Refabslab ran as a guest mod on 26.3: one jar with its classes, resources and a
`modularity.mod.json`, no Fabric module.

## 8.5 Decisions

| Decision | Reason |
|---|---|
| Inject at `preLaunch` | The earliest point where mod code runs; Minecraft classes and the mods' main entrypoints are not loaded yet. Fabric discovers mods before any mod code, so no earlier hook exists. |
| Generate `fabric.mod.json` and let Fabric's parser read it | The container is exactly what Fabric builds for a normal mod; no copy of Fabric's metadata rules. |
| Real mod container in the loader's mod list | Resource and data packs, mod menus, `isModLoaded` and crash reports work without extra code. |
| Dependencies checked by Modularity | Fabric resolved before the guests were added. |
| No guest mixins | Mixins target one version's Minecraft code; the API is the single place for version-specific behaviour. |
| `modularity.guestMods` property | Development and smoke tests load guests from build output without copying into `mods/`. |
