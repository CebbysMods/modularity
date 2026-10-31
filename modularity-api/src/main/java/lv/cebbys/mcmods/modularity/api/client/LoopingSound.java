package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.sound.SoundHandle;

/**
 * A looping sound attached to an entity. The platform asks for volume and pitch every tick and
 * stops the sound when {@link #isStopped()} or the entity is removed.
 */
public interface LoopingSound {
    SoundHandle sound();

    EntityRef entity();

    boolean isStopped();

    float volume();

    float pitch();
}
