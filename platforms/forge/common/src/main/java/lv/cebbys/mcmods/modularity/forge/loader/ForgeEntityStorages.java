package lv.cebbys.mcmods.modularity.forge.loader;

import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/**
 * The tank and the inventory of one Modularity entity, as Forge capability providers. Forge attaches capabilities to
 * an entity instance, not to an entity type, so each entity of an exposed type gets its own provider.
 */
final class ForgeEntityStorages implements ICapabilityProvider {
    private final LazyOptional<IFluidHandler> fluids;
    private final LazyOptional<IItemHandler> items;

    ForgeEntityStorages(ModularEntity entity, EntityDefinition definition) {
        this.fluids = definition.tankCapacity() > 0
                ? LazyOptional.of(() -> new ForgeTankHandler(entity.nativeTank()))
                : LazyOptional.empty();
        this.items = definition.inventorySlots() > 0
                ? LazyOptional.of(() -> new InvWrapper(entity.nativeInventory().container()))
                : LazyOptional.empty();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluids.cast();
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return items.cast();
        }
        return LazyOptional.empty();
    }
}
