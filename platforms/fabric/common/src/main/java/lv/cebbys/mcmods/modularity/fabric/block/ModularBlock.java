package lv.cebbys.mcmods.modularity.fabric.block;

import lv.cebbys.mcmods.modularity.api.block.BlockDefinition;
import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.fabric.Props;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.compat.LootCompat;
import lv.cebbys.mcmods.modularity.fabric.compat.RegistryCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.Map;

/** Native block created from a portable {@link BlockDefinition}. */
public class ModularBlock extends Block {
    private static final ThreadLocal<BlockDefinition> CONSTRUCTING = new ThreadLocal<>();

    private final BlockDefinition definition;

    protected ModularBlock(Id id, BlockDefinition definition) {
        super(properties(id, definition));
        this.definition = definition;
        BlockState state = stateDefinition.any();
        for (Map.Entry<Property<?>, Object> entry : definition.properties().entrySet()) {
            state = withValue(state, entry.getKey(), entry.getValue());
        }
        registerDefaultState(state);
    }

    public static ModularBlock create(Id id, BlockDefinition definition) {
        CONSTRUCTING.set(definition);
        try {
            return definition.data().isEmpty() ? new ModularBlock(id, definition) : new ModularEntityBlock(id, definition);
        } finally {
            CONSTRUCTING.remove();
        }
    }

    public BlockDefinition definition() {
        return definition;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState withValue(BlockState state, Property<?> property, Object value) {
        var nativeProperty = (net.minecraft.world.level.block.state.properties.Property) Props.toNative(property);
        return state.setValue(nativeProperty, (Comparable) Props.toNativeValue(property, value));
    }

    private static BlockBehaviour.Properties properties(Id id, BlockDefinition definition) {
        var properties = RegistryCompat.blockProperties(id)
                .strength(definition.hardness(), definition.resistance())
                .sound(sound(definition))
                .pushReaction(PushReaction.valueOf(definition.pushReaction().name()));
        if (definition.lightLevel() != null) {
            var light = definition.lightLevel();
            properties.lightLevel(state -> light.applyAsInt(Wrap.state(state)));
        }
        if (definition.requiresCorrectTool()) {
            properties.requiresCorrectToolForDrops();
        }
        if (!definition.occludes()) {
            properties.noOcclusion();
        }
        if (!definition.hasLootTable()) {
            properties.noLootTable();
        }
        return properties;
    }

    private static SoundType sound(BlockDefinition definition) {
        return switch (definition.sound()) {
            case STONE -> SoundType.STONE;
            case WOOD -> SoundType.WOOD;
            case GRAVEL -> SoundType.GRAVEL;
            case GRASS -> SoundType.GRASS;
            case METAL -> SoundType.METAL;
            case GLASS -> SoundType.GLASS;
            case WOOL -> SoundType.WOOL;
            case SAND -> SoundType.SAND;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        BlockDefinition constructing = CONSTRUCTING.get();
        if (constructing == null) {
            throw new IllegalStateException("ModularBlock must be created through ModularBlock.create");
        }
        for (Property<?> property : constructing.properties().keySet()) {
            builder.add(Props.toNative(property));
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float custom = definition.behaviour().destroyProgress(Contexts.block(level, pos, state), Wrap.player(player));
        return Float.isNaN(custom) ? super.getDestroyProgress(state, player, level, pos) : custom;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        definition.behaviour().onPlayerBreak(Contexts.breaking(level, pos, state, player));
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ModularBlockEntity modular
                ? modular : null;
        var player = params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player p ? p : null;
        var tool = LootCompat.tool(params);
        var pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        var context = Contexts.drops(params.getLevel(), pos, state, entity, player, tool,
                () -> super.getDrops(state, params));
        return definition.behaviour().drops(context).stream().map(Wrap::stack).toList();
    }
}
