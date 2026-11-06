package lv.cebbys.mcmods.modularity.platform.world;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.entity.EntityData;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Portable view of any native entity; Modularity features are available when it is a {@link ModularEntity}. */
public class NativeEntityRef implements EntityRef {
    protected final Entity entity;

    public NativeEntityRef(Entity entity) {
        this.entity = entity;
    }

    @Override
    public UUID uuid() {
        return entity.getUUID();
    }

    @Override
    public Id typeId() {
        return Ids.id(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    @Override
    public Level level() {
        return Wrap.level(entity.level());
    }

    @Override
    public Vec3 position() {
        return Wrap.vec(entity.position());
    }

    @Override
    public void setPosition(Vec3 position) {
        entity.setPos(position.x(), position.y(), position.z());
    }

    @Override
    public float yaw() {
        return entity.getYRot();
    }

    @Override
    public void setYaw(float yaw) {
        if (entity instanceof ModularEntity modular) {
            modular.setOrientation(lv.cebbys.mcmods.modularity.api.world.Rotation.ofYaw(yaw));
        } else {
            entity.setYRot(yaw);
        }
    }

    @Override
    public lv.cebbys.mcmods.modularity.api.world.Rotation orientation() {
        return entity instanceof ModularEntity modular ? modular.orientation()
                : lv.cebbys.mcmods.modularity.api.world.Rotation.ofYaw(entity.getYRot());
    }

    @Override
    public void setOrientation(lv.cebbys.mcmods.modularity.api.world.Rotation orientation) {
        if (entity instanceof ModularEntity modular) {
            modular.setOrientation(orientation);
        } else {
            entity.setYRot(orientation.yaw());
        }
    }

    @Override
    public <T> T data(EntityData<T> data) {
        return entity instanceof ModularEntity modular ? modular.getData(data) : data.defaultValue();
    }

    @Override
    public <T> void setData(EntityData<T> data, T value) {
        if (!(entity instanceof ModularEntity modular)) {
            throw new IllegalStateException(entity + " is not a Modularity entity");
        }
        modular.setData(data, value);
    }

    @Override
    public List<EntityRef> passengers() {
        return entity.getPassengers().stream().map(Wrap::entity).toList();
    }

    @Override
    public Optional<EntityRef> vehicle() {
        return Optional.ofNullable(entity.getVehicle()).map(Wrap::entity);
    }

    @Override
    public boolean addPassenger(EntityRef passenger) {
        Entity rider = passenger.unwrap(Entity.class);
        if (entity.level().isClientSide()) {
            // The server decides; it syncs the passenger to clients.
            return entity instanceof ModularEntity modular && modular.hasFreeSeat();
        }
        return rider.startRiding(entity);
    }

    @Override
    public boolean isRemoved() {
        return entity.isRemoved();
    }

    @Override
    public void remove() {
        entity.discard();
    }

    @Override
    public Optional<FluidTank> fluidTank() {
        return entity instanceof ModularEntity modular ? modular.fluidTank() : Optional.empty();
    }

    @Override
    public Optional<ItemInventory> inventory() {
        return entity instanceof ModularEntity modular ? modular.inventory() : Optional.empty();
    }

    @Override
    public Object handle() {
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NativeEntityRef other && other.entity == entity;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(entity);
    }

    @Override
    public String toString() {
        return entity.toString();
    }
}
