package lv.cebbys.mcmods.modularity.api.server;

import lv.cebbys.mcmods.modularity.api.Platform;

/**
 * Everything a mod can do during dedicated server initialization. Server-only features (e.g.
 * server configs, console commands) are added here in later milestones.
 */
public interface ServerContext {
    String modId();

    Platform platform();
}
