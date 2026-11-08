package lv.cebbys.mcmods.modularity.fabric.mixin;

import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.block.Contexts;
import lv.cebbys.mcmods.modularity.fabric.block.Interceptors;
import lv.cebbys.mcmods.modularity.fabric.block.PendingData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dispatches {@code BlockInterceptor#placement} for every block item placement. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void modularity$interceptPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        if (Interceptors.isEmpty()) {
            return;
        }
        var portable = Contexts.placement(context);
        for (var interceptor : Interceptors.forBlock(getBlock())) {
            var result = interceptor.placement(portable);
            if (result.isPresent()) {
                BlockState state = Wrap.state(result.get().state());
                if (canPlace(context, state)) {
                    PendingData.put(context.getLevel(), context.getClickedPos(), state.getBlock(), result.get().data());
                    cir.setReturnValue(state);
                }
                return;
            }
        }
    }
}
