package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;

import java.util.Objects;
import java.util.Optional;

/**
 * Static access to the running platform, for code that has no context object at hand (behaviours,
 * models). Installed by the platform implementation before any mod is initialized.
 */
public final class Modularity {
    private static volatile Runtime runtime;

    private Modularity() {
    }

    public static Platform platform() {
        return runtime().platform();
    }

    /** Looks up any registered block by id. */
    public static Optional<BlockRef> block(Id id) {
        return runtime().block(id);
    }

    public static ItemStackRef emptyStack() {
        return runtime().emptyStack();
    }

    /** Internal: called by the platform implementation. */
    public static void install(Runtime installed) {
        runtime = Objects.requireNonNull(installed, "runtime");
    }

    private static Runtime runtime() {
        Runtime current = runtime;
        if (current == null) {
            throw new IllegalStateException("Modularity platform is not installed");
        }
        return current;
    }

    /** Implemented by platforms. */
    public interface Runtime {
        Platform platform();

        Optional<BlockRef> block(Id id);

        ItemStackRef emptyStack();
    }
}
