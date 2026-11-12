package lv.cebbys.mcmods.modularity.platform.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HitParticles;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client registrations collected from all mods, keyed by id and not by the native object. The Forge family runs the
 * client stage before its block and entity registries are readable, so a registration must never resolve the native
 * object while the stage runs (docs/07 7.8). The loader resolves the id when its own registry is open.
 */
public final class ClientRegistry {
    static final Map<Id, DynamicModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<Id, HitParticles> HIT_PARTICLES = new ConcurrentHashMap<>();

    private ClientRegistry() {
    }

    static void model(Id id, DynamicModel model) {
        MODELS.put(id, model);
    }

    static void hitParticles(Id id, HitParticles particles) {
        HIT_PARTICLES.put(id, particles);
    }

    /** The hit-particle handler of {@code block}, or {@code null} when no mod registered one. */
    public static HitParticles hitParticles(Block block) {
        return HIT_PARTICLES.get(Ids.id(BuiltInRegistries.BLOCK.getKey(block)));
    }
}
