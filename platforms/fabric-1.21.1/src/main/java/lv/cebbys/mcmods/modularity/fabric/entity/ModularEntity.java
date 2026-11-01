package lv.cebbys.mcmods.modularity.fabric.entity;

import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.entity.EntityContext;
import lv.cebbys.mcmods.modularity.api.entity.EntityData;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.api.entity.EntityPart;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.world.Rotation;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.block.NbtData;
import lv.cebbys.mcmods.modularity.fabric.mixin.EntityAccessor;
import lv.cebbys.mcmods.modularity.fabric.transfer.FabricFluidTank;
import lv.cebbys.mcmods.modularity.fabric.transfer.FabricItemInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Native entity behind every Modularity entity type. Typed data lives in a map; synced data is
 * mirrored to clients through one compound tracked data entry, saved data is written under
 * {@code "modularity"}. Movement is server authoritative and interpolated on clients.
 */
public class ModularEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> SYNCED = SynchedEntityData.defineId(ModularEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Quaternionf> ORIENTATION =
            SynchedEntityData.defineId(ModularEntity.class, EntityDataSerializers.QUATERNION);
    private static final String TANK_SYNC_KEY = "modularity:tank";

    private final EntityDefinition definition;
    private final Map<EntityData<?>, Object> values = new LinkedHashMap<>();
    private final FabricFluidTank tank;
    private final FabricItemInventory inventory;
    private final EntityContext context;
    private float lastYaw;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYaw;
    /** Full orientation; {@code null} while only the yaw is known. */
    private Rotation orientation;
    private Rotation orientationO = Rotation.IDENTITY;
    private Rotation lerpOrientation;
    private int lerpOrientationSteps;
    private final ModularPart[] parts;

    public ModularEntity(EntityType<? extends ModularEntity> type, Level level) {
        super(type, level);
        this.definition = EntityTypes.definition(type);
        List<EntityPart> partList = definition.parts();
        this.parts = new ModularPart[partList.size()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new ModularPart(this, partList.get(i), i);
        }
        if (parts.length > 0) {
            // Reserve consecutive ids for the parts, like the Ender Dragon: clients build the same
            // parts and both sides resolve part ids from the parent id.
            setId(EntityAccessor.modularity$counter().getAndAdd(parts.length + 1) + 1);
        }
        this.tank = definition.tankCapacity() > 0
                ? new FabricFluidTank(definition.tankCapacity(), definition.tankFluids(), this::syncData) : null;
        this.inventory = definition.inventorySlots() > 0 ? new FabricItemInventory(definition.inventorySlots()) : null;
        EntityRef self = Wrap.entity(this);
        this.context = () -> self;
    }

    public EntityDefinition definition() {
        return definition;
    }

    // ------------------------------------------------------------------ parts & orientation

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                parts[i].setId(id + 1 + i);
            }
        }
    }

    public List<ModularPart> parts() {
        return List.of(parts);
    }

    /** The part with entity id {@code id}, or {@code null}. */
    public ModularPart partById(int id) {
        int index = id - getId() - 1;
        return index >= 0 && index < parts.length ? parts[index] : null;
    }

    public boolean isMultipart() {
        return parts.length > 0;
    }

    public Rotation orientation() {
        return orientation != null ? orientation : Rotation.ofYaw(getYRot());
    }

    /** Orientation interpolated between the last two ticks, for rendering. */
    public Rotation orientation(float partialTick) {
        return orientationO.slerp(orientation(), partialTick);
    }

    /** Sets the full orientation (server: synced to clients). */
    public void setOrientation(Rotation rotation) {
        Rotation r = rotation.normalize();
        orientation = r;
        super.setYRot(r.yaw());
        if (!level().isClientSide()) {
            entityData.set(ORIENTATION, new Quaternionf((float) r.x(), (float) r.y(), (float) r.z(), (float) r.w()));
        }
        placeParts();
    }

    private void placeParts() {
        if (parts == null || parts.length == 0) {
            return;
        }
        Rotation r = orientation();
        net.minecraft.world.phys.Vec3 origin = position();
        for (ModularPart part : parts) {
            part.place(origin, r);
        }
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        if (parts == null || parts.length == 0) {
            return super.makeBoundingBox();
        }
        AABB box = null;
        for (ModularPart part : parts) {
            box = box == null ? part.getBoundingBox() : box.minmax(part.getBoundingBox());
        }
        return box;
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        placeParts();
    }

    boolean hurtPart(ModularPart part, DamageSource source, float amount) {
        if (level().isClientSide()) {
            return true;
        }
        Optional<EntityRef> attacker = Optional.ofNullable(source.getEntity()).map(Wrap::entity);
        return definition.behaviour().attackedPart(context, attacker, amount, part.part());
    }

    net.minecraft.world.InteractionResult interactPart(ModularPart part, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return toNative(definition.behaviour().interactPart(context, Wrap.player(player), part.part()));
    }

    /**
     * Moves entities that stood on solid parts along with this entity (translation and rotation),
     * and pushes entities out of solid parts that moved into them. Server: every entity but players;
     * client: the local player (players move themselves).
     */
    private void carry(net.minecraft.world.phys.Vec3 oldPos, Rotation oldRot, List<Entity> standing) {
        net.minecraft.world.phys.Vec3 newPos = position();
        Rotation newRot = orientation();
        if (!newPos.equals(oldPos) || !newRot.equals(oldRot)) {
            Rotation delta = newRot.mul(oldRot.inverse());
            float yawDelta = Mth.wrapDegrees(newRot.yaw() - oldRot.yaw());
            for (Entity e : standing) {
                net.minecraft.world.phys.Vec3 rel = e.position().subtract(oldPos);
                var moved = delta.rotate(new Vec3(rel.x, rel.y, rel.z));
                e.setPos(newPos.x + moved.x(), newPos.y + moved.y(), newPos.z + moved.z());
                if (yawDelta != 0) {
                    e.setYRot(e.getYRot() + yawDelta);
                    e.setYHeadRot(e.getYHeadRot() + yawDelta);
                }
                e.setOnGround(true);
            }
        }
        for (Entity e : nearbyMovable()) {
            pushOut(e);
        }
    }

    /** Entities this side may move: not riding, not parts, players only on their own client. */
    private List<Entity> nearbyMovable() {
        List<Entity> out = new ArrayList<>();
        AABB area = getBoundingBox().inflate(0.5).expandTowards(0, 1.5, 0);
        for (Entity e : level().getEntities(this, area, e -> !(e instanceof ModularPart) && !e.isPassenger()
                && !e.isSpectator() && !e.noPhysics)) {
            boolean mine = level().isClientSide()
                    ? e instanceof Player p && p.isLocalPlayer()
                    : !(e instanceof Player);
            if (mine) {
                out.add(e);
            }
        }
        return out;
    }

    /** Entities whose feet rest on a solid part (checked before the parts move). */
    private List<Entity> standingOnParts() {
        List<Entity> out = new ArrayList<>();
        for (Entity e : nearbyMovable()) {
            AABB box = e.getBoundingBox();
            AABB feet = box.move(0, -0.12, 0);
            boolean on = false;
            for (ModularPart part : parts) {
                for (var column : part.collisionShapes()) {
                    AABB c = column.bounds();
                    if (c.intersects(feet) && box.minY >= c.maxY - 0.3) {
                        on = true;
                        break;
                    }
                }
                if (on) {
                    break;
                }
            }
            if (on) {
                out.add(e);
            }
        }
        return out;
    }

    private void pushOut(Entity e) {
        for (ModularPart part : parts) {
            for (var column : part.collisionShapes()) {
                AABB c = column.bounds();
                AABB b = e.getBoundingBox();
                if (!c.intersects(b)) {
                    continue;
                }
                double up = c.maxY - b.minY;
                double px = Math.min(c.maxX - b.minX, b.maxX - c.minX);
                double pz = Math.min(c.maxZ - b.minZ, b.maxZ - c.minZ);
                if (up <= 0.6 && up <= px && up <= pz) {
                    e.setPos(e.getX(), e.getY() + up, e.getZ());
                } else if (px <= pz) {
                    double dir = (b.minX + b.maxX) / 2 < (c.minX + c.maxX) / 2 ? -1 : 1;
                    e.setPos(e.getX() + dir * px, e.getY(), e.getZ());
                } else {
                    double dir = (b.minZ + b.maxZ) / 2 < (c.minZ + c.maxZ) / 2 ? -1 : 1;
                    e.setPos(e.getX(), e.getY(), e.getZ() + dir * pz);
                }
            }
        }
    }

    // ------------------------------------------------------------------ data

    public <T> T getData(EntityData<T> key) {
        @SuppressWarnings("unchecked") T value = (T) values.get(key);
        return value != null ? value : key.defaultValue();
    }

    public <T> void setData(EntityData<T> key, T value) {
        if (!definition.data().contains(key)) {
            throw new IllegalArgumentException(getType() + " does not declare " + key);
        }
        Object previous = values.put(key, value);
        if (key.synced() && !value.equals(previous) && !level().isClientSide()) {
            syncData();
        }
    }

    private void syncData() {
        if (level().isClientSide()) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        values.forEach((key, value) -> {
            if (key.synced()) {
                tag.put(key.id().toString(), write(key, value));
            }
        });
        if (tank != null) {
            tag.put(TANK_SYNC_KEY, tank.save());
        }
        entityData.set(SYNCED, tag);
    }

    private static <T> CompoundTag write(EntityData<T> key, Object value) {
        CompoundTag out = new CompoundTag();
        @SuppressWarnings("unchecked") T typed = (T) value;
        key.codec().write(typed, new NbtData(out));
        return out;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SYNCED, new CompoundTag());
        builder.define(ORIENTATION, new Quaternionf());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (ORIENTATION.equals(accessor) && level().isClientSide()) {
            Quaternionf q = entityData.get(ORIENTATION);
            Rotation target = new Rotation(q.x, q.y, q.z, q.w).normalize();
            if (orientation == null) {
                orientation = target;
                orientationO = target;
                placeParts();
            } else {
                lerpOrientation = target;
                lerpOrientationSteps = 3;
            }
        }
        if (SYNCED.equals(accessor) && level().isClientSide()) {
            CompoundTag tag = entityData.get(SYNCED);
            for (EntityData<?> key : definition.data()) {
                if (key.synced() && tag.contains(key.id().toString())) {
                    values.put(key, key.codec().read(new NbtData(tag.getCompound(key.id().toString()))));
                }
            }
            if (tank != null && tag.contains(TANK_SYNC_KEY)) {
                tank.load(tag.getCompound(TANK_SYNC_KEY));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        CompoundTag stored = tag.getCompound("modularity");
        for (EntityData<?> key : definition.data()) {
            if (key.saved() && stored.contains(key.id().toString())) {
                values.put(key, key.codec().read(new NbtData(stored.getCompound(key.id().toString()))));
            }
        }
        if (tank != null) {
            tank.load(stored.getCompound("tank"));
        }
        if (inventory != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(stored.getCompound("inventory"), items, registryAccess());
            for (int i = 0; i < items.size(); i++) {
                inventory.container().setItem(i, items.get(i));
            }
        }
        syncData();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        CompoundTag stored = new CompoundTag();
        values.forEach((key, value) -> {
            if (key.saved()) {
                stored.put(key.id().toString(), write(key, value));
            }
        });
        if (tank != null) {
            stored.put("tank", tank.save());
        }
        if (inventory != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);
            for (int i = 0; i < items.size(); i++) {
                items.set(i, inventory.container().getItem(i));
            }
            stored.put("inventory", ContainerHelper.saveAllItems(new CompoundTag(), items, registryAccess()));
        }
        tag.put("modularity", stored);
    }

    public Optional<FluidTank> fluidTank() {
        return Optional.ofNullable(tank);
    }

    public Optional<ItemInventory> inventory() {
        return Optional.ofNullable(inventory);
    }

    public FabricFluidTank nativeTank() {
        return tank;
    }

    public FabricItemInventory nativeInventory() {
        return inventory;
    }

    // ------------------------------------------------------------------ ticking & movement

    @Override
    public void tick() {
        lastYaw = getYRot();
        net.minecraft.world.phys.Vec3 oldPos = position();
        Rotation oldRot = orientation();
        orientationO = oldRot;
        List<Entity> standing = parts.length > 0 ? standingOnParts() : List.of();
        super.tick();
        if (level().isClientSide()) {
            if (lerpOrientationSteps > 0) {
                orientation = orientation().slerp(lerpOrientation, 1.0 / lerpOrientationSteps);
                super.setYRot(orientation.yaw());
                lerpOrientationSteps--;
            }
            if (lerpSteps > 0) {
                double x = getX() + (lerpX - getX()) / lerpSteps;
                double y = getY() + (lerpY - getY()) / lerpSteps;
                double z = getZ() + (lerpZ - getZ()) / lerpSteps;
                if (orientation == null) {
                    setYRot(getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) / lerpSteps);
                }
                lerpSteps--;
                setPos(x, y, z);
            }
        }
        definition.behaviour().tick(context);
        if (parts.length > 0) {
            placeParts();
            carry(oldPos, oldRot, standing);
        }
    }

    @Override
    public void setYRot(float yaw) {
        super.setYRot(yaw);
        if (orientation != null && Math.abs(Mth.wrapDegrees(orientation.yaw() - yaw)) > 1e-3) {
            // Turned by someone else (e.g. teleport, yaw-only API): keep pitch and roll.
            orientation = Rotation.ofYaw(yaw - orientation.yaw()).mul(orientation).normalize();
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yRot;
        lerpSteps = Math.max(steps, 3);
    }

    // ------------------------------------------------------------------ riding

    public boolean hasFreeSeat() {
        return getPassengers().size() < definition.seats().size();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return hasFreeSeat();
    }

    @Override
    public net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        int index = getPassengers().indexOf(passenger);
        if (index < 0 || index >= definition.seats().size()) {
            super.positionRider(passenger, move);
            return;
        }
        Vec3 offset = orientation().rotate(definition.seats().get(index));
        move.accept(passenger, getX() + offset.x(), getY() + offset.y(), getZ() + offset.z());
        // Riders turn with the vehicle.
        float delta = Mth.wrapDegrees(getYRot() - lastYaw);
        if (delta != 0) {
            passenger.setYRot(passenger.getYRot() + delta);
            passenger.setYHeadRot(passenger.getYHeadRot() + delta);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public net.minecraft.world.InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return toNative(definition.behaviour().interact(context, Wrap.player(player)));
    }

    private net.minecraft.world.InteractionResult toNative(InteractionResult result) {
        return switch (result) {
            case PASS -> net.minecraft.world.InteractionResult.PASS;
            case SUCCESS -> net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide());
            case CONSUME -> net.minecraft.world.InteractionResult.CONSUME;
            case FAIL -> net.minecraft.world.InteractionResult.FAIL;
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (level().isClientSide()) {
            return true;
        }
        Optional<EntityRef> attacker = Optional.ofNullable(source.getEntity()).map(Wrap::entity);
        return definition.behaviour().attacked(context, attacker, amount);
    }

    @Override
    public boolean isPickable() {
        // A multipart entity is aimed at through its parts.
        return !isRemoved() && !isMultipart();
    }

    @Override
    public boolean canBeCollidedWith() {
        return definition.solid() && !isMultipart();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return ItemStack.EMPTY;
    }
}
