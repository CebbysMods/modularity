# 8. Guest mods

A **guest mod** is a mod written only against the Modularity API and shipped as one jar with no loader
metadata: no `fabric.mod.json`, no `neoforge.mods.toml`, no per-loader or per-version submodule. It
carries a `modularity.mod.json` instead. Modularity turns it into a real mod of the running loader at
startup, so the same jar works on every loader and Minecraft version Modularity supports.

Players install the guest jar next to `modularity-fabric-<version>`, both in `mods/`. Modularity needs no
other mod: it detects the Minecraft version and the loader itself (docs/06).

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
| `entrypoints` | yes | `ModularityModCommon` classes (one string or a list). |
| `minecraft`, `modularity` | no (default `*`) | Version ranges the mod runs on. |
| `name`, `description`, `authors`, `contributors`, `contact`, `license`, `icon`, `environment` | no | Shown in mod lists. |
| `depends`, `recommends`, `suggests`, `breaks`, `conflicts` | no | Other mods, by id and version range. |

Ranges use the loader's syntax (Fabric: `>=1.21.1`, `~1.21.1`, `1.21.x`, `*`).

## 8.2 No mixins

Guest mods have no mixins and no loader or Minecraft classes. When a mod needs something the API
lacks, the API grows: the feature is added to `modularity-api` and implemented in each platform's
layers (docs/07), where version changes are handled once for every mod.

## 8.3 Fabric

`ModularityFabricLaunch` (`fabric/entrypoint`) is Modularity's pre-launch entrypoint (the `launch` role,
Fabric key `preLaunch`). It runs after Fabric discovered and resolved all mods, before Minecraft starts.
The candidate scan and descriptor read are shared with Quilt in `loom/guest/GuestCandidates`; the Fabric
launch adds the guests to Fabric Loader:

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
   called by `ModularityFabricCommon` with every other Modularity mod.
4. Checks `depends` and `breaks` with Fabric's version predicates (Fabric resolved the other mods
   before the guests existed) and stops the game with a clear message when one fails. Two mods with
   the same id are an error too.

Fabric logs `Found 1 non-fabric mod: <jar>` during discovery, before Modularity runs. It is expected.

`LoaderInjection` is the only class that uses Fabric Loader internals (no public API can add a mod
after discovery). Checked against Fabric Loader 0.20.1. A loader version that changes them needs a
new `LoaderInjection` only, and the smoke test of every platform shows it at once.

**Quilt.** `ModularityQuiltLaunch` (`quilt/entrypoint`) is the native `pre_launch` entrypoint. It uses
`GuestCandidates` to find the guests, then logs an error naming them and loads none. Checked against
Quilt Loader 0.30.2 (sources jar of the version in `gradle/platforms.toml`):

- The supported route is a Quilt loader plugin: `QuiltPluginContext.addModLoadOption` is public and runs
  during discovery. A mod can offer one only through the `experimental_quilt_loader_plugin` field of its
  `quilt.mod.json`, and `V1ModMetadataImpl` rejects that field unless the system property
  `loader.experimental.allow_loading_plugins` is set. It also logs that mod-provided plugins are for
  amusement only. A jar cannot set a loader system property before the loader parses metadata, so the
  route is closed for a distributed mod.
- The reflective route has the pieces Fabric uses: `QuiltLoaderImpl` keeps a mutable `mods` list and
  `modMap`, and `EntrypointStorage.add` is public. It fails at the container: the only mod container
  implementations need a `QuiltPluginContext` (`InternalModContainerBase`), and a plugin context exists
  only inside a plugin. `LoaderInjection` reads Fabric's own metadata parser and builds a
  `ModContainerImpl` from a candidate; Quilt has no equivalent public or reflective constructor.

So Quilt Loader does not have the Fabric Loader internals that `LoaderInjection` uses, and its supported
alternative is gated. The Quilt smoke runs without a guest and expects neither guest line. Bean
`modularity-4bm8` holds the evidence; a loader version that exposes a mod-provided plugin would reopen it.

**NeoForge and Forge.** Guest mods are not supported yet. Neither loader offers a public API to add a
mod after discovery, and the candidate scan lives in the `loom` root, which only Fabric and Quilt use.
Their smoke runs load no guest and expect neither guest line. The port programme records this as a
limit and closes its guest row with epic `modularity-ecjq`, which starts after Stage 5 and owns the
route on Quilt, NeoForge and Forge. Bean `modularity-4bm8` holds the loader evidence for Quilt.

## 8.4 Verification

The smoke tests of every platform (docs/07, 7.6) load a guest mod on Fabric, `modularity-smoke/`
(its own Gradle module: Modularity API only, `modularity.mod.json`, not on the run class path). The
checks require
`Loaded guest mod smoke_guest` and, once the server started,
`Guest mod smoke_guest: loaded=true initialized=true`. The Quilt platform runs the same smoke
mod without the guest and expects neither line.

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
