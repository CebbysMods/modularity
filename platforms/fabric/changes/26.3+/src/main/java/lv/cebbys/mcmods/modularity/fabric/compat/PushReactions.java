package lv.cebbys.mcmods.modularity.fabric.compat;

import lv.cebbys.mcmods.modularity.api.block.PushReaction;

/** Piston behaviour of blocks: portable value to Minecraft's. 26.3: the values were renamed. */
public final class PushReactions {
    private PushReactions() {
    }

    public static net.minecraft.world.level.material.PushReaction toNative(PushReaction reaction) {
        return switch (reaction) {
            case NORMAL -> net.minecraft.world.level.material.PushReaction.PUSH_PULL;
            case DESTROY -> net.minecraft.world.level.material.PushReaction.POPPED;
            case BLOCK -> net.minecraft.world.level.material.PushReaction.IMMOVEABLE;
            case IGNORE -> net.minecraft.world.level.material.PushReaction.IGNORE_ENTITY;
            case PUSH_ONLY -> net.minecraft.world.level.material.PushReaction.PUSH;
        };
    }
}
