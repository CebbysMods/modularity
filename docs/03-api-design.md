# 3. API design

## 3.1 Goals in one sentence

**Write a mod once against small, typed, Minecraft-free interfaces; run it on every loader and
version Modularity has a platform for; and when the API is not enough yet, step down to native
code without leaving the project.**

## 3.2 Architecture

```
┌──────────────────────────────── your mod ─────────────────────────────────┐
│  common module (plain Java, depends only on modularity-api)               │
│    MyMod implements ModularMod  ── blocks, behaviours, interceptors, ...  │
│  tests (modularity-testkit: in-memory game, no Minecraft)                 │
│  per-target packaging: fabric.mod.json / neoforge.mods.toml + resources   │
└───────────────────────────────────────────────────────────────────────────┘
                 │ compiles against                    ▲ runs inside
┌────────────────▼───────────────┐   ┌─────────────────┴────────────────────┐
│ modularity-api (Java 17, no MC) │◄──│ platforms/<loader>-<mc version>       │
│ portable types + contracts      │   │ implements the API with native code, │
└─────────────────────────────────┘   │ owns all mixins, bundled jar-in-jar  │
                                      └──────────────────────────────────────┘
```

* `modularity-api` – interfaces and value types only, no Minecraft classes, Java 17.
* `modularity-testkit` – a deterministic in-memory implementation of the runtime that simulates the
  vanilla code paths the API hooks (placement, mining, breaking, drops, rendering). Mods use it for
  unit tests.
* `platforms/fabric-21.1` (milestone 1), `platforms/neoforge-21.1`, `platforms/fabric-20.1`
  (milestone 2), ... – one per loader × version family. Shared code between neighbouring versions is
  factored into `platforms/common-*` source sets when the second version arrives.

## 3.3 Design principles

1. **Progressive disclosure.** Every feature has three levels:
   1. *Declare* – builder calls (`.strength(2f)`, `.lightLevel(s -> ...)`).
   2. *Override* – implement a behaviour interface with default methods; override one method.
   3. *Escape* – `handle.unwrap(NativeClass.class)` or a `Versioned` implementation selected per
      Minecraft version.
2. **Portable handles, not copies.** `BlockStateRef`, `Level`, `Player`, `ItemStackRef` are thin
   wrappers (records) around native objects. Cheap to create, always unwrappable.
3. **Defaults = vanilla.** A behaviour with no overrides behaves exactly like vanilla.
4. **Empty means "not me".** Interceptors return `Optional.empty()` to keep the original behaviour,
   so several mods compose on the same block.
5. **The platform owns the hooks.** One generic mixin per hook point (e.g. `BlockItem#getPlacementState`)
   dispatches to all registered interceptors – mods ship no mixins for supported needs.
6. **Data is storage-agnostic.** `BlockData<T>` says *what* is stored; the platform chooses *how*
   (block entity + render data on Fabric/NeoForge today; chunk attachments possible later).
7. **Contexts over long parameter lists.** Callbacks get a context object (`PlacementContext`,
   `BreakContext`, `DropContext`) so adding information later is not a breaking change.
8. **Version is data.** `McVersion`, `VersionRange`, `Versioned<T>` replace the annotation scanning of
   the old refabslab plugin loader with an explicit, testable selector.

## 3.4 The API, step by step

### Step 1 – entrypoint

```java
public final class MyMod implements ModularMod {
    @Override public void initCommon(ModContext mod) { /* content */ }
    @Override public void initClient(ClientContext client) { /* rendering */ }
    @Override public void initServer(ServerContext server) { /* dedicated server only */ }
}
```

Fabric: `"entrypoints": { "modularity": ["com.example.MyMod"] }`. The same instance receives
`initClient`/`initServer`, so fields set in `initCommon` are available there.

| Method | Runs on | Typical use |
|---|---|---|
| `initCommon` | always, first | blocks, items, data, interceptors |
| `initClient` | physical client (also singleplayer) | models, particles, renderers, key bindings |
| `initServer` | dedicated server only | server config, console-only features |

### Step 2 – a block

```java
BlockHandle ruby = mod.block("ruby_block", b -> b.strength(5f).sound(BlockSound.METAL).item());
```

### Step 3 – state and behaviour

```java
static final IntegerProperty CHARGE = Property.integer("charge", 0, 3);

mod.block("battery", b -> b
        .property(CHARGE, 0)
        .lightLevel(state -> state.get(CHARGE) * 5)
        .behaviour(new BlockBehaviour() {
            @Override public List<ItemStackRef> drops(DropContext ctx) { ... }
        }));
```

### Step 4 – data per position

```java
static final BlockData<Owner> OWNER = BlockData.of(Id.of("mymod", "owner"), Owner.CODEC);

mod.block("safe", b -> b.data(OWNER));
level.setData(pos, OWNER, new Owner("Steve"));
level.data(pos, OWNER).ifPresent(...);            // persisted + synced + visible to models
```

### Step 5 – change blocks you don't own

```java
mod.intercept(BlockSelector.type(BlockType.SLAB), new BlockInterceptor() {
    @Override public Optional<PlacementResult> placement(PlacementContext ctx) { ... }
    @Override public Optional<Boolean> canBeReplaced(BlockStateRef existing, PlacementContext ctx) { ... }
});
```

### Step 6 – client

```java
client.blockModel(handle, (view, pos, state, out) -> out.blockState(...));
client.hitParticles(handle, (view, pos, state, face, at) -> ...);
```

### Step 7 – escape hatches

```java
// native access
net.minecraft.world.level.Level nativeLevel = level.unwrap(net.minecraft.world.level.Level.class);

// version-specific implementation, newest compatible wins
Renderer renderer = Versioned.<Renderer>builder()
        .since("1.21.2", NewRenderer::new)
        .since("1.20", OldRenderer::new)
        .select(mod.platform().minecraftVersion());
```

Native code lives in an optional per-target source set of the mod (milestone 2 tooling), so the
common module stays Minecraft-free.

## 3.5 Package map (milestone 1)

| Package | Contents |
|---|---|
| `api` | `ModularMod`, `ModContext`, `Platform`, `Modularity` (static runtime access) |
| `api.server` | `ServerContext` |
| `api.core` | `Id`, `McVersion`, `VersionRange`, `Versioned`, `Handle`, `Loader`, `Side` |
| `api.block` | `BlockDefinition` (+Builder), `BlockHandle`, `BlockRef`, `BlockStateRef`, `BlockBehaviour`, contexts, `BlockInterceptor`, `BlockSelector`, `PlacementResult`, `BlockTag`, `BlockType`, `PushReaction`, `BlockSound` |
| `api.block.property` | `Property` (`IntegerProperty`, `BooleanProperty`, `EnumProperty`), `VanillaProperties`, `SlabType`, `PropertyValue` |
| `api.data` | `BlockData`, `DataCodec`, `DataInput`, `DataOutput`, `MapData` |
| `api.world` | `BlockView`, `Level`, `Player`, `Pos`, `Vec3`, `Direction` |
| `api.item` | `ItemStackRef` |
| `api.client` | `ClientContext`, `DynamicModel`, `ModelOutput`, `HitParticles` |

## 3.6 How the Fabric 1.21.1 platform implements it

| API | Native implementation |
|---|---|
| `ModularMod` discovery | Fabric entrypoint key `modularity`, run from Modularity's own `main`/`client`/`server` entrypoints |
| `mod.block` | `ModularBlock` (or `ModularEntityBlock` when data is declared) registered in `BuiltInRegistries.BLOCK`; properties added through a construction-time `ThreadLocal` |
| `Property` | cached mapping to vanilla `IntegerProperty`/`BooleanProperty`, vanilla properties by key, portable enums as a `Property<String>` of serialized names |
| `BlockData` | `ModularBlockEntity`: NBT under `modularity`, update packet sync, client re-mesh on receive, Fabric `RenderDataBlockEntity` snapshot |
| `PlacementResult.with` | `PendingData` keyed by (level, pos, block), consumed in `BlockEntity#setLevel` – works for client prediction and server |
| `placement` interceptor | mixin `BlockItem#getPlacementState` (all block items, one hook) |
| `canBeReplaced` interceptor | mixin `BlockStateBase#canBeReplaced(BlockPlaceContext)` (all blocks, one hook) |
| behaviour hooks | `getDestroyProgress`, `playerWillDestroy`, `getDrops(LootParams)` overrides |
| `DynamicModel` | `ModelLoadingPlugin#registerBlockStateResolver` → `DynamicBakedModel` delegating to sub-models via FRAPI `emitBlockQuads` (Indigo, Sodium ≥0.6, Iris) |
| `HitParticles` | mixin `ParticleEngine#crack` |

## 3.7 Known limits and decisions

* **Interceptors** cover placement and replacement only in M1; more hooks (use, neighbour update,
  shape, random tick) follow the same pattern in M3.
* **Data storage moved to a block entity** for the refabslab port. Worlds from the old Cardinal
  Components versions (≤1.19) can't be opened with 1.21.1 anyway; a migration path for
  same-version upgrades is a milestone 2 item if needed.
* **Selectors are static** (id, type, namespace) because they are cached per block; tag-based
  decisions belong inside the interceptor (`state.is(tag)`), since tags reload.
