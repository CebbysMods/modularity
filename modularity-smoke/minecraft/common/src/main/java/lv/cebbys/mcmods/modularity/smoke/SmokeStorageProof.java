package lv.cebbys.mcmods.modularity.smoke;

import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.entity.EntityContext;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.platform.entity.ModularEntity;
import lv.cebbys.mcmods.modularity.platform.loader.Loaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The right-click half of the storage proof (docs/07 7.6): a player uses a water bucket on the smoke entity, the
 * entity behaviour hands the held item to the loader's {@code fillFromHeldItem}, and the tank of the entity fills
 * while the bucket empties.
 *
 * <p>The behaviour receives Modularity's portable {@link Player}; the storage seam needs the native player, which the
 * scenario captures on the server thread when it gives the bucket. The class names no client type, so the dedicated
 * server loads it safely.</p>
 */
final class SmokeStorageProof {
    private static final Logger LOGGER = LoggerFactory.getLogger("modularity-smoke");

    private static net.minecraft.world.entity.player.Player clickedBy;
    private static ModularEntity clickedEntity;

    private SmokeStorageProof() {
    }

    /** Captures the native player and entity of the right-click that is about to happen. */
    static void arm(net.minecraft.world.entity.player.Player player, ModularEntity entity) {
        clickedBy = player;
        clickedEntity = entity;
    }

    /** Handles the entity right-click: fills the entity tank from the item in the player's main hand. */
    static InteractionResult interact(EntityContext context, Player player) {
        var nativePlayer = clickedBy;
        var entity = clickedEntity;
        if (nativePlayer == null || entity == null) {
            LOGGER.error("Smoke storage: right-click without an armed player or entity");
            return InteractionResult.PASS;
        }
        // The client runs the interaction too, as a prediction. Only the logical server owns the tank; running both
        // in one JVM makes Fabric resize the item storage list twice (run of 2026-11-12), so the client passes.
        if (context.isClientSide()) {
            return InteractionResult.PASS;
        }
        try {
            LOGGER.info("Smoke storage: right-clicked {}", context.entity().typeId());
            long unitsPerMb = Loaders.storage().unitsPerMb();
            if (nativePlayer.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)) {
                // An empty bucket in the hand: the loader moves the fluid out of the entity into the bucket.
                long took = Loaders.storage().drainToHeldItem(nativePlayer, entity.nativeTank());
                LOGGER.info("Smoke storage: bucket took {} mB", took);
                LOGGER.info("Smoke storage: entity tank holds {} mB", entity.nativeTank().units() / unitsPerMb);
                return took > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            long moved = Loaders.storage().fillFromHeldItem(nativePlayer, entity.nativeTank());
            LOGGER.info("Smoke storage: held item moved {} mB", moved);
            LOGGER.info("Smoke storage: entity tank holds {} mB", entity.nativeTank().units() / unitsPerMb);
            return moved > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } catch (RuntimeException e) {
            LOGGER.error("Smoke storage: right-click fill failed", e);
            return InteractionResult.PASS;
        }
    }
}
