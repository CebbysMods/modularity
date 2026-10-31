package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.data.MapData;
import lv.cebbys.mcmods.modularity.api.entity.EntityData;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;
import lv.cebbys.mcmods.modularity.api.transfer.SimpleFluidTank;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory entity. Data values are stored serialized (through their codec), like a platform
 * would, so codec bugs show up in tests.
 */
public class TestEntity implements EntityRef {
    private final UUID uuid;
    private final Id typeId;
    private final EntityDefinition definition;
    private final Map<EntityData<?>, MapData> data = new HashMap<>();
    private final List<EntityRef> passengers = new ArrayList<>();
    private final SimpleFluidTank tank;
    private final TestInventory inventory;
    TestLevel level;
    private Vec3 position = Vec3.ZERO;
    private float yaw;
    private EntityRef vehicle;
    private boolean removed;

    TestEntity(UUID uuid, Id typeId, EntityDefinition definition) {
        this.uuid = uuid;
        this.typeId = typeId;
        this.definition = definition;
        this.tank = definition != null && definition.tankCapacity() > 0
                ? new SimpleFluidTank(definition.tankCapacity(), definition.tankFluids()) : null;
        this.inventory = definition != null && definition.inventorySlots() > 0
                ? new TestInventory(definition.inventorySlots()) : null;
    }

    /** Definition when registered by a mod, {@code null} for players. */
    public EntityDefinition definition() {
        return definition;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public Id typeId() {
        return typeId;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public Vec3 position() {
        return position;
    }

    @Override
    public void setPosition(Vec3 position) {
        this.position = position;
    }

    @Override
    public float yaw() {
        return yaw;
    }

    @Override
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    @Override
    public <T> T data(EntityData<T> key) {
        MapData raw = data.get(key);
        return raw == null ? key.defaultValue() : key.codec().read(raw);
    }

    @Override
    public <T> void setData(EntityData<T> key, T value) {
        if (definition == null || !definition.data().contains(key)) {
            throw new IllegalArgumentException(typeId + " does not declare " + key);
        }
        MapData raw = new MapData();
        key.codec().write(value, raw);
        data.put(key, raw);
    }

    @Override
    public List<EntityRef> passengers() {
        return List.copyOf(passengers);
    }

    @Override
    public Optional<EntityRef> vehicle() {
        return Optional.ofNullable(vehicle);
    }

    @Override
    public boolean addPassenger(EntityRef passenger) {
        int seats = definition == null ? 0 : definition.seats().size();
        if (passengers.size() >= seats || passengers.contains(passenger) || passenger == this) {
            return false;
        }
        TestEntity other = (TestEntity) passenger;
        other.dismount();
        passengers.add(passenger);
        other.vehicle = this;
        return true;
    }

    /** Leaves the current vehicle (vanilla: sneaking). */
    public void dismount() {
        if (vehicle instanceof TestEntity current) {
            current.passengers.remove(this);
        }
        vehicle = null;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    @Override
    public void remove() {
        removed = true;
        new ArrayList<>(passengers).forEach(p -> ((TestEntity) p).dismount());
        if (level != null) {
            level.removeEntity(this);
        }
    }

    @Override
    public Optional<FluidTank> fluidTank() {
        return Optional.ofNullable(tank);
    }

    @Override
    public Optional<ItemInventory> inventory() {
        return Optional.ofNullable(inventory);
    }

    @Override
    public Object handle() {
        return this;
    }

    /** What would be written to the world save. */
    MapData save() {
        MapData out = new MapData();
        out.putDouble("x", position.x()).putDouble("y", position.y()).putDouble("z", position.z());
        out.putDouble("yaw", yaw);
        data.forEach((key, raw) -> {
            if (key.saved()) {
                out.values().put("data:" + key.id(), raw);
            }
        });
        if (tank != null) {
            MapData tankData = new MapData();
            tank.write(tankData);
            out.values().put("tank", tankData);
        }
        if (inventory != null) {
            List<ItemStackRef0> items = new ArrayList<>();
            for (int i = 0; i < inventory.size(); i++) {
                items.add(new ItemStackRef0(inventory.get(i)));
            }
            out.values().put("inventory", items);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    void load(MapData in) {
        position = new Vec3(in.getDouble("x"), in.getDouble("y"), in.getDouble("z"));
        yaw = (float) in.getDouble("yaw");
        if (definition != null) {
            for (EntityData<?> key : definition.data()) {
                Object raw = in.values().get("data:" + key.id());
                if (raw instanceof MapData mapData) {
                    data.put(key, mapData);
                }
            }
        }
        if (tank != null && in.values().get("tank") instanceof MapData tankData) {
            tank.read(tankData);
        }
        if (inventory != null && in.values().get("inventory") instanceof List<?> items) {
            for (int i = 0; i < items.size(); i++) {
                inventory.set(i, ((ItemStackRef0) items.get(i)).stack());
            }
        }
    }

    private record ItemStackRef0(lv.cebbys.mcmods.modularity.api.item.ItemStackRef stack) {
    }

    @Override
    public String toString() {
        return typeId + "@" + position;
    }
}
