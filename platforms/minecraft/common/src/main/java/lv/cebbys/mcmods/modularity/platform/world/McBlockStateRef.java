package lv.cebbys.mcmods.modularity.platform.world;

import lv.cebbys.mcmods.modularity.api.block.BlockRef;
import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.block.BlockTag;
import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.world.BlockView;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.platform.Props;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record McBlockStateRef(BlockState state) implements BlockStateRef {
    private static final Map<BlockTag, TagKey<Block>> TAGS = new ConcurrentHashMap<>();

    @Override
    public BlockRef block() {
        return Wrap.block(state.getBlock());
    }

    @Override
    public boolean has(Property<?> property) {
        return state.hasProperty(Props.toNative(property));
    }

    @Override
    public <T> T get(Property<T> property) {
        return Props.toPortableValue(property, state.getValue(Props.toNative(property)));
    }

    @Override
    public <T> BlockStateRef with(Property<T> property, T value) {
        return Wrap.state(Props.with(state, property, value));
    }

    @Override
    public float destroySpeed(BlockView view, Pos pos) {
        return state.getDestroySpeed(view.unwrap(BlockGetter.class), Wrap.pos(pos));
    }

    @Override
    public int lightEmission() {
        return state.getLightEmission();
    }

    @Override
    public boolean is(BlockTag tag) {
        return state.is(TAGS.computeIfAbsent(tag, t -> TagKey.create(Registries.BLOCK, Ids.rl(t.id()))));
    }

    @Override
    public boolean is(BlockRef block) {
        return state.is(Wrap.block(block));
    }

    @Override
    public Object handle() {
        return state;
    }
}
