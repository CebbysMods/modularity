package lv.cebbys.mcmods.modularity.fabric.mixin;

import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.block.Contexts;
import lv.cebbys.mcmods.modularity.fabric.block.Interceptors;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dispatches {@code BlockInterceptor#canBeReplaced} for every block. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z", at = @At("HEAD"), cancellable = true)
    private void modularity$interceptCanBeReplaced(BlockPlaceContext context, CallbackInfoReturnable<Boolean> cir) {
        if (Interceptors.isEmpty()) {
            return;
        }
        var interceptors = Interceptors.forBlock(getBlock());
        if (interceptors.isEmpty()) {
            return;
        }
        var existing = Wrap.state(asState());
        var portable = Contexts.placement(context);
        for (var interceptor : interceptors) {
            var answer = interceptor.canBeReplaced(existing, portable);
            if (answer.isPresent()) {
                cir.setReturnValue(answer.get());
                return;
            }
        }
    }
}
