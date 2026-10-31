package lv.cebbys.mcmods.modularity.fabric;

import lv.cebbys.mcmods.modularity.api.ModContext;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.block.BlockInterceptor;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.block.BlockSelector;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.fabric.block.Interceptors;
import lv.cebbys.mcmods.modularity.fabric.block.ModularBlock;
import lv.cebbys.mcmods.modularity.fabric.block.ModularBlockEntity;
import lv.cebbys.mcmods.modularity.fabric.block.ModularEntityBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.item.CreativeTab;
import lv.cebbys.mcmods.modularity.api.item.ItemDefinition;
import lv.cebbys.mcmods.modularity.api.item.ItemHandle;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.network.Channel;
import lv.cebbys.mcmods.modularity.api.sound.SoundHandle;
import lv.cebbys.mcmods.modularity.fabric.entity.EntityTypes;
import lv.cebbys.mcmods.modularity.fabric.item.ModularItem;
import lv.cebbys.mcmods.modularity.fabric.network.FabricChannel;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

final class FabricModContext implements ModContext {
    private final String modId;
    private final System.Logger logger;

    FabricModContext(String modId) {
        this.modId = modId;
        this.logger = System.getLogger(modId);
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public Platform platform() {
        return FabricRuntime.INSTANCE;
    }

    @Override
    public System.Logger logger() {
        return logger;
    }

    @Override
    public BlockHandle block(String name, Consumer<BlockDefinition.Builder> spec) {
        BlockDefinition.Builder builder = BlockDefinition.builder();
        spec.accept(builder);
        BlockDefinition definition = builder.build();
        Id id = id(name);
        var location = Wrap.rl(id);

        ModularBlock block = Registry.register(BuiltInRegistries.BLOCK, location, ModularBlock.create(definition));
        if (block instanceof ModularEntityBlock entityBlock) {
            BlockEntityType<ModularBlockEntity> type = BlockEntityType.Builder
                    .<ModularBlockEntity>of((pos, state) -> (ModularBlockEntity) entityBlock.newBlockEntity(pos, state), entityBlock)
                    .build(null);
            entityBlock.bindType(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, location, type));
        }
        if (definition.hasItem()) {
            Registry.register(BuiltInRegistries.ITEM, location, new BlockItem(block, new Item.Properties()));
        }
        BlockRef ref = Wrap.block(block);
        return new BlockHandle() {
            @Override
            public Id id() {
                return id;
            }

            @Override
            public BlockDefinition definition() {
                return definition;
            }

            @Override
            public BlockRef get() {
                return ref;
            }
        };
    }

    @Override
    public void intercept(BlockSelector selector, BlockInterceptor interceptor) {
        Interceptors.register(selector, interceptor);
    }

    @Override
    public EntityHandle entity(String name, Consumer<EntityDefinition.Builder> spec) {
        EntityDefinition.Builder builder = EntityDefinition.builder();
        spec.accept(builder);
        EntityDefinition definition = builder.build();
        Id id = id(name);
        EntityTypes.register(Wrap.rl(id), definition);
        return new EntityHandle() {
            @Override
            public Id id() {
                return id;
            }

            @Override
            public EntityDefinition definition() {
                return definition;
            }
        };
    }

    @Override
    public ItemHandle item(String name, Consumer<ItemDefinition.Builder> spec) {
        ItemDefinition.Builder builder = ItemDefinition.builder();
        spec.accept(builder);
        ItemDefinition definition = builder.build();
        Id id = id(name);
        ModularItem item = Registry.register(BuiltInRegistries.ITEM, Wrap.rl(id), new ModularItem(definition));
        if (definition.creativeTab() != null) {
            ItemGroupEvents.modifyEntriesEvent(tab(definition.creativeTab())).register(entries -> entries.accept(item));
        }
        return new ItemHandle() {
            @Override
            public Id id() {
                return id;
            }

            @Override
            public ItemDefinition definition() {
                return definition;
            }

            @Override
            public ItemStackRef stack(int count) {
                return Wrap.stack(new ItemStack(item, count));
            }
        };
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> tab(CreativeTab tab) {
        return switch (tab) {
            case BUILDING_BLOCKS -> CreativeModeTabs.BUILDING_BLOCKS;
            case COLORED_BLOCKS -> CreativeModeTabs.COLORED_BLOCKS;
            case NATURAL_BLOCKS -> CreativeModeTabs.NATURAL_BLOCKS;
            case FUNCTIONAL_BLOCKS -> CreativeModeTabs.FUNCTIONAL_BLOCKS;
            case REDSTONE_BLOCKS -> CreativeModeTabs.REDSTONE_BLOCKS;
            case TOOLS_AND_UTILITIES -> CreativeModeTabs.TOOLS_AND_UTILITIES;
            case COMBAT -> CreativeModeTabs.COMBAT;
            case FOOD_AND_DRINKS -> CreativeModeTabs.FOOD_AND_DRINKS;
            case INGREDIENTS -> CreativeModeTabs.INGREDIENTS;
            case SPAWN_EGGS -> CreativeModeTabs.SPAWN_EGGS;
        };
    }

    @Override
    public SoundHandle sound(String name) {
        Id id = id(name);
        var location = Wrap.rl(id);
        Registry.register(BuiltInRegistries.SOUND_EVENT, location, SoundEvent.createVariableRangeEvent(location));
        return () -> id;
    }

    @Override
    public <T> Channel<T> channel(String name, DataCodec<T> codec) {
        return new FabricChannel<>(id(name), codec);
    }
}
