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
}
