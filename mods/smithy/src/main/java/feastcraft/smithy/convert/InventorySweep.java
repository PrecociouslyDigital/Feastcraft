package feastcraft.smithy.convert;

import feastcraft.smithy.Smithy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Converts what a player already carries: trades taken before the mod loaded,
 * rewards from other mods, and plain tools that survived the switch. Runs every
 * second, so a converted item is back before the player looks twice.
 */
public final class InventorySweep {
    private static final int INTERVAL = 20;
    private static final Map<UUID, Integer> TICKS_ONLINE = new HashMap<>();

    private InventorySweep() {
    }

    /** Forget players between worlds, so the map doesn't grow forever. */
    public static void onServerStart(ServerStartedEvent event) {
        TICKS_ONLINE.clear();
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int ticks = TICKS_ONLINE.merge(player.getUUID(), 1, Integer::sum);
            if (ticks % INTERVAL == 0) {
                sweep(player);
            }
        }
    }

    private static void sweep(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack converted = Smithy.convert(inventory.getItem(slot));
            inventory.setItem(slot, converted);
        }
    }
}
