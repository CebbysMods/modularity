package lv.cebbys.mcmods.modularity.fabric.world;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.data.BlockData;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.block.ModularBlockEntity;
import lv.cebbys.mcmods.modularity.api.entity.EntityHandle;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.world.Aabb;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class FabricLevel extends NativeBlockView implements Level {
    private final net.minecraft.world.level.Level level;

    public FabricLevel(net.minecraft.world.level.Level level) {
        super(level);
        this.level = level;
    }

    @Override
    public boolean isClientSide() {
        return level.isClientSide();
    }

    @Override
    public boolean isUnobstructed(BlockStateRef state, Pos pos) {
        return level.isUnobstructed(Wrap.state(state), Wrap.pos(pos), CollisionContext.empty());
    }

    @Override
    public <T> void setData(Pos pos, BlockData<T> data, T value) {
        if (!(level.getBlockEntity(Wrap.pos(pos)) instanceof ModularBlockEntity entity)) {
            throw new IllegalStateException("Block at " + pos + " does not store Modularity block data");
        }
        entity.set(data, value);
    }

    @Override
    public void dropItem(Pos pos, ItemStackRef stack) {
        if (!level.isClientSide() && !stack.isEmpty()) {
            Block.popResource(level, Wrap.pos(pos), Wrap.stack(stack).copy());
        }
    }

    @Override
    public void dropItem(Vec3 at, ItemStackRef stack) {
        if (!level.isClientSide() && !stack.isEmpty()) {
            ItemEntity item = new ItemEntity(level, at.x(), at.y(), at.z(), Wrap.stack(stack).copy());
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
    }

    @Override
    public List<Aabb> blockCollisions(Aabb area) {
        List<Aabb> boxes = new ArrayList<>();
        AABB query = new AABB(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ());
        for (var shape : level.getBlockCollisions(null, query)) {
            for (AABB box : shape.toAabbs()) {
                boxes.add(new Aabb(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
            }
        }
        return boxes;
    }

    @Override
    public EntityRef spawn(EntityHandle type, Vec3 position, float yaw) {
        if (level.isClientSide()) {
            throw new IllegalStateException("Entities are spawned on the server");
        }
        var nativeType = BuiltInRegistries.ENTITY_TYPE.get(Wrap.rl(type.id()));
        var entity = nativeType.create(level);
        if (entity == null) {
            throw new IllegalStateException("Could not create " + type.id());
        }
        entity.moveTo(position.x(), position.y(), position.z(), yaw, 0.0F);
        level.addFreshEntity(entity);
        return Wrap.entity(entity);
    }

    @Override
    public long gameTime() {
        return level.getGameTime();
    }
}
