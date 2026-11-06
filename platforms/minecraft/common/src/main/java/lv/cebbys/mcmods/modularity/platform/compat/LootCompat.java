package lv.cebbys.mcmods.modularity.platform.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Reading loot parameters. */
public final class LootCompat {
    private LootCompat() {
    }

    /** The tool that broke the block, or {@code null}. */
    public static ItemStack tool(LootParams.Builder params) {
        return params.getOptionalParameter(LootContextParams.TOOL);
    }
}
