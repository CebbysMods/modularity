package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Game logic calls. */
public final class GameCompat {
    private GameCompat() {
    }

    /** The "it worked" result of a use or interaction, as vanilla returns it on that side. */
    public static InteractionResult success(boolean clientSide) {
        return InteractionResult.sidedSuccess(clientSide);
    }

    /** Minecraft's result for a portable one; SUCCESS as vanilla returns it on that side. */
    public static InteractionResult result(lv.cebbys.mcmods.modularity.api.core.InteractionResult result,
                                           boolean clientSide) {
        return switch (result) {
            case PASS -> InteractionResult.PASS;
            case SUCCESS -> success(clientSide);
            case CONSUME -> InteractionResult.CONSUME;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /** Piglins near {@code player} get angry, as if they saw a guarded block break. */
    public static void angerNearbyPiglins(Level level, Player player) {
        PiglinAi.angerNearbyPiglins(player, false);
    }
}
