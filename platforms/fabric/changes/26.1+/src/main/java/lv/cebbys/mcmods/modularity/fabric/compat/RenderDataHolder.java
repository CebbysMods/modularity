package lv.cebbys.mcmods.modularity.fabric.compat;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;

/**
 * A block entity that hands render data to models, also while chunks mesh off the game thread. 26.1: Fabric's
 * block view API is called block getter.
 */
public interface RenderDataHolder extends RenderDataBlockEntity {
}
