# Modularity

Multi version, multi loader API for Minecraft mod development.

Write your mod once against small, Minecraft-free interfaces. Modularity platforms implement them
for each loader and Minecraft version, own all the mixins, and let you drop down to native code
whenever you need to.

```java
public final class RefabslabMod implements ModularMod {
    @Override
    public void init(ModContext mod) {
        doubleSlab = mod.block("double_slab_block", block -> block
                .property(LIGHT_LEVEL, 0)
                .lightLevel(state -> state.get(LIGHT_LEVEL))
                .pushReaction(PushReaction.BLOCK)
                .data(DOUBLE_SLAB_DATA)
                .behaviour(new DoubleSlabBehaviour()));

        mod.intercept(BlockSelector.type(BlockType.SLAB), new SlabCombining());
    }

    @Override
    public void initClient(ClientContext client) {
        client.blockModel(doubleSlab, new DoubleSlabModel());
        client.hitParticles(doubleSlab, new DoubleSlabParticles());
    }
}
```

## Modules

| Module | What | Needs Minecraft |
|---|---|---|
| `modularity-api` | The API (Java 17) | no |
| `modularity-testkit` | In-memory game to unit test mods | no |
| `platforms/fabric-1.21.1` | Fabric 1.21.1 implementation | yes |

## Building

```sh
./gradlew build                                  # everything (needs Fabric/Mojang mavens)
./gradlew build -Pmodularity.platforms=none      # API + testkit only (Maven Central only)
./gradlew publishToMavenLocal                    # for mods built against a local Modularity
```

## Documentation

1. [Research of the existing projects](docs/01-research.md)
2. [Requirements](docs/02-requirements.md)
3. [API design](docs/03-api-design.md)
4. [Milestones](docs/04-milestones.md)

Status: **milestone 1** – core block API, Fabric 1.21.1 platform, refabslab ported
([CebbysMods/refabslab](https://github.com/CebbysMods/refabslab)).
