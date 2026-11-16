package feastcraft.expeditions.exfil;

import com.mojang.logging.LogUtils;
import dev.shadowsoffire.apotheosis.adventure.boss.ApothBoss;
import dev.shadowsoffire.apotheosis.adventure.boss.ApothMiniboss;
import dev.shadowsoffire.apotheosis.adventure.boss.BossRegistry;
import dev.shadowsoffire.apotheosis.adventure.boss.MinibossRegistry;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.expedition.Expedition;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * The hostile waves that attack a beacon while it counts down. Wave mobs spawn whatever the light,
 * and undead among them do not burn until their countdown ends.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class ExfilWaves {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WAVE_TAG = Expeditions.MOD_ID + ":exfil_wave";
    private static final double RAIDER_RANGE = 48;
    private static final double MIN_SPAWN_DISTANCE = 12;
    private static final double MAX_SPAWN_DISTANCE = 24;
    private static final int SPAWN_TRIES = 16;

    private ExfilWaves() {
    }

    @SubscribeEvent
    static void onCountdown(ExfilEvent.Countdown event) {
        ServerLevel level = event.getLevel();
        WavePlans.planFor(level, event.getKey()).ifPresent(plan -> {
            List<WavePlan.Wave> due = WaveSchedule.due(plan.waves(), WavePlan.Wave::at, event.getDuration(), event.getTicksLeft());
            if (due.isEmpty()) {
                return;
            }
            // With no one left to attack, a wave would only load chunks to stand in them.
            List<ServerPlayer> raiders = raidersNear(level, event.getBeacon());
            if (raiders.isEmpty()) {
                return;
            }
            var attack = new Attack(level, event.getBeacon(), event.getKey(), event.getTicksLeft(), raiders);
            due.forEach(wave -> wave.spawns().forEach(attack::spawn));
        });
    }

    /** Marks a mob as part of a beacon's waves: it stays behind at departure and is sun-proof while the beacon counts down. */
    public static void tagAsWave(Entity entity, long beaconKey) {
        entity.getPersistentData().putLong(WAVE_TAG, beaconKey);
    }

    public static boolean isWaveMob(Entity entity) {
        return entity.getPersistentData().contains(WAVE_TAG, Tag.TAG_LONG);
    }

    public static boolean isSunProof(Mob mob) {
        return isWaveMob(mob)
                && mob.level() instanceof ServerLevel level
                && ActiveExfils.get(level).countdownAt(mob.getPersistentData().getLong(WAVE_TAG)).isPresent();
    }

    private static List<ServerPlayer> raidersNear(ServerLevel level, BlockPos beacon) {
        Vec3 center = Vec3.atCenterOf(beacon);
        return level.players().stream()
                .filter(player -> player.isAlive() && player.position().closerThan(center, RAIDER_RANGE))
                .filter(player -> Expedition.isBound(player) && Expedition.hasRaid(player))
                .toList();
    }

    private record Attack(ServerLevel level, BlockPos beacon, long key, int ticksLeft, List<ServerPlayer> raiders) {
        void spawn(WavePlan.WaveSpawn spawn) {
            if (level.random.nextDouble() >= spawn.chance()) {
                return;
            }
            if (spawn instanceof WavePlan.EntitySpawn entity) {
                int count = entity.count().sample(level.random) * (entity.perRaider() ? raiders.size() : 1);
                for (int i = 0; i < count; i++) {
                    spawnEntity(entity);
                }
            } else if (spawn instanceof WavePlan.ApotheosisBoss boss) {
                spawnBoss(boss);
            }
        }

        private void spawnEntity(WavePlan.EntitySpawn spawn) {
            Optional<BlockPos> spot = findSpot(spawn.entity());
            if (spot.isEmpty()) {
                return;
            }
            if (!(spawn.entity().create(level) instanceof Mob mob)) {
                LOGGER.error("Exfil waves can only spawn mobs, not {}", EntityType.getKey(spawn.entity()));
                return;
            }
            BlockPos pos = spot.get();
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            // Merged after finalizing, so the plan's NBT wins over whatever finalizing rolled.
            spawn.nbt().ifPresent(nbt -> mob.load(mob.saveWithoutId(new CompoundTag()).merge(nbt)));
            spawn.effects().forEach(effect -> mob.addEffect(new MobEffectInstance(effect.effect(), ticksLeft, effect.amplifier())));
            join(mob);

            if (level.random.nextDouble() < spawn.minibossChance()) {
                ApothMiniboss miniboss = MinibossRegistry.INSTANCE.getRandomItem(level.random, luck(),
                        IDimensional.matches(level), MinibossRegistry.IEntityMatch.matches(mob));
                if (miniboss != null) {
                    // After joining the level, which is when Apotheosis transforms its own minibosses.
                    miniboss.transformMiniboss(level, mob, level.random, luck());
                    join(mob);
                }
            }
        }

        private void spawnBoss(WavePlan.ApotheosisBoss spawn) {
            ApothBoss boss = spawn.boss()
                    .map(BossRegistry.INSTANCE::getValue)
                    .orElseGet(() -> BossRegistry.INSTANCE.getRandomItem(level.random, luck(), IDimensional.matches(level)));
            if (boss == null) {
                LOGGER.warn("No Apotheosis boss {} for an exfil wave in {}", spawn.boss().map(Object::toString).orElse("at all"), level.dimension().location());
                return;
            }
            findSpot(boss.getEntity()).ifPresent(pos -> join(boss.createBoss(level, pos, level.random, luck())));
        }

        /**
         * Adds a mob, with whatever it rides or carries, to the level as part of this beacon's waves.
         * Already-added entities are only retagged, which picks up mounts and riders added since.
         */
        private void join(Mob mob) {
            Entity root = mob.getRootVehicle();
            root.getSelfAndPassengers().forEach(entity -> tagAsWave(entity, key));
            if (!root.isAddedToWorld()) {
                level.addFreshEntityWithPassengers(root);
            }
            root.getSelfAndPassengers()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .forEach(attacker -> attacker.setTarget(nearestRaider(attacker)));
        }

        private ServerPlayer nearestRaider(Entity entity) {
            return raiders.stream().min(Comparator.comparingDouble(entity::distanceToSqr)).orElseThrow();
        }

        /** Apotheosis rolls bosses with the luck of the player they spawn near; here, the raider nearest the beacon. */
        private float luck() {
            Vec3 center = Vec3.atCenterOf(beacon);
            return raiders.stream().min(Comparator.comparingDouble(raider -> raider.distanceToSqr(center))).orElseThrow().getLuck();
        }

        /** A standing spot on the surface of a ring around the beacon, outside its zone, that fits the entity. */
        private Optional<BlockPos> findSpot(EntityType<?> type) {
            SpawnPlacements.Type placement = SpawnPlacements.getPlacementType(type);
            for (int i = 0; i < SPAWN_TRIES; i++) {
                double angle = level.random.nextDouble() * Mth.TWO_PI;
                double distance = Mth.lerp(level.random.nextDouble(), MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
                BlockPos column = beacon.offset(Mth.floor(Math.cos(angle) * distance), 0, Mth.floor(Math.sin(angle) * distance));
                if (!level.hasChunkAt(column)) {
                    continue;
                }
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                AABB box = type.getAABB(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
                if (NaturalSpawner.isSpawnPositionOk(placement, level, surface, type) && level.noCollision(box)) {
                    return Optional.of(surface);
                }
            }
            return Optional.empty();
        }
    }
}
