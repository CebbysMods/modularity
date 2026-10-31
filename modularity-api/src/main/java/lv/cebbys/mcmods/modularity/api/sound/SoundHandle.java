package lv.cebbys.mcmods.modularity.api.sound;

import lv.cebbys.mcmods.modularity.api.core.Id;

/**
 * A sound event you registered with {@code mod.sound("engine")}. The audio files are declared in
 * {@code assets/<modid>/sounds.json} like for any vanilla sound.
 */
public interface SoundHandle {
    Id id();
}
