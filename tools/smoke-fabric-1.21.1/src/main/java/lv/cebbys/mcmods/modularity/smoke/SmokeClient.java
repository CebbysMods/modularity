package lv.cebbys.mcmods.modularity.smoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * In the client: frames the slabs, waits for chunks to mesh and takes screenshots. With KleeSlabs
 * installed it also lets the (survival) player mine one half of two double slabs through the
 * regular block breaking code, logs the outcome and takes another screenshot. Then quits.
 */
public final class SmokeClient implements ClientModInitializer {
    private int ticks;
    private int y;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        ticks++;
        boolean klee = FabricLoader.getInstance().isModLoaded("kleeslabs");
        if (ticks == 40) {
            y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, -2);
            mc.options.hideGui = true;
        }
        if (ticks == 40) {
            mc.player.moveTo(0.5D, y, -2.5D, 0.0F, 28.0F);
        }
        if (ticks == 240) {
            shot(mc, "refabslab-overview.png");
        }
        if (ticks == 260) {
            mc.player.moveTo(-1.5D, y, 1.2D, -20.0F, 38.0F);
        }
        if (ticks == 420) {
            shot(mc, "refabslab-closeup.png");
        }
        if (klee) {
            // Look at the front face of the double slab, upper half (y + 0.75) resp. lower half (y + 0.25).
            if (ticks == 440) {
                mine(mc, new BlockPos(-6, y, 4), 23.5F);
            }
            if (ticks == 470) {
                mine(mc, new BlockPos(-4, y, 4), 34.4F);
            }
            if (ticks == 500) {
                server(mc, player -> player.connection.teleport(-4.0D, y, 0.8D, 0.0F, 30.0F));
            }
            if (ticks == 660) {
                shot(mc, "refabslab-kleeslabs.png");
            }
        }
        if (ticks == (klee ? 720 : 480)) {
            SmokeServer.LOGGER.info("[smoke] client done");
            mc.stop();
        }
    }

    /** Survival player with a diamond pickaxe stands 2 blocks in front of {@code pos} and breaks it. */
    private void mine(Minecraft mc, BlockPos pos, float pitch) {
        server(mc, player -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            player.connection.teleport(pos.getX() + 0.5D, pos.getY(), pos.getZ() - 2.0D, 0.0F, pitch);
            var level = player.serverLevel();
            var before = level.getBlockState(pos);
            boolean destroyed = player.gameMode.destroyBlock(pos);
            var after = level.getBlockState(pos);
            var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2.0D))
                    .stream().map(item -> item.getItem().toString()).toList();
            SmokeServer.LOGGER.info("[smoke] kleeslabs mine {} looking pitch {}: {} -> destroyBlock={} -> {} drops={}",
                    pos.toShortString(), pitch, before, destroyed, after, drops);
        });
    }

    private static void server(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().get(0)));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> SmokeServer.LOGGER.info("[smoke] {}", message.getString()));
    }
}
