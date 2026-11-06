package lv.cebbys.mcmods.modularity.platform.compat;

/**
 * A block entity that hands render data to models. The loader layers replace this file to extend their own render
 * data interface (Fabric: {@code RenderDataBlockEntity}), so the data also reaches chunks meshed off the game thread.
 */
public interface RenderDataHolder {
    Object getRenderData();
}
