package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.client.ClientContext;

/**
 * The client lifecycle of a Modularity mod: models, particles, renderers. It runs on the physical
 * client only, after {@link ModularityModCommon#initialize(CommonContext)}.
 *
 * <p>A mod implements this interface only when it has client work; the method is default, so the
 * implementation may override nothing.</p>
 */
public interface ModularityModClient {
    /** Client initialization: models, particles, renderers. Never runs on a dedicated server. */
    default void initialize(ClientContext client) {
    }
}
