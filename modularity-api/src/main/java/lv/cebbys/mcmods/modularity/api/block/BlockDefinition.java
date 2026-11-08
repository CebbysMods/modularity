package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.block.property.Property;
import lv.cebbys.mcmods.modularity.api.data.BlockData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Everything a platform needs to create a block. Built through {@link Builder}:
 *
 * <pre>{@code
 * mod.block("double_slab", block -> block
 *         .property(LIGHT, 0)
 *         .strength(2.0F, 6.0F)
 *         .lightLevel(state -> state.get(LIGHT))
 *         .pushReaction(PushReaction.BLOCK)
 *         .data(DOUBLE_SLAB)
 *         .behaviour(new DoubleSlabBehaviour()));
 * }</pre>
 */
public final class BlockDefinition {
    private final Map<Property<?>, Object> properties;
    private final float hardness;
    private final float resistance;
    private final BlockSound sound;
    private final ToIntFunction<BlockStateRef> lightLevel;
    private final PushReaction pushReaction;
    private final boolean requiresCorrectTool;
    private final boolean occludes;
    private final boolean lootTable;
    private final boolean item;
    private final Set<BlockData<?>> data;
    private final BlockBehaviour behaviour;

    private BlockDefinition(Builder builder) {
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
        this.hardness = builder.hardness;
        this.resistance = builder.resistance;
        this.sound = builder.sound;
        this.lightLevel = builder.lightLevel;
        this.pushReaction = builder.pushReaction;
        this.requiresCorrectTool = builder.requiresCorrectTool;
        this.occludes = builder.occludes;
        this.lootTable = builder.lootTable;
        this.item = builder.item;
        this.data = Collections.unmodifiableSet(new LinkedHashSet<>(builder.data));
        this.behaviour = builder.behaviour;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Properties in declaration order mapped to their default value. */
    public Map<Property<?>, Object> properties() {
        return properties;
    }

    public float hardness() {
        return hardness;
    }

    public float resistance() {
        return resistance;
    }

    public BlockSound sound() {
        return sound;
    }

    /** {@code null} when the block does not emit light. */
    public ToIntFunction<BlockStateRef> lightLevel() {
        return lightLevel;
    }

    public PushReaction pushReaction() {
        return pushReaction;
    }

    public boolean requiresCorrectTool() {
        return requiresCorrectTool;
    }

    public boolean occludes() {
        return occludes;
    }

    public boolean hasLootTable() {
        return lootTable;
    }

    public boolean hasItem() {
        return item;
    }

    public Set<BlockData<?>> data() {
        return data;
    }

    public BlockBehaviour behaviour() {
        return behaviour;
    }

    public static final class Builder {
        private final Map<Property<?>, Object> properties = new LinkedHashMap<>();
        private float hardness = 1.5F;
        private float resistance = 6.0F;
        private BlockSound sound = BlockSound.STONE;
        private ToIntFunction<BlockStateRef> lightLevel;
        private PushReaction pushReaction = PushReaction.NORMAL;
        private boolean requiresCorrectTool;
        private boolean occludes = true;
        private boolean lootTable = true;
        private boolean item;
        private final List<BlockData<?>> data = new ArrayList<>();
        private BlockBehaviour behaviour = BlockBehaviour.VANILLA;

        private Builder() {
        }

        /** Adds a block state property with its default value. */
        public <T> Builder property(Property<T> property, T defaultValue) {
            Objects.requireNonNull(property, "property");
            if (!property.values().contains(defaultValue)) {
                throw new IllegalArgumentException(defaultValue + " is not a value of " + property);
            }
            properties.put(property, defaultValue);
            return this;
        }

        /** Adds a block state property, defaulting to its first value. */
        public Builder property(Property<?> property) {
            properties.put(property, property.defaultValue());
            return this;
        }

        public Builder strength(float hardness, float resistance) {
            this.hardness = hardness;
            this.resistance = resistance;
            return this;
        }

        public Builder strength(float strength) {
            return strength(strength, strength);
        }

        public Builder sound(BlockSound sound) {
            this.sound = Objects.requireNonNull(sound, "sound");
            return this;
        }

        public Builder lightLevel(ToIntFunction<BlockStateRef> lightLevel) {
            this.lightLevel = Objects.requireNonNull(lightLevel, "lightLevel");
            return this;
        }

        public Builder lightLevel(int lightLevel) {
            return lightLevel(state -> lightLevel);
        }

        public Builder pushReaction(PushReaction pushReaction) {
            this.pushReaction = Objects.requireNonNull(pushReaction, "pushReaction");
            return this;
        }

        public Builder requiresCorrectTool() {
            this.requiresCorrectTool = true;
            return this;
        }

        public Builder noOcclusion() {
            this.occludes = false;
            return this;
        }

        /** The block has no loot table; drops come only from {@link BlockBehaviour#drops}. */
        public Builder noLootTable() {
            this.lootTable = false;
            return this;
        }

        /** Also registers a block item with the same id. */
        public Builder item() {
            this.item = true;
            return this;
        }

        /** Data this block stores per position. */
        public Builder data(BlockData<?>... data) {
            this.data.addAll(List.of(data));
            return this;
        }

        public Builder behaviour(BlockBehaviour behaviour) {
            this.behaviour = Objects.requireNonNull(behaviour, "behaviour");
            return this;
        }

        public BlockDefinition build() {
            return new BlockDefinition(this);
        }
    }
}
