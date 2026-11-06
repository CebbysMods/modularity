package lv.cebbys.mcmods.modularity.fabric.mixin.client;

import lv.cebbys.mcmods.modularity.fabric.client.HitParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
        if (HitParticles.spawn(level, random, pos, side)) {
            ci.cancel();
        }
    }
}
