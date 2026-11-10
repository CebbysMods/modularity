package lv.cebbys.mcmods.modularity.fabric.entrypoint;

import lv.cebbys.mcmods.modularity.fabric.guest.FabricGuestJson;
import lv.cebbys.mcmods.modularity.fabric.guest.LoaderInjection;
import lv.cebbys.mcmods.modularity.loom.entrypoint.LoomEntrypoints;
import lv.cebbys.mcmods.modularity.loom.guest.GuestCandidates;
import lv.cebbys.mcmods.modularity.platform.guest.GuestDescriptor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fabric launch entrypoint: loads guest mods found by {@link GuestCandidates}. Each becomes a real Fabric mod before
 * the game starts; its {@code modularity} entrypoints then run with all other Modularity mods.
 */
public final class ModularityFabricLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity");

    @Override
    public void onPreLaunch() {
        LoomEntrypoints.launch("fabric");
        Map<String, GuestDescriptor> byId = new LinkedHashMap<>();
        for (GuestDescriptor guest : GuestCandidates.find()) {
            if (FabricLoader.getInstance().isModLoaded(guest.id()) || byId.containsKey(guest.id())) {
                throw new IllegalStateException("Modularity: guest mod '" + guest.id() + "' (" + guest.origin()
                        + ") is already loaded");
            }
            byId.put(guest.id(), guest);
        }
        for (GuestDescriptor guest : byId.values()) {
            try {
                LoaderInjection.addMod(guest.origin(), FabricGuestJson.of(guest).toString());
            } catch (Exception e) {
                throw new IllegalStateException("Modularity: cannot add guest mod '" + guest.id() + "'", e);
            }
            LOGGER.info("Loaded guest mod {} {} from {}", guest.id(), guest.version(),
                    guest.origin().getFileName());
        }
        byId.values().forEach(ModularityFabricLaunch::checkDependencies);
        LoomEntrypoints.launchMods("fabric");
    }

    /** Fabric resolved dependencies before guests existed; the same rules, checked here. */
    private static void checkDependencies(GuestDescriptor guest) {
        List<String> problems = new ArrayList<>();
        guest.depends().forEach((id, range) -> {
            Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(id);
            if (mod.isEmpty()) {
                problems.add("requires " + id + " " + range + ", which is missing");
            } else if (!matches(range, mod.get())) {
                problems.add("requires " + id + " " + range + ", found "
                        + mod.get().getMetadata().getVersion().getFriendlyString());
            }
        });
        guest.map("breaks").forEach((id, range) -> FabricLoader.getInstance().getModContainer(id)
                .filter(mod -> matches(range, mod))
                .ifPresent(mod -> problems.add("is incompatible with " + id + " "
                        + mod.getMetadata().getVersion().getFriendlyString())));
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Modularity: guest mod '" + guest.id() + "' "
                    + String.join("; ", problems));
        }
    }

    private static boolean matches(String range, ModContainer mod) {
        try {
            return VersionPredicate.parse(range).test(mod.getMetadata().getVersion());
        } catch (Exception e) {
            throw new IllegalStateException("Modularity: invalid version range '" + range + "'", e);
        }
    }
}
