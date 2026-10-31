package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.core.Id;

/**
 * A block you registered. Registration may be deferred by the loader (NeoForge), so the actual block
 * is resolved lazily by {@link #get()}; keep the handle in a static field and call {@code get()}
 * when needed.
 */
public interface BlockHandle {
    Id id();

    BlockDefinition definition();

    /** @throws IllegalStateException when called before the loader registered the block */
    BlockRef get();

    default BlockStateRef defaultState() {
        return get().defaultState();
    }
}
