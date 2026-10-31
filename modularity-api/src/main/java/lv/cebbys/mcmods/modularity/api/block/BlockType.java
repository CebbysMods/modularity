package lv.cebbys.mcmods.modularity.api.block;

/**
 * Vanilla block families. Platforms map each one to the vanilla base class (e.g. {@code SLAB} to
 * {@code SlabBlock}), so modded blocks extending those classes match too.
 */
public enum BlockType {
    SLAB, STAIRS, WALL, FENCE, FENCE_GATE, DOOR, TRAPDOOR, BUTTON, PRESSURE_PLATE, LEAVES, FALLING
}
