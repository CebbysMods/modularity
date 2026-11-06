package lv.cebbys.mcmods.modularity.fabric.client;

import lv.cebbys.mcmods.modularity.api.client.LoopingSound;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Positional looping sound following an entity; volume and pitch are polled every tick. */
final class FabricLoopingSound extends AbstractTickableSoundInstance {
    private final LoopingSound sound;

    FabricLoopingSound(LoopingSound sound) {
        super(BuiltInRegistries.SOUND_EVENT.getOptional(Wrap.rl(sound.sound().id())).orElseThrow(), SoundSource.NEUTRAL, RandomSource.create());
        this.sound = sound;
        this.looping = true;
        this.delay = 0;
        update();
    }

    private void update() {
        var position = sound.entity().position();
        this.x = position.x();
        this.y = position.y();
        this.z = position.z();
        this.volume = Math.max(0.0F, sound.volume());
        this.pitch = Math.max(0.01F, sound.pitch());
    }

    @Override
    public void tick() {
        if (sound.isStopped() || sound.entity().isRemoved()) {
            stop();
            return;
        }
        update();
    }
}
