package feastcraft.expeditions.expedition;

import com.mojang.logging.LogUtils;
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
import net.minecraft.core.particles.ParticleTypes;
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
import org.slf4j.Logger;

/**
 * Finds a drop point and sends a player there, over as many ticks as the overworld needs to generate
 * it. Nothing is recorded until the final teleport, so a crash mid-deployment leaves the player at base.
 *
 * A fresh chunk this far out can take about a minute to generate, so the deployment waits as long as
 * it takes; timing out would throw the progress away.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class Deployment {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TicketType<ChunkPos> LOADING = TicketType.create(
            Expeditions.MOD_ID + ":deployment", Comparator.comparingLong(ChunkPos::toLong));
    private static final int MAX_CANDIDATES = 64;
    /** Radius 0: only the candidate chunk is fully generated. */
    private static final int TICKET_RADIUS = 0;
    private static final Map<UUID, Deployment> ACTIVE = new HashMap<>();

    private final UUID playerId;
    private int candidatesTried;
    @Nullable
    private BlockPos candidate;
    private int progressTicks;

    private Deployment(UUID playerId) {
        this.playerId = playerId;
    }

    /** Returns false if the player is already deploying or already on a raid. */
    public static boolean start(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID()) || Expedition.hasRaid(player)) {
            return false;
        }
        ACTIVE.put(player.getUUID(), new Deployment(player.getUUID()));
        LOGGER.info("Deployment for {} started", player.getGameProfile().getName());
        player.displayClientMessage(Component.translatable("feastcraft_expeditions.deploy.searching", 0), true);
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
            LOGGER.debug("Deployment for {} abandoned (player gone, dead, or already on a raid)", playerId);
            releaseChunk(overworld);
            return true;
        }

        if (progressTicks++ % 20 == 0) {
            showProgress(player, candidate == null);
        }

        if (candidate == null) {
            if (candidatesTried++ >= MAX_CANDIDATES) {
                LOGGER.info("Deployment for {} found no safe drop point in {} candidates", playerId, candidatesTried);
                player.displayClientMessage(Component.translatable("feastcraft_expeditions.deploy.failed"), false);
                return true;
            }
            BlockPos picked = pickCandidate(overworld);
            if (picked != null) {
                LOGGER.debug("Deployment for {} candidate {} at {}", playerId, candidatesTried, picked.toShortString());
                candidate = picked;
                overworld.getChunkSource().addRegionTicket(LOADING, new ChunkPos(picked), TICKET_RADIUS, new ChunkPos(picked));
            }
            return false;
        }

        LevelChunk chunk = overworld.getChunkSource().getChunkNow(candidate.getX() >> 4, candidate.getZ() >> 4);
        if (chunk == null) {
            return false;
        }

        Optional<BlockPos> spot = standingSpot(overworld, chunk, candidate);
        if (spot.isEmpty()) {
            LOGGER.debug("Deployment for {}: no standing spot near {}, moving on (attempt {}/{})",
                    playerId, candidate.toShortString(), candidatesTried, MAX_CANDIDATES);
            releaseChunk(overworld);
            return false;
        }
        BlockPos spotPos = spot.get();
        releaseChunk(overworld);
        LOGGER.info("Deployment for {} landed at {} after {} ticks", playerId, spotPos.toShortString(), progressTicks);
        Expedition.deploy(player, spotPos);
        return true;
    }

    /** Action bar seconds elapsed, plus a swirl of particles around the player. */
    private void showProgress(ServerPlayer player, boolean searching) {
        String key = searching ? "feastcraft_expeditions.deploy.searching" : "feastcraft_expeditions.deploy.loading";
        player.displayClientMessage(Component.translatable(key, progressTicks / 20), true);
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(progressTicks * 18 + i * 45);
            level.sendParticles(ParticleTypes.ENCHANT, player.getX() + Math.cos(angle) * 1.5,
                    player.getY() + 1.0, player.getZ() + Math.sin(angle) * 1.5, 1, 0.0, 0.05, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.2, player.getZ(), 4, 0.2, 0.3, 0.2, 0.5);
    }

    @Nullable
    private static BlockPos pickCandidate(ServerLevel overworld) {
        int min = ExpeditionsConfig.DROP_MIN_DISTANCE.get();
        int max = Math.max(min, ExpeditionsConfig.DROP_MAX_DISTANCE.get());
        DropGeometry.Offset offset = DropGeometry.sample(overworld.random.nextDouble(), overworld.random.nextDouble(), min, max);
        BlockPos spawn = overworld.getSharedSpawnPos();
        int x = spawn.getX() + (int) Math.floor(offset.x());
        int z = spawn.getZ() + (int) Math.floor(offset.z());
        BlockPos candidate = new BlockPos(x, overworld.getSeaLevel(), z);
        return LandCheck.isDryLand(overworld, x, z) ? candidate : null;
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
            overworld.getChunkSource().removeRegionTicket(LOADING, new ChunkPos(candidate), TICKET_RADIUS, new ChunkPos(candidate));
            candidate = null;
        }
    }
}
