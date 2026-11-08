package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.block.BlockInterceptor;
import lv.cebbys.mcmods.modularity.api.block.BlockSelector;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.data.DataCodec;

import java.util.function.Consumer;

/** Everything a mod can do during common initialization. Ids are created in the mod namespace. */
public interface ModContext {
    String modId();

    Platform platform();

    System.Logger logger();

    default Id id(String path) {
        return Id.of(modId(), path);
    }

    /** Declares data stored per block position (see {@link BlockData}). */
    default <T> BlockData<T> blockData(String name, DataCodec<T> codec) {
        return BlockData.of(id(name), codec);
    }

    /** Registers a new block. */
    BlockHandle block(String name, Consumer<BlockDefinition.Builder> definition);

    /** Changes behaviour of existing blocks (vanilla or other mods). */
    void intercept(BlockSelector selector, BlockInterceptor interceptor);
}
