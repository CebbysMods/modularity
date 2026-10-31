package lv.cebbys.mcmods.modularity.api.block.property;

/**
 * Portable handles for properties that vanilla blocks use. Reading them from a block that does not
 * have the property throws, use {@code BlockStateRef#has} first.
 */
public final class VanillaProperties {
    public static final EnumProperty<SlabType> SLAB_TYPE = new EnumProperty<>("type", SlabType.class, "slab_type");
    public static final BooleanProperty WATERLOGGED = new BooleanProperty("waterlogged", "waterlogged");
    public static final BooleanProperty LIT = new BooleanProperty("lit", "lit");
    public static final BooleanProperty POWERED = new BooleanProperty("powered", "powered");
    public static final IntegerProperty POWER = new IntegerProperty("power", 0, 15, "power");

    private VanillaProperties() {
    }
}
