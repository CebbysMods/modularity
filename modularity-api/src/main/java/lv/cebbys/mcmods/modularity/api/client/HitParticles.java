package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

/** Decides how the particles of a block being mined look. */
@FunctionalInterface
public interface HitParticles {
    /**
     * @param state    the block being hit
     * @param face     the face being hit
     * @param particle where the particle spawns
     * @return the state whose texture the particle uses
     */
    BlockStateRef particleState(BlockView view, Pos pos, BlockStateRef state, Direction face, Vec3 particle);
}
