package lv.cebbys.mcmods.modularity.platform.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicInteger;

/** Entity id counter, to reserve consecutive ids for multipart entities. 26.2: moved from {@code Entity}. */
@Mixin(ServerLevel.class)
public interface ServerLevelAccessor {
    @Accessor("ENTITY_COUNTER")
    static AtomicInteger modularity$counter() {
        throw new AssertionError();
    }
}
