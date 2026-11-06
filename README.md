# Modularity

Multi version, multi loader API for Minecraft mod development.

Write your mod once against small, Minecraft-free interfaces. Modularity platforms implement them
for each loader and Minecraft version, own all the mixins, and let you drop down to native code
whenever you need to.

```java
public final class RefabslabMod implements ModularMod {
    @Override
    public void initCommon(ModContext mod) {
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
| `platforms/fabric` | Fabric implementation, one jar per Minecraft version (`gradle/platforms.toml`), layered: `common`, `changes/<range>`, `versions/<version>` ([docs/07](docs/07-multi-version-build.md)) | yes |
| `build-logic` | Gradle plugins `lv.cebbys.modularity*` that build the platforms | no |

## Building

```sh
./gradlew buildAll collectJars                   # everything; mod jars in build/libs-all/ (needs Fabric/Mojang mavens)
./gradlew build -Pmodularity.platforms=21.1,26.3 # only these platforms
./gradlew build -Pmodularity.platforms=none      # API + testkit only (Maven Central only)
./gradlew publishToMavenLocal                    # for mods built against a local Modularity
```

## Documentation

1. [Research of the existing projects](docs/01-research.md)
2. [Requirements](docs/02-requirements.md)
3. [API design](docs/03-api-design.md)
4. [Milestones](docs/04-milestones.md)

Status: **milestone 1** – core block API, Fabric 1.21.1 platform, refabslab ported
([CebbysMods/refabslab](https://github.com/CebbysMods/refabslab)); **milestone 1.5** – entities, items,
networking, transfer and client APIs, used by the vehicle mod
([CebbysMods/immersive-vehicles](https://github.com/CebbysMods/immersive-vehicles)).
