package lv.cebbys.mcmods.modularity.platform.mixin;

import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.entity.ModularPart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Resolves part ids sent by clients (attack / interact packets) to the part entities: parts use
 * the ids right after their parent's.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Shadow
    public abstract Entity getEntity(int id);

    @Inject(method = "getEntityOrPart", at = @At("RETURN"), cancellable = true)
    private void modularity$part(int id, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        for (int k = 1; k <= EntityDefinition.Builder.MAX_PARTS; k++) {
            if (getEntity(id - k) instanceof ModularEntity m && m.isMultipart()) {
                ModularPart part = m.partById(id);
                if (part != null) {
                    cir.setReturnValue(part);
                }
                return;
            }
        }
    }
}
