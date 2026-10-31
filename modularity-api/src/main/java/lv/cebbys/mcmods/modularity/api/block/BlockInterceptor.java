package lv.cebbys.mcmods.modularity.api.block;

import java.util.Optional;

/**
 * Changes the behaviour of blocks you do not own (vanilla or other mods) without writing mixins.
 * Register with {@code mod.intercept(BlockSelector.type(BlockType.SLAB), interceptor)}.
 *
 * <p>Each hook is matched against the block whose vanilla method is being intercepted. Returning
 * {@link Optional#empty()} keeps the original behaviour, so several mods can intercept the same
 * block; the first non-empty answer wins.
 */
public interface BlockInterceptor {
    /**
     * Intercepts {@code getStateForPlacement} of the block being placed (the selector is matched
     * against the block of the item in hand).
     */
    default Optional<PlacementResult> placement(PlacementContext ctx) {
        return Optional.empty();
    }

    /**
     * Intercepts {@code canBeReplaced} of the block that already is in the world (the selector is
     * matched against {@code existing}).
     */
    default Optional<Boolean> canBeReplaced(BlockStateRef existing, PlacementContext ctx) {
        return Optional.empty();
    }
}
