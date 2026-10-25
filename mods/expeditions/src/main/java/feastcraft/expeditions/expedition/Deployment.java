package feastcraft.expeditions.expedition;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Finds a drop point and sends a player there, over as many ticks as the overworld needs to generate
 * it. Nothing is recorded until the final teleport, so a crash mid-deployment leaves the player at base.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class Deployment {
    private static final TicketType<ChunkPos> LOADING = TicketType.create(
            Expeditions.MOD_ID + ":deployment", Comparator.comparingLong(ChunkPos::toLong));
    private static final int MAX_CANDIDATES = 64;
    private static final int MAX_LOADING_TICKS = 30 * 20;
    private static final Map<UUID, Deployment> ACTIVE = new HashMap<>();

    private final UUID playerId;
    private int candidatesTried;
    @Nullable
    private BlockPos candidate;
    private int loadingTicks;

    private Deployment(UUID playerId) {
        this.playerId = playerId;
    }

    /** Returns false if the player is already deploying or already on a raid. */
    public static boolean start(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID()) || Expedition.hasRaid(player)) {
            return false;
        }
        ACTIVE.put(player.getUUID(), new Deployment(player.getUUID()));
        player.displayClientMessage(Component.translatable("feastcraft_expeditions.deploy.searching"), true);
        return true;
    }

    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !ACTIVE.isEmpty()) {
            ACTIVE.values().removeIf(deployment -> deployment.step(event.getServer()));
        }
    }

    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Deployment deployment = ACTIVE.remove(event.getEntity().getUUID());
        if (deployment != null) {
            deployment.releaseChunk(event.getEntity().getServer().overworld());
        }
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.clear();
    }

    /** Advances one tick; returns true once the deployment is over, successful or not. */
    private boolean step(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null || !player.isAlive() || Expedition.hasRaid(player)) {
            releaseChunk(overworld);
            return true;
        }

        if (candidate == null) {
            if (candidatesTried++ >= MAX_CANDIDATES) {
                player.displayClientMessage(Component.translatable("feastcraft_expeditions.deploy.failed"), false);
                return true;
            }
            BlockPos picked = pickCandidate(overworld);
            if (picked != null) {
                candidate = picked;
                loadingTicks = 0;
                overworld.getChunkSource().addRegionTicket(LOADING, new ChunkPos(picked), 1, new ChunkPos(picked));
            }
            return false;
        }

        LevelChunk chunk = overworld.getChunkSource().getChunkNow(candidate.getX() >> 4, candidate.getZ() >> 4);
        if (chunk == null) {
            if (++loadingTicks > MAX_LOADING_TICKS) {
                releaseChunk(overworld);
            }
            return false;
        }

        Optional<BlockPos> spot = standingSpot(overworld, chunk, candidate);
        releaseChunk(overworld);
        if (spot.isEmpty()) {
            return false;
        }
        Expedition.deploy(player, spot.get());
        return true;
    }

    @Nullable
    private static BlockPos pickCandidate(ServerLevel overworld) {
        int min = ExpeditionsConfig.DROP_MIN_DISTANCE.get();
        int max = Math.max(min, ExpeditionsConfig.DROP_MAX_DISTANCE.get());
        DropGeometry.Offset offset = DropGeometry.sample(overworld.random.nextDouble(), overworld.random.nextDouble(), min, max);
        BlockPos spawn = overworld.getSharedSpawnPos();
        int x = spawn.getX() + (int) Math.floor(offset.x());
        int z = spawn.getZ() + (int) Math.floor(offset.z());
        return LandCheck.isDryLand(overworld, x, z) ? new BlockPos(x, 0, z) : null;
    }

    /** The column in the chunk nearest the candidate where a player can stand on solid, dry ground. */
    private static Optional<BlockPos> standingSpot(ServerLevel level, LevelChunk chunk, BlockPos candidate) {
        ChunkPos chunkPos = chunk.getPos();
        return IntStream.range(0, 256)
                .mapToObj(i -> new BlockPos(chunkPos.getMinBlockX() + (i & 15), 0, chunkPos.getMinBlockZ() + (i >> 4)))
                .sorted(Comparator.comparingInt(column -> column.distManhattan(candidate)))
                .map(column -> column.atY(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) + 1))
                .filter(feet -> isSafeToStand(level, feet))
                .findFirst();
    }

    private static boolean isSafeToStand(ServerLevel level, BlockPos feet) {
        BlockPos ground = feet.below();
        return level.getBlockState(ground).isValidSpawn(level, ground, EntityType.PLAYER)
                && level.getFluidState(ground).isEmpty()
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(feet.above()).isEmpty()
                && level.noCollision(EntityType.PLAYER.getAABB(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
    }

    private void releaseChunk(ServerLevel overworld) {
        if (candidate != null) {
            overworld.getChunkSource().removeRegionTicket(LOADING, new ChunkPos(candidate), 1, new ChunkPos(candidate));
            candidate = null;
        }
    }
}
