package feastcraft.expeditions.gametest;

import static feastcraft.expeditions.gametest.TestFixtures.PLENTY_OF_FUEL;
import static feastcraft.expeditions.gametest.TestFixtures.base;
import static feastcraft.expeditions.gametest.TestFixtures.setFuel;

import com.mojang.authlib.GameProfile;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.exfil.ExfilBeaconBlock;
import feastcraft.expeditions.exfil.ExfilBeacons;
import feastcraft.expeditions.exfil.ExfilCountdown;
import feastcraft.expeditions.exfil.ExfilEvent;
import feastcraft.expeditions.expedition.Expedition;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Hand-placed beacons with short countdowns. Each test resets its beacon first, since batches share a spot. */
@GameTestHolder(Expeditions.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeaconTests {
    private static final BlockPos BEACON = new BlockPos(12, 1, 12);
    private static final BlockPos IN_ZONE = new BlockPos(14, 1, 12);
    private static final BlockPos OUT_OF_ZONE = new BlockPos(1, 1, 1);
    private static final int SHORT_COUNTDOWN = 20;

    private BeaconTests() {
    }

    @GameTest(template = "arena", batch = "beacon_departure", timeoutTicks = 200)
    public static void departureCarriesTheZoneHomeAndSpendsTheBeacon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel base = base(helper);
        setFuel(base, PLENTY_OF_FUEL);
        BlockPos beacon = placeBeacon(helper);
        Cow inside = stillCow(helper, IN_ZONE);
        Cow outside = stillCow(helper, OUT_OF_ZONE);

        helper.assertTrue(ExfilBeacons.ignite(level, beacon, SHORT_COUNTDOWN), "a fresh beacon refused to ignite");
        helper.assertTrue(level.getBlockState(beacon).getValue(ExfilBeaconBlock.SPENT), "the called beacon does not look spent");
        helper.assertFalse(ExfilBeacons.ignite(level, beacon, SHORT_COUNTDOWN), "a spent beacon ignited again");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(base.getEntity(inside.getUUID()) != null, "the cow in the zone has not arrived home"))
                .thenExecute(() -> {
                    helper.assertTrue(outside.isAlive() && outside.level() == level, "the cow outside the zone left");
                    base.getEntity(inside.getUUID()).discard();
                    setFuel(base, 0);
                })
                .thenSucceed();
    }

    @GameTest(template = "arena", batch = "beacon_veto", timeoutTicks = 300)
    public static void cancellingDepartureHoldsTheCountdownAtZero(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel base = base(helper);
        setFuel(base, PLENTY_OF_FUEL);
        BlockPos beacon = placeBeacon(helper);
        Cow cow = stillCow(helper, IN_ZONE);
        HoldDeparture hold = new HoldDeparture(beacon);
        MinecraftForge.EVENT_BUS.register(hold);
        ExfilBeacons.ignite(level, beacon, SHORT_COUNTDOWN);

        helper.startSequence()
                .thenIdle(SHORT_COUNTDOWN * 3)
                .thenExecute(() -> {
                    helper.assertTrue(cow.isAlive() && cow.level() == level, "the cow left despite the veto");
                    helper.assertTrue(ExfilBeacons.countdownAt(level, beacon).orElse(null) instanceof ExfilCountdown.AtZero,
                            "the vetoed countdown is not held at zero");
                    MinecraftForge.EVENT_BUS.unregister(hold);
                })
                .thenWaitUntil(() -> helper.assertTrue(base.getEntity(cow.getUUID()) != null, "the cow did not leave once the veto lifted"))
                .thenExecute(() -> {
                    base.getEntity(cow.getUUID()).discard();
                    setFuel(base, 0);
                })
                .thenSucceed();
    }

    /**
     * A raider who deploys beside a beacon and rides it out, through the real residency checks.
     * Forge's login path needs a live network channel, so the raider is a fake player started in the base.
     */
    @GameTest(template = "arena", batch = "beacon_player", timeoutTicks = 200)
    public static void raiderInTheZoneIsExtracted(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel base = base(helper);
        BlockPos beacon = placeBeacon(helper);
        ServerPlayer player = FakePlayerFactory.get(base, new GameProfile(UUID.randomUUID(), "exfil_test_raider"));

        helper.assertTrue(ExfilBeacons.call(level, beacon, player) == ExfilBeacons.CallResult.NOT_RAIDING,
                "a player at base called extraction");

        Expedition.deploy(player, helper.absolutePos(IN_ZONE));
        helper.assertTrue(player.level() == level && Expedition.hasRaid(player), "deploying did not start a raid in the overworld");
        helper.assertTrue(player.getInventory().hasAnyMatching(stack -> stack.is(ExpeditionsRegistry.EXFIL_COMPASS.get())),
                "deploying did not hand out a compass");

        helper.assertTrue(ExfilBeacons.ignite(level, beacon, SHORT_COUNTDOWN), "a fresh beacon refused to ignite");
        helper.assertTrue(ExfilBeacons.call(level, beacon, player) == ExfilBeacons.CallResult.SPENT, "a spent beacon answered");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(BaseDimension.isBase(player.level()), "the raider has not arrived home"))
                .thenExecute(() -> {
                    helper.assertFalse(Expedition.hasRaid(player), "the raider is home but still raiding");
                    base.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                })
                .thenSucceed();
    }

    public static final class HoldDeparture {
        private final BlockPos beacon;

        HoldDeparture(BlockPos beacon) {
            this.beacon = beacon;
        }

        @SubscribeEvent
        public void onDepart(ExfilEvent.Depart event) {
            if (event.getBeacon().equals(beacon)) {
                event.setCanceled(true);
            }
        }
    }

    private static BlockPos placeBeacon(GameTestHelper helper) {
        BlockPos beacon = helper.absolutePos(BEACON);
        helper.getLevel().setBlockAndUpdate(beacon, ExpeditionsRegistry.EXFIL_BEACON.get().defaultBlockState());
        ExfilBeacons.reset(helper.getLevel(), beacon);
        return beacon;
    }

    private static Cow stillCow(GameTestHelper helper, BlockPos at) {
        Cow cow = helper.spawn(EntityType.COW, at);
        cow.setNoAi(true);
        return cow;
    }
}
