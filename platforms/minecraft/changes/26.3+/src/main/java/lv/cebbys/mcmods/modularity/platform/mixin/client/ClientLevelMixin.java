package lv.cebbys.mcmods.modularity.platform.mixin.client;

import lv.cebbys.mcmods.modularity.platform.client.HitParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets {@code HitParticles} choose the texture of "block being mined" particles. 1.21.9: they are spawned by
 * {@code ClientLevel#addBreakingBlockEffect} (was {@code ParticleEngine#crack}).
 * 26.3: the particles are spawned by {@code addBreakingParticles}, the sound separately.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "addBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void modularity$hitParticles(BlockPos pos, Direction side, BlockState state, CallbackInfo ci) {
        ClientLevel self = (ClientLevel) (Object) this;
        if (HitParticles.spawn(self, self.getRandom(), pos, side)) {
            ci.cancel();
        }
    }
}
