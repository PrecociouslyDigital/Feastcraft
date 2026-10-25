package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While the base is fueled, holds a Forge ticking ticket on every chunk inside its bounds so
 * machines and crops run with no player present (vanilla forced chunks do not random tick).
 * Tickets are derived from the fuel state: saved ones are dropped on load and re-derived once the
 * server has started.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class BaseChunkLoading {
    private BaseChunkLoading() {
    }

    public static void registerTicketValidation() {
        ForgeChunkManager.setForcedChunkLoadingCallback(Expeditions.MOD_ID,
                (level, tickets) -> tickets.getBlockTickets().keySet().forEach(tickets::removeAllTickets));
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        ServerLevel base = BaseDimension.level(event.getServer());
        sync(base, BaseFuel.get(base).tank().running());
    }

    public static void sync(ServerLevel base, boolean running) {
        for (BaseBounds.Chunk chunk : BaseDimension.bounds().chunks()) {
            ForgeChunkManager.forceChunk(base, Expeditions.MOD_ID, BaseDimension.SPAWN, chunk.x(), chunk.z(), running, true);
        }
    }
}
