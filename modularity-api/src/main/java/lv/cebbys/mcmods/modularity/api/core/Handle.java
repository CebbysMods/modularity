package lv.cebbys.mcmods.modularity.api.core;

/**
 * Every portable game object wraps a native Minecraft object. {@link #unwrap(Class)} is the escape
 * hatch: when the portable API does not cover something yet, version specific code can still reach
 * the real object (e.g. {@code level.unwrap(net.minecraft.world.level.Level.class)}).
 */
public interface Handle {
    Object handle();

    default <T> T unwrap(Class<T> type) {
        return type.cast(handle());
    }
}
