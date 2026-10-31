package lv.cebbys.mcmods.modularity.fabric.mixin.client;

import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.client.ClientRegistry;
import lv.cebbys.mcmods.modularity.fabric.world.NativeBlockView;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets {@code HitParticles} choose the texture of "block being mined" particles. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    protected ClientLevel level;

    @Shadow
    @Final
    private RandomSource random;

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void modularity$hitParticles(BlockPos pos, Direction side, CallbackInfo ci) {
        var state = level.getBlockState(pos);
        var handler = ClientRegistry.hitParticles(state.getBlock());
        if (handler == null || state.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        // Same placement math as vanilla ParticleEngine#crack.
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
        var particle = ((ParticleEngine) (Object) this)
                .createParticle(new BlockParticleOption(ParticleTypes.BLOCK, Wrap.state(chosen)), x, y, z, 0.0D, 0.0D, 0.0D);
        if (particle != null) {
            particle.setPower(0.2F).scale(0.6F);
        }
        ci.cancel();
    }
}
