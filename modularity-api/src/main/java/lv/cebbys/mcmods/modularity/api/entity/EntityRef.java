package lv.cebbys.mcmods.modularity.api.entity;

import lv.cebbys.mcmods.modularity.api.core.Handle;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Any entity in the world (players included). */
public interface EntityRef extends Handle {
    UUID uuid();

    Id typeId();

    Level level();

    Vec3 position();

    void setPosition(Vec3 position);

    /** Yaw in degrees, Minecraft convention (0 = facing south / +Z). */
    float yaw();

    void setYaw(float yaw);

    /** Data declared in the entity definition; its default when never set. */
    <T> T data(EntityData<T> data);

    <T> void setData(EntityData<T> data, T value);

    List<EntityRef> passengers();

    Optional<EntityRef> vehicle();

    /** Makes {@code passenger} ride this entity, respecting the declared seat count. */
    boolean addPassenger(EntityRef passenger);

    boolean isRemoved();

    void remove();

    /** Fluid tank declared with {@code EntityDefinition.Builder#fluidTank}. */
    Optional<FluidTank> fluidTank();

    /** Inventory declared with {@code EntityDefinition.Builder#inventory}. */
    Optional<ItemInventory> inventory();

    default boolean is(EntityHandle type) {
        return typeId().equals(type.id());
    }
}
