package lv.cebbys.mcmods.modularity.platform;

import lv.cebbys.mcmods.modularity.api.ModContext;
import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.block.BlockInterceptor;
import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.block.BlockSelector;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.platform.block.Interceptors;
import lv.cebbys.mcmods.modularity.platform.block.ModularBlock;
import lv.cebbys.mcmods.modularity.platform.block.ModularEntityBlock;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.loader.LoaderHooks;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import lv.cebbys.mcmods.modularity.platform.compat.RegistryCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;

import lv.cebbys.mcmods.modularity.api.data.DataCodec;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.item.CreativeTab;
import lv.cebbys.mcmods.modularity.api.item.ItemDefinition;
import lv.cebbys.mcmods.modularity.api.item.ItemHandle;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.network.Channel;
import lv.cebbys.mcmods.modularity.api.sound.SoundHandle;
import lv.cebbys.mcmods.modularity.platform.entity.EntityTypes;
import lv.cebbys.mcmods.modularity.platform.item.ModularItem;
import lv.cebbys.mcmods.modularity.platform.network.McChannel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;

/** The {@link ModContext} of one mod, the same on every loader; loader work goes through {@link Loaders#hooks()}. */
final class McModContext implements ModContext {
    private final String modId;
    private final System.Logger logger;

    McModContext(String modId) {
        this.modId = modId;
        this.logger = System.getLogger(modId);
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public Platform platform() {
        return McRuntime.INSTANCE;
    }

    @Override
    public System.Logger logger() {
        return logger;
    }

    @Override
    public BlockHandle block(String name, Consumer<BlockDefinition.Builder> spec) {
        BlockDefinition definition = build(BlockDefinition.builder(), spec, BlockDefinition.Builder::build);
        Id id = id(name);
        LoaderHooks hooks = Loaders.hooks();
        ModularBlock block = hooks.register(BuiltInRegistries.BLOCK, id, ModularBlock.create(id, definition));
        if (block instanceof ModularEntityBlock entityBlock) {
            entityBlock.bindType(hooks.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, hooks.blockEntityType(entityBlock)));
        }
        if (definition.hasItem()) {
            hooks.register(BuiltInRegistries.ITEM, id, new BlockItem(block, RegistryCompat.itemProperties(id)));
        }
        return new RegisteredBlock(id, definition, Wrap.block(block));
    }

    @Override
    public void intercept(BlockSelector selector, BlockInterceptor interceptor) {
        Interceptors.register(selector, interceptor);
    }

    @Override
    public EntityHandle entity(String name, Consumer<EntityDefinition.Builder> spec) {
        EntityDefinition definition = build(EntityDefinition.builder(), spec, EntityDefinition.Builder::build);
        Id id = id(name);
        EntityTypes.register(id, definition);
        return new RegisteredEntity(id, definition);
    }

    @Override
    public ItemHandle item(String name, Consumer<ItemDefinition.Builder> spec) {
        ItemDefinition definition = build(ItemDefinition.builder(), spec, ItemDefinition.Builder::build);
        Id id = id(name);
        ModularItem item = Loaders.hooks().register(BuiltInRegistries.ITEM, id, new ModularItem(id, definition));
        if (definition.creativeTab() != null) {
            Loaders.hooks().addToCreativeTab(tab(definition.creativeTab()), item);
        }
        return new RegisteredItem(id, definition, item);
    }

    private static <B, D> D build(B builder, Consumer<B> spec, Function<B, D> build) {
        spec.accept(builder);
        return build.apply(builder);
    }

    private record RegisteredBlock(Id id, BlockDefinition definition, BlockRef get) implements BlockHandle {
    }

    private record RegisteredEntity(Id id, EntityDefinition definition) implements EntityHandle {
    }

    private record RegisteredItem(Id id, ItemDefinition definition, ModularItem item) implements ItemHandle {
        @Override
        public ItemStackRef stack(int count) {
            return Wrap.stack(new ItemStack(item, count));
        }
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
        Loaders.hooks().register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(Ids.rl(id)));
        return () -> id;
    }

    @Override
    public <T> Channel<T> channel(String name, DataCodec<T> codec) {
        return new McChannel<>(id(name), codec);
    }
}
