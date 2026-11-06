package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;

/** A block entity that hands render data to models, also while chunks mesh off the game thread. */
public interface RenderDataHolder extends RenderDataBlockEntity {
}
