package lv.cebbys.mcmods.modularity.fabric.mixin;

import com.google.common.collect.ImmutableList;
import lv.cebbys.mcmods.modularity.fabric.entity.EntityTypes;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.fabric.entity.ModularPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Multipart entities in entity queries, like the Ender Dragon's parts: {@code getEntities} returns
 * the parts of multipart Modularity entities (instead of their roots), so aiming, attacking,
 * projectiles and entity collision see each part. Solid parts collide with their rotated shape.
 */
@Mixin(Level.class)
public abstract class LevelMixin implements EntityGetter {
    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private void modularity$parts(@Nullable Entity except, AABB area, Predicate<? super Entity> predicate,
                                  CallbackInfoReturnable<List<Entity>> cir) {
        double reach = EntityTypes.maxPartReach();
        if (reach <= 0) {
            return;
        }
        List<Entity> result = null;
        List<Entity> found = cir.getReturnValue();
        // Roots of multipart entities are represented by their parts.
        for (Entity e : found) {
            if (e instanceof ModularEntity m && m.isMultipart()) {
                result = new ArrayList<>(found);
                result.removeIf(x -> x instanceof ModularEntity mm && mm.isMultipart());
                break;
            }
        }
        List<Entity> out = result;
        List<Entity> parts = new ArrayList<>();
        getEntities().get(area.inflate(reach), e -> {
            if (e instanceof ModularEntity m && m.isMultipart() && m != except) {
                for (ModularPart part : m.parts()) {
                    if (part != except && part.getBoundingBox().intersects(area) && predicate.test(part)) {
                        parts.add(part);
                    }
                }
            }
        });
        if (parts.isEmpty() && out == null) {
            return;
        }
        List<Entity> merged = out != null ? out : new ArrayList<>(found);
        merged.addAll(parts);
        cir.setReturnValue(merged);
    }

    /**
     * Same as {@link EntityGetter#getEntityCollisions}, except that solid parts contribute their
     * rotated (column) shape instead of their axis-aligned bounds.
     */
    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB area) {
        if (area.getSize() < 1.0E-7) {
            return List.of();
        }
        Predicate<Entity> predicate = entity == null
                ? EntitySelector.CAN_BE_COLLIDED_WITH
                : EntitySelector.NO_SPECTATORS.and(entity::canCollideWith);
        List<Entity> entities = getEntities(entity, area.inflate(1.0E-7), predicate);
        if (entities.isEmpty()) {
            return List.of();
        }
        ImmutableList.Builder<VoxelShape> shapes = ImmutableList.builderWithExpectedSize(entities.size());
        for (Entity other : entities) {
            if (other instanceof ModularPart part) {
                if (entity != null && part.parent().hasPassenger(entity)) {
                    continue;
                }
                for (VoxelShape column : part.collisionShapes()) {
                    if (column.bounds().intersects(area)) {
                        shapes.add(column);
                    }
                }
            } else {
                shapes.add(Shapes.create(other.getBoundingBox()));
            }
        }
        return shapes.build();
    }
}
