package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.core.Id;

/** Reference to a block tag such as {@code minecraft:guarded_by_piglins}. Tags are data driven. */
public record BlockTag(Id id) {
    public static final BlockTag GUARDED_BY_PIGLINS = of(Id.minecraft("guarded_by_piglins"));
    public static final BlockTag MINEABLE_PICKAXE = of(Id.minecraft("mineable/pickaxe"));
    public static final BlockTag MINEABLE_AXE = of(Id.minecraft("mineable/axe"));
    public static final BlockTag SLABS = of(Id.minecraft("slabs"));

    public static BlockTag of(Id id) {
        return new BlockTag(id);
    }

    public static BlockTag of(String id) {
        return new BlockTag(Id.parse(id));
    }
}
