package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.world.NativeBlockView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;

/** "Block being mined" particles whose texture a portable {@code HitParticles} handler chooses (used by mixins). */
public final class HitParticles {
    private HitParticles() {
    }

    /** Spawns one hit particle like vanilla, with the handler's state; false if the block has no handler. */
    public static boolean spawn(ClientLevel level, RandomSource random, BlockPos pos, Direction side) {
        var state = level.getBlockState(pos);
        var handler = ClientRegistry.hitParticles(state.getBlock());
        if (handler == null || state.getRenderShape() == RenderShape.INVISIBLE) {
            return false;
        }
        // Same placement math as vanilla.
        var box = state.getShape(level, pos).bounds();
        double x = pos.getX() + random.nextDouble() * (box.maxX - box.minX - 0.2D) + 0.1D + box.minX;
        double y = pos.getY() + random.nextDouble() * (box.maxY - box.minY - 0.2D) + 0.1D + box.minY;
        double z = pos.getZ() + random.nextDouble() * (box.maxZ - box.minZ - 0.2D) + 0.1D + box.minZ;
        switch (side) {
            case DOWN -> y = pos.getY() + box.minY - 0.1D;
            case UP -> y = pos.getY() + box.maxY + 0.1D;
            case NORTH -> z = pos.getZ() + box.minZ - 0.1D;
            case SOUTH -> z = pos.getZ() + box.maxZ + 0.1D;
            case WEST -> x = pos.getX() + box.minX - 0.1D;
            case EAST -> x = pos.getX() + box.maxX + 0.1D;
        }
        var chosen = handler.particleState(new NativeBlockView(level), Wrap.pos(pos), Wrap.state(state),
                Wrap.direction(side), new Vec3(x, y, z));
        var particle = Minecraft.getInstance().particleEngine
                .createParticle(new BlockParticleOption(ParticleTypes.BLOCK, Wrap.state(chosen)), x, y, z, 0.0D, 0.0D, 0.0D);
        if (particle != null) {
            particle.setPower(0.2F).scale(0.6F);
        }
        return true;
    }
}
