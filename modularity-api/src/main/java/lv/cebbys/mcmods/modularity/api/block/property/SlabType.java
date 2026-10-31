package lv.cebbys.mcmods.modularity.api.block.property;

public enum SlabType implements PropertyValue {
    TOP("top"), BOTTOM("bottom"), DOUBLE("double");

    private final String serializedName;

    SlabType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
