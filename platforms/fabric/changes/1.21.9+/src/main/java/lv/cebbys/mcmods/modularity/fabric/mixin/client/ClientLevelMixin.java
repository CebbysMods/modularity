package lv.cebbys.mcmods.modularity.fabric.mixin.client;

import lv.cebbys.mcmods.modularity.fabric.client.HitParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets {@code HitParticles} choose the texture of "block being mined" particles. 1.21.9: they are spawned by
 * {@code ClientLevel#addBreakingBlockEffect} (was {@code ParticleEngine#crack}).
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void modularity$hitParticles(BlockPos pos, Direction side, CallbackInfo ci) {
        ClientLevel self = (ClientLevel) (Object) this;
        if (HitParticles.spawn(self, self.random, pos, side)) {
            ci.cancel();
        }
    }
}
