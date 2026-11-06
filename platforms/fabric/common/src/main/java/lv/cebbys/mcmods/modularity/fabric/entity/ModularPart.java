package lv.cebbys.mcmods.modularity.fabric.entity;

import lv.cebbys.mcmods.modularity.api.entity.EntityPart;
import lv.cebbys.mcmods.modularity.api.world.Rotation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * One part of a multipart {@link ModularEntity}, in the style of the Ender Dragon's parts: never
 * added to the level or sent to clients (both sides build the same parts with ids following the
 * parent's), but returned by {@code Level.getEntities} so it can be aimed at, hit, interacted with
 * and collided with. Its box follows the parent's position and full orientation every tick.
 */
public final class ModularPart extends Entity {
    private final ModularEntity parent;
    private final EntityPart part;
    private final int index;
    private PartShape shape;

    ModularPart(ModularEntity parent, EntityPart part, int index) {
        super(parent.getType(), parent.level());
        this.parent = parent;
        this.part = part;
        this.index = index;
        this.noPhysics = true;
        this.shape = PartShape.place(part, parent.position(), parent.orientation());
        setBoundingBox(shape.bounds());
        refreshDimensions();
    }

    public ModularEntity parent() {
        return parent;
    }

    public EntityPart part() {
        return part;
    }

    public int index() {
        return index;
    }

    public PartShape shape() {
        return shape;
    }

    /** Moves the part to follow its parent. */
    void place(Vec3 origin, Rotation orientation) {
        shape = PartShape.place(part, origin, orientation);
        AABB bounds = shape.bounds();
        Vec3 center = bounds.getCenter();
        setPosRaw(center.x, bounds.minY, center.z);
        xo = getX();
        yo = getY();
        zo = getZ();
        setBoundingBox(bounds);
    }

    /** Solid boxes approximating the rotated part, for entity collision. */
    public List<VoxelShape> collisionShapes() {
        return shape.columns();
    }

    @Override
    protected AABB makeBoundingBox() {
        return shape != null ? shape.bounds() : super.makeBoundingBox();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (shape == null) {
            return super.getDimensions(pose);
        }
        AABB b = shape.bounds();
        return EntityDimensions.scalable((float) Math.max(b.getXsize(), b.getZsize()), (float) b.getYsize());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !parent.isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return part.solid() && !parent.isRemoved();
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return !is(other) && super.canCollideWith(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && parent.hurtPart(this, source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return parent.interactPart(this, player, hand);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || parent == entity;
    }

    @Override
    public ItemStack getPickResult() {
        return parent.getPickResult();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        throw new UnsupportedOperationException("Parts are never sent to clients");
    }

    @Override
    public String toString() {
        return "ModularPart[" + part.name() + " of " + parent + "]";
    }
}
