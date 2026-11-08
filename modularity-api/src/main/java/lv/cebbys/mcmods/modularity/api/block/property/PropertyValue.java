package lv.cebbys.mcmods.modularity.api.block.property;

/** Enum values used in {@link EnumProperty} must provide the name used in block state files. */
public interface PropertyValue {
    String serializedName();
}
