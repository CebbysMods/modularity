package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.Platform;
import lv.cebbys.mcmods.modularity.api.block.BlockHandle;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.network.Channel;

import java.util.function.Consumer;

/** Everything a mod can do during client initialization. */
public interface ClientContext {
    String modId();

    Platform platform();

    default Id id(String path) {
        return Id.of(modId(), path);
    }

    /**
     * Renders {@code block} with a model computed per position. No block state or model JSON is
     * needed for that block. Works with Sodium/Iris through the platform renderer API.
     */
    void blockModel(BlockHandle block, DynamicModel model);

    /** Chooses which block state's texture the "block is being hit" particles use. */
    void hitParticles(BlockHandle block, HitParticles particles);

    /** Renders entities of {@code type}. */
    void entityRenderer(EntityHandle type, EntityRenderer renderer);

    /**
     * An OBJ model from the resource packs, e.g. {@code id("models/entity/car.obj")} for
     * {@code assets/<modid>/models/entity/car.obj}. Loaded lazily and reloaded with resources.
     */
    ModelRef objModel(Id resource);

    /** A key binding shown in the controls menu. {@code defaultKey} is a {@link Keys} code. */
    KeyBinding keyBinding(String name, int defaultKey);

    /** Called at the end of every client tick. */
    void onTick(Consumer<ClientState> handler);

    /** Receives server → client messages of {@code channel}. */
    <T> void onMessage(Channel<T> channel, Consumer<T> handler);

    /** Overrides the camera, e.g. while riding a vehicle. */
    void camera(CameraController controller);

    /** Draws on the in-game HUD. */
    void hud(HudLayer layer);

    /** Plays sounds that follow entities and change volume/pitch every tick. */
    void play(LoopingSound sound);

    /** Current client state (local player, input). */
    ClientState state();
}
