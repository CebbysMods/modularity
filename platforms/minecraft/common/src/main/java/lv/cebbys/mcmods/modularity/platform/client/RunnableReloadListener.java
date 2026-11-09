package lv.cebbys.mcmods.modularity.platform.client;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Runs a no-argument action on the game thread after a client resource reload. */
public final class RunnableReloadListener implements PreparableReloadListener {
    private final Runnable action;

    public RunnableReloadListener(Runnable action) {
        this.action = action;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resources,
                                          ProfilerFiller prepareProfiler, ProfilerFiller reloadProfiler,
                                          Executor background, Executor game) {
        return barrier.wait((Void) null).thenRunAsync(action, game);
    }
}
