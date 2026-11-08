package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.core.Loader;
import lv.cebbys.mcmods.modularity.api.core.McVersion;
import lv.cebbys.mcmods.modularity.api.core.Side;

import java.util.Optional;

/** Facts about the running game. */
public interface Platform {
    Loader loader();

    McVersion minecraftVersion();

    Side side();

    boolean isModLoaded(String modId);

    Optional<String> modVersion(String modId);

    boolean isDevelopment();
}
