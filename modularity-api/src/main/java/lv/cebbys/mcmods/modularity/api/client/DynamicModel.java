package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Pos;

/** A block model decided at render time, for example from {@code view.data(pos, ...)}. */
@FunctionalInterface
public interface DynamicModel {
    void emit(BlockView view, Pos pos, BlockStateRef state, ModelOutput out);

    /** Texture (in the block atlas) used for particles when no position is known. */
    default Id particleTexture() {
        return Id.minecraft("block/stone");
    }
}
