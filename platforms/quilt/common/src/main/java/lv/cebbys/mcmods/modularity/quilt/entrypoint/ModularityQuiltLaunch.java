package lv.cebbys.mcmods.modularity.quilt.entrypoint;

import lv.cebbys.mcmods.modularity.loom.entrypoint.LoomEntrypoints;
import lv.cebbys.mcmods.modularity.loom.guest.GuestCandidates;
import lv.cebbys.mcmods.modularity.platform.guest.GuestDescriptor;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Quilt launch entrypoint: runs before the game starts. Quilt has no guest-mod injection (docs/08), so it only reports
 * the guests it finds and does not load them.
 */
public final class ModularityQuiltLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    @Override
    public void onPreLaunch(ModContainer mod) {
        LoomEntrypoints.launch("quilt");
        List<GuestDescriptor> guests = GuestCandidates.find();
        if (!guests.isEmpty()) {
            // LoaderInjection uses Fabric Loader internals, which Quilt Loader does not have (docs/08).
            LOGGER.error("[modularity] Guest mods are not supported on Quilt yet; not loaded: {}",
                    guests.stream().map(GuestDescriptor::id).toList());
        }
        LoomEntrypoints.launchMods("quilt");
    }
}
