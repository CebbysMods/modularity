# 2. Requirements

Requirements come from two sources: the CebbysMods projects (concrete) and the common shape of
Minecraft mods (to make sure the API can grow to "almost everything"). Each requirement is tagged
with the milestone that delivers it (see [04-milestones.md](04-milestones.md)).

## 2.1 Non-functional requirements

| ID | Requirement | M |
|---|---|---|
| N1 | One mod source runs on several loaders (Fabric, NeoForge, later Quilt/Forge) and several Minecraft versions. | 1–2 |
| N2 | **Simple first**: the common case is one line; advanced cases are reachable step by step without switching paradigms. | 1 |
| N3 | **No dead ends**: every portable object can be unwrapped to the native object (`unwrap`) and version-specific code can be selected (`Versioned`). | 1 |
| N4 | Mod logic is unit-testable without starting Minecraft. | 1 |
| N5 | Mods don't write mixins for common needs; the platform owns a small set of generic hooks. | 1 |
| N6 | Low overhead: portable wrappers are thin records, per-block lookups are cached, hot paths bail out early when nothing is registered. | 1 |
| N7 | API stability: interfaces grow with default methods; implementations are never broken by additions. | all |
| N8 | Every jar targets the lowest Java its Minecraft versions run on: API/testkit Java 17 (MC 1.18–1.20.4), each platform the Java of its MC version (1.20.5+: 21). Configured in `gradle.properties` (`java_api_release`). | 1 |
| N9 | One jar per (loader, MC version) for players; Modularity is bundled (jar-in-jar) so users install one file. | 1 |
| N10 | Build tooling generates loader metadata and per-target jars from one project. | 2 |

## 2.2 Functional requirements

### From refabslab (milestone 1 — all must be covered)

| ID | Requirement | API element |
|---|---|---|
| R1 | Register a block with custom block state properties | `mod.block(name, b -> b.property(...))`, `Property.integer/bool/enumeration` |
| R2 | Light level computed from state | `.lightLevel(state -> ...)` |
| R3 | Piston reaction, strength, sound, loot table toggle | `.pushReaction`, `.strength`, `.sound`, `.noLootTable` |
| R4 | Change placement behaviour of blocks the mod doesn't own (all slabs) | `mod.intercept(BlockSelector.type(SLAB), BlockInterceptor)` with `placement` / `canBeReplaced` |
| R5 | Store typed data per block position, persisted and synced | `BlockData<T>` + `DataCodec<T>`, `.data(...)`, `level.data/setData` |
| R6 | Attach data atomically with a placement (client prediction + server) | `PlacementResult.of(state).with(data, value)` |
| R7 | Custom mining speed from other block states and player tools | `BlockBehaviour#destroyProgress`, `Player#destroySpeed/hasCorrectToolForDrops`, `BlockStateRef#destroySpeed` |
| R8 | Custom break effects for several states, piglin reaction | `BlockBehaviour#onPlayerBreak`, `BreakContext#breakEffects` |
| R9 | Custom drops that depend on block data, also for explosions | `BlockBehaviour#drops(DropContext)` |
| R10 | Render a block as the combination of other block states' models, data driven, renderer-mod compatible | `client.blockModel(handle, DynamicModel)`, `ModelOutput#blockState` |
| R11 | Read block data while chunks are meshed (vanilla region, Sodium slice) | `BlockView#data` |
| R12 | Choose hit-particle texture per position | `client.hitParticles(handle, HitParticles)` |
| R13 | Data-driven exclusion list (other mods' slabs) | `BlockTag` + `state.is(tag)` + tag JSON |
| R14 | Detect other mods and their versions | `Platform#isModLoaded/modVersion` |
| R15 | Look up blocks by id, convert blocks to items | `Modularity.block(id)`, `BlockRef#asStack` |

### From respro, mythos-odyssey, immersive-vehicles and typical mods (later milestones)

| Area | Requirements | M |
|---|---|---|
| Items | Simple items, tools/armor tiers, food, block items, creative tabs, item behaviour (use, use on block/entity, inventory tick), item components (1.20.5+) vs NBT | 3 |
| Events | Lifecycle (server start/stop, tick), player (join, interact, break), world (load, tick), entity (spawn, death, damage), commands | 3 |
| Networking | Typed client↔server messages with codecs | 3 |
| Config | Typed config with sync to clients | 3 |
| Entities | Entity types, attributes, spawning rules, projectiles, AI goals, vehicles/riding, entity data sync | 4 |
| Rendering | Entity renderers, block entity renderers, render layers, colors/tints, GeckoLib integration | 4 |
| Input | Key bindings, mouse/vehicle input | 4 |
| Data/resources | Runtime asset/data packs (respro), data generation, recipes, loot tables, tags, advancements, worldgen | 5 |
| UI | Menus (container screens), screens, HUD overlays, tooltips | 6 |
| Advanced | Fluids, dimensions, enchantments, effects, sounds/particles registration, attachments on entities/chunks/levels | 6–7 |
| Compat | Optional integrations (JEI/REI/EMI, Jade, Sodium), modules activated by mod presence | 7 |
