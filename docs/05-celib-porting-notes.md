# 5. celib porting notes

Modularity replaces **celib** (CebbysMods/celib, formerly celib-mod), which is retired as of 2026-11-06.
This page lists the parts of celib that Modularity does not cover yet, so they can be ported when the
matching milestone comes up. Everything else in celib (blocks, properties, break events, items, entities,
levels, particles, registries, ids) is already covered by `modularity-api` in a better form.

## Where the celib code lives

Full history, offline: `D:\Archive\old-pc-projects\celib.bundle`. It holds all 9 GitHub branches plus 2 old-PC
work-in-progress branches that were never pushed.

```
git clone D:\Archive\old-pc-projects\celib.bundle celib
cd celib
git switch <branch>          # e.g. git switch wip/old-pc-resource-packs
```

| Branch | MC | What |
|---|---|---|
| `1.16-0.3.1`, `1.17-0.5.0`, `1.18-1.0.0` | 1.16-1.18 | v0: builders, registries, datagen handlers, structures |
| `1.18-1.1`, `1.19-1.1` | 1.18.2-1.19 | v1: block entity + inventory components, registrators |
| `1.19-2.0` | 1.19 | v2: block API, block break events, mod/bridge split |
| `wip/old-pc-registry` (local only) | 1.19 | uncommitted v2 work: registry entries, Item/ItemStack mixins, test blocks |
| `wip/old-pc-resource-packs` (local only) | 1.19 | uncommitted v2 work: runtime asset/data packs, `PackRepositoryMixin` |
| `feature/2.0/fabric-and-forge` | - | first Fabric + Forge split |
| `feature/2.0.0/modular-environment`, `feature/2.0.0/new-modular-environment` | 1.16-1.20 | multi-module `celib-api` v0/v1 + Fabric bridges per MC version |

## To port

### 1. Runtime resource and data packs, datagen (milestone M5, Data and resources)

- `wip/old-pc-resource-packs`: `core/api/resource/pack/{AssetPack, DataPack, ResourcePack, *Repository, *RepositoryRegistry}.java`
  and `core/mixin/PackRepositoryMixin.java`. These inject in-memory asset/data packs into the vanilla pack
  repository, so generated models, blockstates, loot tables and recipes need no files in the jar.
- `1.17-0.5.0`: `handlers/datagen/*`, the older datagen helpers.
- Related: `minecraft\respro` (resource processor) did the same job as a separate mod. Check it before
  designing the M5 API.

### 2. Multi-version bridges (milestone M2, second loader and second version)

- `feature/2.0.0/new-modular-environment`: `celib-bridge` with `bridge/fabric/v1/{v16,v17,v18,v19,v20}` and
  `loader/`. It has one bridge implementation per Minecraft version behind a shared API, selected at runtime.
  It is a reference for how platforms beyond `fabric-1.21.1` could share code across close versions.
- `minecraft\mvl` (minecraft-version-loader) is the version-detection library that this design used.

### 3. Block entities and inventories (milestones M3/M7)

- `1.19-1.1`: `component/blockentity/*`, `component/inventory/*`, an inventory-backed block entity
  abstraction. Compare with `modularity-api` `transfer` and `data` before porting.

## Not worth porting

The generic helpers (`Maybe`/`Only`, `Lazy`, `Exceptional*` functional interfaces, `lock`, `structure/collection`),
loggers, and the 2025 Fabric 1.21.5 template skeleton (no real code).
