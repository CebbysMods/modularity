package lv.cebbys.mcmods.modularity.neoforge.loader;

import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.StorageBackend;
import lv.cebbys.mcmods.modularity.platform.transfer.McFluidTank;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/** The NeoForge storage seam: NeoForge fluid units are millibuckets; the capability bridges are not wired yet. */
public final class NeoForgeStorageBackend implements StorageBackend {

    @Override
    public long unitsPerMb() {
        return 1;
    }

    @Override
    public void exposeEntityStorages(EntityType<ModularEntity> type, EntityDefinition definition) {
        throw notImplemented("exposeEntityStorages");
    }

    @Override
    public long fillFromHeldItem(Player player, McFluidTank tank) {
        throw notImplemented("fillFromHeldItem");
    }

    private static UnsupportedOperationException notImplemented(String method) {
        return new UnsupportedOperationException(
                "Modularity storage integration is not implemented on NeoForge yet (" + method + ")");
    }
}
