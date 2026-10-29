package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Which chunks of the base currently hold a fueled hearth, and the tickets that keep those chunks
 * running. Each hearth powers only the chunk it occupies; chunks without a fueled hearth stay in
 * stasis even when a player is standing in them.
 *
 * <p>On server start every chunk inside {@link BaseBounds} is loaded once so saved hearths can
 * register themselves through {@link net.minecraft.world.level.block.entity.BlockEntity#onLoad};
 * unfueled chunks unload again on their own, since only fueled hearths hold tickets. Saved tickets
 * are wiped on load and re-derived from the block entities, so the index can never disagree with
 * the world.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class HearthIndex {
    private static final Map<ChunkPos, Integer> FUELED_HEARTHS = new HashMap<>();

    private HearthIndex() {
    }

    public static void registerTicketValidation() {
        ForgeChunkManager.setForcedChunkLoadingCallback(Expeditions.MOD_ID,
                (level, tickets) -> tickets.getBlockTickets().keySet().forEach(tickets::removeAllTickets));
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        FUELED_HEARTHS.clear();
        if (!ExpeditionsConfig.EMBER_ENABLED.get()) {
            return;
        }
        ServerLevel base = BaseDimension.level(event.getServer());
        for (BaseBounds.Chunk chunk : BaseDimension.bounds().chunks()) {
            // Loading the chunk fires onLoad for its hearths, which registers fuel and tickets.
            base.getChunk(chunk.x(), chunk.z());
        }
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        FUELED_HEARTHS.clear();
    }

    /** Whether any fueled hearth keeps this chunk running. */
    public static boolean isFueled(ChunkPos pos) {
        return FUELED_HEARTHS.getOrDefault(pos, 0) > 0;
    }

    static void onLoaded(ServerLevel base, HearthBlockEntity hearth) {
        if (!ExpeditionsConfig.EMBER_ENABLED.get() || !hearth.fueled() || hearth.indexed) {
            return;
        }
        hearth.indexed = true;
        if (count(base, hearth, 1) == 1) {
            setTicket(base, hearth, true);
        }
    }

    static void onUnloaded(ServerLevel base, HearthBlockEntity hearth) {
        if (!hearth.indexed) {
            return;
        }
        hearth.indexed = false;
        if (count(base, hearth, -1) == 0) {
            setTicket(base, hearth, false);
        }
    }

    static void onLit(ServerLevel base, HearthBlockEntity hearth) {
        onLoaded(base, hearth);
        broadcast(base, "feastcraft_expeditions.hearth.lit");
    }

    static void onDied(ServerLevel base, HearthBlockEntity hearth) {
        onUnloaded(base, hearth);
        broadcast(base, "feastcraft_expeditions.hearth.died");
    }

    /** Adjusts the count of fueled hearths in the hearth's chunk, returning the new count. */
    private static int count(ServerLevel base, HearthBlockEntity hearth, int delta) {
        ChunkPos chunk = hearth.chunkPos();
        int count = FUELED_HEARTHS.getOrDefault(chunk, 0) + delta;
        if (count <= 0) {
            FUELED_HEARTHS.remove(chunk);
            return 0;
        }
        FUELED_HEARTHS.put(chunk, count);
        return count;
    }

    private static void setTicket(ServerLevel base, HearthBlockEntity hearth, boolean add) {
        BlockPos pos = hearth.getBlockPos();
        ForgeChunkManager.forceChunk(base, Expeditions.MOD_ID, pos,
                hearth.chunkPos().x, hearth.chunkPos().z, add, true);
    }

    private static void broadcast(ServerLevel base, String key) {
        Component message = Component.translatable(key).withStyle(ChatFormatting.GOLD);
        base.players().forEach(player -> player.sendSystemMessage(message));
    }
}
