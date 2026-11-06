package lv.cebbys.mcmods.modularity.platform.compat;

import lv.cebbys.mcmods.modularity.api.block.PushReaction;

/** Piston behaviour of blocks: portable value to Minecraft's. */
public final class PushReactions {
    private PushReactions() {
    }

    public static net.minecraft.world.level.material.PushReaction toNative(PushReaction reaction) {
        return switch (reaction) {
            case NORMAL -> net.minecraft.world.level.material.PushReaction.NORMAL;
            case DESTROY -> net.minecraft.world.level.material.PushReaction.DESTROY;
            case BLOCK -> net.minecraft.world.level.material.PushReaction.BLOCK;
            case IGNORE -> net.minecraft.world.level.material.PushReaction.IGNORE;
            case PUSH_ONLY -> net.minecraft.world.level.material.PushReaction.PUSH_ONLY;
        };
    }
}
