package feastcraft.expeditions.expedition;

import com.mojang.logging.LogUtils;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.TickTime;
import feastcraft.expeditions.exfil.ExfilCountdown;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Finds a drop point and sends the gathered party there, over as many ticks as the overworld needs
 * to generate it. Activating the gate starts a countdown; when it ends, everyone (and every mount)
 * in the gate's zone deploys together to one shared drop point.
 * Nothing is recorded until the final teleport, so a crash mid-deployment leaves the party at base.
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
    private static final int ZONE_RADIUS = 8;
    private static final int ZONE_HALF_HEIGHT = 6;
    private static final TagKey<EntityType<?>> NOT_TAKEN = TagKey.create(Registries.ENTITY_TYPE, Expeditions.id("not_extractable"));

    /** One group deploys at a time: the gate is the base's single door out. */
    @Nullable
    private static Deployment ACTIVE;

    private final UUID initiatorId;
    private final BlockPos gate;
    private ExfilCountdown countdown;
    private int candidatesTried;
    @Nullable
    private BlockPos candidate;
    private int progressTicks;
    @Nullable
    private List<UUID> raiders;
    @Nullable
    private List<Integer> mounts;

    private Deployment(UUID initiatorId, BlockPos gate) {
        this.initiatorId = initiatorId;
        this.gate = gate;
        this.countdown = ExfilCountdown.start(ExpeditionsConfig.CO_DEPLOY_COUNTDOWN_TICKS.get());
    }

    /** Returns false if a deployment is already underway or the player is already on a raid. */
    public static boolean start(ServerPlayer player, BlockPos gate) {
        if (ACTIVE != null || Expedition.hasRaid(player)) {
            return false;
        }
        ACTIVE = new Deployment(player.getUUID(), gate);
        LOGGER.info("Deployment for {} started", player.getGameProfile().getName());
        player.displayClientMessage(Component.translatable("feastcraft_expeditions.gate.countdown",
                TickTime.format(ExpeditionsConfig.CO_DEPLOY_COUNTDOWN_TICKS.get())), false);
        return true;
    }

    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ACTIVE != null) {
            if (ACTIVE.step(event.getServer())) {
                ACTIVE = null;
            }
        }
    }

    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ACTIVE != null && event.getEntity().getUUID().equals(ACTIVE.initiatorId)) {
            ACTIVE.releaseChunk(event.getEntity().getServer().overworld());
            ACTIVE = null;
        }
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE = null;
    }

    /** Advances one tick; returns true once the deployment is over, successful or not. */
    private boolean step(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        ServerPlayer initiator = server.getPlayerList().getPlayer(initiatorId);
        if (initiator == null || !initiator.isAlive()) {
            LOGGER.debug("Deployment for {} abandoned (initiator gone or dead)", initiatorId);
            releaseChunk(overworld);
            return true;
        }

        if (countdown.ticksLeft() > 0) {
            countdown = countdown.step(false);
            return false;
        }
        if (raiders == null) {
            gather(server);
            LOGGER.info("Deployment for {} gathered {} players and {} mounts", initiatorId, raiders.size(), mounts.size());
            return false;
        }

        if (progressTicks++ % 20 == 0) {
            showProgress(server, candidate == null);
        }

        if (candidate == null) {
            if (candidatesTried++ >= MAX_CANDIDATES) {
                LOGGER.info("Deployment for {} found no safe drop point in {} candidates", initiatorId, candidatesTried);
                initiator.displayClientMessage(Component.translatable("feastcraft_expeditions.deploy.failed"), false);
                return true;
            }
            BlockPos picked = pickCandidate(overworld);
            if (picked != null) {
                LOGGER.debug("Deployment for {} candidate {} at {}", initiatorId, candidatesTried, picked.toShortString());
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
                    initiatorId, candidate.toShortString(), candidatesTried, MAX_CANDIDATES);
            releaseChunk(overworld);
            return false;
        }
        BlockPos spotPos = spot.get();
        releaseChunk(overworld);
        LOGGER.info("Deployment for {} landed at {} after {} ticks", initiatorId, spotPos.toShortString(), progressTicks);
        depart(server, spotPos);
        return true;
    }

    /** Action bar seconds elapsed, plus a swirl of particles around each waiting raider. */
    private void showProgress(MinecraftServer server, boolean searching) {
        String key = searching ? "feastcraft_expeditions.deploy.searching" : "feastcraft_expeditions.deploy.loading";
        Component message = Component.translatable(key, progressTicks / 20);
        for (UUID id : raiders) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                continue;
            }
            player.displayClientMessage(message, true);
            ServerLevel level = player.serverLevel();
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(progressTicks * 18 + i * 45);
                level.sendParticles(ParticleTypes.ENCHANT, player.getX() + Math.cos(angle) * 1.5,
                        player.getY() + 1.0, player.getZ() + Math.sin(angle) * 1.5, 1, 0.0, 0.05, 0.0, 0.0);
            }
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.2, player.getZ(), 4, 0.2, 0.3, 0.2, 0.5);
        }
    }

    /** Fixes the party: whoever is in the gate zone when the countdown ends goes, even if they wander off while the drop loads. */
    private void gather(MinecraftServer server) {
        ServerLevel base = feastcraft.expeditions.base.BaseDimension.level(server);
        AABB zone = new AABB(gate).inflate(ZONE_RADIUS, ZONE_HALF_HEIGHT, ZONE_RADIUS);
        double centerX = gate.getX() + 0.5;
        double centerZ = gate.getZ() + 0.5;
        raiders = new ArrayList<>();
        mounts = new ArrayList<>();
        for (Entity entity : base.getEntities((Entity) null, zone,
                entity -> Math.hypot(entity.getX() - centerX, entity.getZ() - centerZ) <= ZONE_RADIUS && comes(entity))) {
            entity.stopRiding();
            entity.ejectPassengers();
            if (entity instanceof ServerPlayer player) {
                raiders.add(player.getUUID());
            } else {
                mounts.add(entity.getId());
            }
        }
    }

    private static boolean comes(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.isAlive() && Expedition.isBound(player) && !Expedition.hasRaid(player);
        }
        return entity instanceof Mob mob && mob.isAlive() && !mob.getType().is(NOT_TAKEN);
    }

    /** Players first, so leashed mounts find their holder already at the drop. */
    private void depart(MinecraftServer server, BlockPos drop) {
        raiders.stream()
                .map(id -> server.getPlayerList().getPlayer(id))
                .filter(player -> player != null && Expedition.isBound(player))
                .forEach(player -> Expedition.deploy(player, drop));
        ServerLevel base = feastcraft.expeditions.base.BaseDimension.level(server);
        mounts.stream()
                .map(base::getEntity)
                .filter(mount -> mount != null && mount.isAlive() && !mount.isRemoved())
                .forEach(mount -> Expedition.arriveAtDrop(mount, drop));
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
