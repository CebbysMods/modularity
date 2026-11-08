package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.DynamicModel;
import lv.cebbys.mcmods.modularity.api.client.HitParticles;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client registrations collected from all mods. */
public final class ClientRegistry {
    static final Map<Block, DynamicModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<Block, HitParticles> HIT_PARTICLES = new ConcurrentHashMap<>();

    private ClientRegistry() {
    }

    static void model(Block block, DynamicModel model) {
        MODELS.put(block, model);
    }

    static void hitParticles(Block block, HitParticles particles) {
        HIT_PARTICLES.put(block, particles);
    }

    public static HitParticles hitParticles(Block block) {
        return HIT_PARTICLES.get(block);
    }
}
