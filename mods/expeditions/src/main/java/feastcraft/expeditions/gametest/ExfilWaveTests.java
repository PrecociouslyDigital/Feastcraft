package feastcraft.expeditions.gametest;

import static feastcraft.expeditions.gametest.TestFixtures.PLENTY_OF_FUEL;
import static feastcraft.expeditions.gametest.TestFixtures.base;
import static feastcraft.expeditions.gametest.TestFixtures.fuelBase;
import static feastcraft.expeditions.gametest.TestFixtures.still;

import com.mojang.authlib.GameProfile;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.exfil.ExfilBeacons;
import feastcraft.expeditions.exfil.ExfilWaves;
import feastcraft.expeditions.expedition.Expedition;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Waves from the mod's own overworld plan, fought at noon so that only ignoring light lets them spawn. */
@GameTestHolder(Expeditions.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExfilWaveTests {
    private static final BlockPos BEACON = new BlockPos(12, 1, 12);
    private static final BlockPos IN_ZONE = new BlockPos(14, 1, 12);
    private static final int SHORT_COUNTDOWN = 20;
    /** Long enough that only the first wave arrives during a test. */
    private static final int LONG_COUNTDOWN = 2_000;
    private static final long NOON = 6_000;

    private ExfilWaveTests() {
    }

    @GameTest(template = "arena", batch = "waves_spawn", timeoutTicks = 100)
    public static void aRaiderAtTheBeaconIsAttackedInDaylight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        atNoon(level);
        BlockPos beacon = placeBeacon(helper);
        ServerPlayer raider = FakePlayerFactory.get(base(helper), new GameProfile(UUID.randomUUID(), "exfil_wave_raider"));
        Expedition.deploy(raider, helper.absolutePos(IN_ZONE));
        ExfilBeacons.ignite(level, beacon, LONG_COUNTDOWN);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertFalse(waveMobsNear(level, beacon).isEmpty(), "no wave arrived"))
                .thenExecute(() -> {
                    for (Mob mob : waveMobsNear(level, beacon)) {
                        helper.assertTrue(level.getMaxLocalRawBrightness(mob.blockPosition()) == 15,
                                "a wave mob spawned out of full daylight, at " + mob.blockPosition());
                        helper.assertTrue(mob.getTarget() == raider, "a wave mob is not after the raider");
                    }
                    ExfilBeacons.reset(level, beacon);
                    waveMobsNear(level, beacon).forEach(Entity::discard);
                    Expedition.end(raider);
                    level.removePlayerImmediately(raider, Entity.RemovalReason.DISCARDED);
                })
                .thenSucceed();
    }

    @GameTest(template = "arena", batch = "waves_departure", timeoutTicks = 200)
    public static void waveMobsInTheZoneStayBehind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel base = base(helper);
        fuelBase(base, PLENTY_OF_FUEL);
        BlockPos beacon = placeBeacon(helper);
        Cow cow = still(helper, EntityType.COW, IN_ZONE);
        Zombie attacker = still(helper, EntityType.ZOMBIE, IN_ZONE);
        ExfilWaves.tagAsWave(attacker, ExfilBeacons.key(level, beacon));
        ExfilBeacons.ignite(level, beacon, SHORT_COUNTDOWN);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.getEntity(cow.getUUID()) != null, "the cow has not arrived home"))
                .thenExecute(() -> {
                    helper.assertTrue(attacker.isAlive() && attacker.level() == level, "the wave mob rode home");
                    attacker.discard();
                    base.getEntity(cow.getUUID()).discard();
                    fuelBase(base, 0);
                })
                .thenSucceed();
    }

    @GameTest(template = "arena", batch = "waves_sun", timeoutTicks = 600)
    public static void waveUndeadBurnOnlyOnceTheirCountdownEnds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        atNoon(level);
        BlockPos beacon = placeBeacon(helper);
        Zombie zombie = still(helper, EntityType.ZOMBIE, IN_ZONE);
        ExfilWaves.tagAsWave(zombie, ExfilBeacons.key(level, beacon));
        ExfilBeacons.ignite(level, beacon, LONG_COUNTDOWN);

        helper.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    helper.assertFalse(zombie.isOnFire(), "a wave zombie burned while its countdown ran");
                    ExfilBeacons.reset(level, beacon);
                })
                .thenWaitUntil(() -> helper.assertTrue(zombie.isOnFire(), "the zombie has not caught fire"))
                .thenExecute(zombie::discard)
                .thenSucceed();
    }

    private static void atNoon(ServerLevel level) {
        level.setDayTime(NOON);
        level.setWeatherParameters(LONG_COUNTDOWN, 0, false, false);
    }

    private static List<Mob> waveMobsNear(ServerLevel level, BlockPos beacon) {
        return level.getEntitiesOfClass(Mob.class, new AABB(beacon).inflate(48), ExfilWaves::isWaveMob);
    }

    private static BlockPos placeBeacon(GameTestHelper helper) {
        return TestFixtures.placeBeacon(helper, BEACON);
    }
}
