package lv.cebbys.mcmods.modularity.platform.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicInteger;

/** Entity id counter, to reserve consecutive ids for multipart entities (as the Ender Dragon does). */
@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("ENTITY_COUNTER")
    static AtomicInteger modularity$counter() {
        throw new AssertionError();
    }
}
