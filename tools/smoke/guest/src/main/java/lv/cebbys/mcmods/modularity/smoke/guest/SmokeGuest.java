package lv.cebbys.mcmods.modularity.smoke.guest;

import lv.cebbys.mcmods.modularity.api.CommonContext;
import lv.cebbys.mcmods.modularity.api.ModularityModCommon;

/**
 * Guest smoke mod: built against the Modularity API only and shipped with a {@code modularity.mod.json} and no loader
 * metadata. Modularity must turn it into a mod of the running loader and run this entrypoint.
 */
public final class SmokeGuest implements ModularityModCommon {
    public static final String PROPERTY = "modularity.smoke.guest";

    @Override
    public void initialize(CommonContext common) {
        System.setProperty(PROPERTY, "initialized");
    }
}
