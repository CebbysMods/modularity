package lv.cebbys.mcmods.modularity.fabric.block;

import lv.cebbys.mcmods.modularity.api.data.BlockData;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stores the portable {@link BlockData} of a block: persisted as NBT, synced to clients and exposed
 * as Fabric render data so models can read it while chunks are meshed (vanilla, Sodium, Iris).
 */
public class ModularBlockEntity extends BlockEntity implements RenderDataBlockEntity {
    private static final String TAG = "modularity";

    private final Map<BlockData<?>, Object> values = new LinkedHashMap<>();
    private volatile Map<BlockData<?>, Object> renderData = Map.of();

    public ModularBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public <T> Optional<T> get(BlockData<T> key) {
        @SuppressWarnings("unchecked") T value = (T) values.get(key);
        return Optional.ofNullable(value);
    }

    public <T> void set(BlockData<T> key, T value) {
        if (!declared().contains(key)) {
            throw new IllegalArgumentException(getBlockState().getBlock() + " does not declare " + key);
        }
        values.put(key, value);
        renderData = Map.copyOf(values);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    private java.util.Set<BlockData<?>> declared() {
        return getBlockState().getBlock() instanceof ModularBlock block ? block.definition().data() : java.util.Set.of();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        Map<BlockData<?>, Object> pending = PendingData.take(level, worldPosition, getBlockState().getBlock());
        if (!pending.isEmpty()) {
            values.putAll(pending);
            renderData = Map.copyOf(values);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag stored = tag.getCompound(TAG);
        for (BlockData<?> key : declared()) {
            String id = key.id().toString();
            if (stored.contains(id)) {
                values.put(key, key.codec().read(new NbtData(stored.getCompound(id))));
            }
        }
        renderData = Map.copyOf(values);
        if (level != null && level.isClientSide()) {
            // Data arrived from the server: re-mesh the chunk section so models see it.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag stored = new CompoundTag();
        values.forEach((key, value) -> stored.put(key.id().toString(), write(key, value)));
        tag.put(TAG, stored);
    }

    private static <T> CompoundTag write(BlockData<T> key, Object value) {
        CompoundTag out = new CompoundTag();
        @SuppressWarnings("unchecked") T typed = (T) value;
        key.codec().write(typed, new NbtData(out));
        return out;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Object getRenderData() {
        return renderData;
    }
}
