package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <p>Saved tickets are wiped on load and re-derived from the block entities. On server start the
 * chunks that had a ticket are loaded once, and their hearths re-register through
 * {@link net.minecraft.world.level.block.entity.BlockEntity#onLoad}.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class HearthIndex {
    private static final Map<ChunkPos, Integer> FUELED_HEARTHS = new HashMap<>();
    private static final Set<ChunkPos> TICKETED_AT_LAST_SAVE = new HashSet<>();

    private HearthIndex() {
    }

    public static void registerTicketValidation() {
        ForgeChunkManager.setForcedChunkLoadingCallback(Expeditions.MOD_ID, (level, tickets) -> {
            if (BaseDimension.isBase(level)) {
                tickets.getBlockTickets().values().forEach(chunks -> chunks.getSecond().forEach(
                        (long chunk) -> TICKETED_AT_LAST_SAVE.add(new ChunkPos(chunk))));
            }
            List.copyOf(tickets.getBlockTickets().keySet()).forEach(tickets::removeAllTickets);
        });
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        FUELED_HEARTHS.clear();
        if (ExpeditionsConfig.EMBER_ENABLED.get()) {
            ServerLevel base = BaseDimension.level(event.getServer());
            // Loading the chunk fires onLoad for its hearths, which registers fuel and tickets.
            TICKETED_AT_LAST_SAVE.forEach(chunk -> base.getChunk(chunk.x, chunk.z));
        }
        TICKETED_AT_LAST_SAVE.clear();
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        FUELED_HEARTHS.clear();
    }

    public static Set<ChunkPos> fueledChunks() {
        return Set.copyOf(FUELED_HEARTHS.keySet());
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

    /** The hearth went cold or was broken, so its chunk stops unless another hearth burns there. */
    static void onStopped(ServerLevel base, HearthBlockEntity hearth) {
        if (forget(base, hearth)) {
            setTicket(base, hearth, false);
        }
    }

    /**
     * The hearth's chunk unloaded, which for a ticketed chunk only happens at shutdown. The ticket
     * must stay so it is saved and reloads the chunk on the next start.
     */
    static void onChunkUnloaded(ServerLevel base, HearthBlockEntity hearth) {
        forget(base, hearth);
    }

    /** Returns whether this was the last fueled hearth indexed in its chunk. */
    private static boolean forget(ServerLevel base, HearthBlockEntity hearth) {
        if (!hearth.indexed) {
            return false;
        }
        hearth.indexed = false;
        return count(base, hearth, -1) == 0;
    }

    static void onLit(ServerLevel base, HearthBlockEntity hearth) {
        onLoaded(base, hearth);
        broadcast(base, "feastcraft_expeditions.hearth.lit");
    }

    static void onDied(ServerLevel base, HearthBlockEntity hearth) {
        onStopped(base, hearth);
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
