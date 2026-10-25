package feastcraft.expeditions.exfil;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.TickTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The exfils counting down in one level. They tick with the level, whether or not the beacon's
 * chunk is loaded, and show their countdown to everyone nearby.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class ActiveExfils extends SavedData {
    private static final String NAME = Expeditions.MOD_ID + "_active_exfils";
    private static final double BOSS_BAR_RANGE = 96;
    private static final double FLARE_RANGE = 512;
    private static final int FLARE_INTERVAL = 10;

    private record Active(BlockPos beacon, int duration, ExfilCountdown countdown) {
    }

    private final Map<Long, Active> active = new LinkedHashMap<>();
    private final Map<Long, ServerBossEvent> bossBars = new HashMap<>();

    static ActiveExfils get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ActiveExfils::load, ActiveExfils::new, NAME);
    }

    @SubscribeEvent
    static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            get(level).tick(level);
        }
    }

    void start(long key, BlockPos beacon, int duration) {
        active.put(key, new Active(beacon.immutable(), duration, ExfilCountdown.start(duration)));
        setDirty();
    }

    void cancel(long key) {
        if (active.remove(key) != null) {
            removeBossBar(key);
            setDirty();
        }
    }

    public Optional<ExfilCountdown> countdownAt(long key) {
        return Optional.ofNullable(active.get(key)).map(Active::countdown);
    }

    private void tick(ServerLevel level) {
        // A copy, because departures teleport entities and listeners may start or cancel exfils.
        for (long key : List.copyOf(active.keySet())) {
            Active exfil = active.get(key);
            if (exfil == null) {
                continue;
            }
            MinecraftForge.EVENT_BUS.post(new ExfilEvent.Countdown(level, exfil.beacon(), exfil.countdown().ticksLeft()));

            List<Entity> passengers = List.of();
            boolean vetoed = false;
            if (exfil.countdown().isDue()) {
                passengers = Departure.passengersAt(level, exfil.beacon());
                vetoed = MinecraftForge.EVENT_BUS.post(new ExfilEvent.Depart(level, exfil.beacon(), passengers));
            }

            ExfilCountdown next = exfil.countdown().step(vetoed);
            setDirty();
            if (next instanceof ExfilCountdown.Departed) {
                active.remove(key);
                removeBossBar(key);
                Departure.sendHome(passengers);
                MinecraftForge.EVENT_BUS.post(new ExfilEvent.Departed(level, exfil.beacon(), passengers));
            } else {
                Active updated = new Active(exfil.beacon(), exfil.duration(), next);
                active.put(key, updated);
                showCountdown(level, key, updated);
            }
        }
    }

    private void showCountdown(ServerLevel level, long key, Active exfil) {
        ServerBossEvent bar = bossBars.computeIfAbsent(key, k ->
                new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS));
        bar.setName(exfil.countdown() instanceof ExfilCountdown.AtZero
                ? Component.translatable("feastcraft_expeditions.exfil.held")
                : Component.translatable("feastcraft_expeditions.exfil.countdown", TickTime.format(exfil.countdown().ticksLeft())));
        bar.setProgress(exfil.countdown().ticksLeft() / (float) exfil.duration());

        Vec3 beacon = Vec3.atCenterOf(exfil.beacon());
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (player.level() != level || !player.position().closerThan(beacon, BOSS_BAR_RANGE)) {
                bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (player.position().closerThan(beacon, BOSS_BAR_RANGE)) {
                bar.addPlayer(player);
            }
        }

        if (level.getGameTime() % FLARE_INTERVAL == 0) {
            flare(level, beacon);
        }
    }

    /** A tall column of sparks anyone within a long way can see, telling rivals that someone is leaving. */
    private static void flare(ServerLevel level, Vec3 beacon) {
        for (ServerPlayer player : level.players()) {
            if (player.position().closerThan(beacon, FLARE_RANGE)) {
                for (int height = 2; height <= 62; height += 6) {
                    level.sendParticles(player, ParticleTypes.FIREWORK, true,
                            beacon.x, beacon.y + height, beacon.z, 4, 0.15, 1.5, 0.15, 0.01);
                }
            }
        }
    }

    private void removeBossBar(long key) {
        ServerBossEvent bar = bossBars.remove(key);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    private static ActiveExfils load(CompoundTag tag) {
        ActiveExfils exfils = new ActiveExfils();
        for (String key : tag.getAllKeys()) {
            CompoundTag entry = tag.getCompound(key);
            exfils.active.put(Long.parseLong(key), new Active(
                    NbtUtils.readBlockPos(entry.getCompound("beacon")),
                    Math.max(1, entry.getInt("duration")),
                    ExfilCountdown.resume(entry.getInt("ticksLeft"))));
        }
        return exfils;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        active.forEach((key, exfil) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("beacon", NbtUtils.writeBlockPos(exfil.beacon()));
            entry.putInt("duration", exfil.duration());
            entry.putInt("ticksLeft", exfil.countdown().ticksLeft());
            tag.put(Long.toString(key), entry);
        });
        return tag;
    }
}
