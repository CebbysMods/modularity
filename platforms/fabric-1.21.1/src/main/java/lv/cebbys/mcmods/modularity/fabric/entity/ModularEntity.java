package lv.cebbys.mcmods.modularity.fabric.entity;

import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.entity.EntityContext;
import lv.cebbys.mcmods.modularity.api.entity.EntityData;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.transfer.ItemInventory;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.block.NbtData;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Native entity behind every Modularity entity type. Typed data lives in a map; synced data is
 * mirrored to clients through one compound tracked data entry, saved data is written under
 * {@code "modularity"}. Movement is server authoritative and interpolated on clients.
 */
public class ModularEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> SYNCED = SynchedEntityData.defineId(ModularEntity.class, EntityDataSerializers.COMPOUND_TAG);
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

    public ModularEntity(EntityType<? extends ModularEntity> type, Level level) {
        super(type, level);
        this.definition = EntityTypes.definition(type);
        this.tank = definition.tankCapacity() > 0
                ? new FabricFluidTank(definition.tankCapacity(), definition.tankFluids(), this::syncData) : null;
        this.inventory = definition.inventorySlots() > 0 ? new FabricItemInventory(definition.inventorySlots()) : null;
        EntityRef self = Wrap.entity(this);
        this.context = () -> self;
    }

    public EntityDefinition definition() {
        return definition;
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
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
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
        super.tick();
        if (level().isClientSide() && lerpSteps > 0) {
            double x = getX() + (lerpX - getX()) / lerpSteps;
            double y = getY() + (lerpY - getY()) / lerpSteps;
            double z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) / lerpSteps);
            lerpSteps--;
            setPos(x, y, z);
        }
        definition.behaviour().tick(context);
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
        Vec3 offset = definition.seats().get(index).rotateYaw(getYRot());
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
        InteractionResult result = definition.behaviour().interact(context, Wrap.player(player));
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
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return definition.solid();
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
