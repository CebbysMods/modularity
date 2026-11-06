package lv.cebbys.mcmods.modularity.fabric.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Game logic calls. 1.21.2: one SUCCESS result for both sides; piglin anger needs the server level. */
public final class GameCompat {
    private GameCompat() {
    }

    /** The "it worked" result of a use or interaction, as vanilla returns it on that side. */
    public static InteractionResult success(boolean clientSide) {
        return InteractionResult.SUCCESS;
    }

    /** Piglins near {@code player} get angry, as if they saw a guarded block break. */
    public static void angerNearbyPiglins(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            PiglinAi.angerNearbyPiglins(serverLevel, player, false);
        }
    }
}
