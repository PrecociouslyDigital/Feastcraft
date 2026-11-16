package feastcraft.expeditions.gametest;

import com.mojang.authlib.GameProfile;
import feastcraft.expeditions.Expeditions;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Expeditions.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SleepTests {
    private static final long MIDNIGHT = 18_000;
    /** Vanilla skips the night once everyone has lain in bed for 100 ticks. */
    private static final int LONG_ENOUGH_TO_SKIP = 150;

    private SleepTests() {
    }

    @GameTest(template = "empty", batch = "sleep", timeoutTicks = 300)
    public static void sleepingRestsThePlayerButTheNightGoesOn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bed = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(bed, Blocks.RED_BED.defaultBlockState());
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "sleep_test_sleeper"));
        // A creative player can sleep with monsters nearby.
        player.setGameMode(GameType.CREATIVE);
        player.setPos(bed.getX() + 0.5, bed.getY(), bed.getZ() + 1.5);
        level.addNewPlayer(player);
        player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), 100_000);
        long night = level.getDayTime() - level.getDayTime() % 24_000 + MIDNIGHT;
        level.setDayTime(night);

        helper.startSequence()
                // The level only notices it has become dark on its next tick.
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(player.startSleepInBed(bed).right().isPresent(), "the player could not get into bed");
                    helper.assertTrue(player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) == 0,
                            "lying down did not reset the phantom timer");
                    // Nothing ticks a fake player, and only a ticking player falls deeper asleep.
                    helper.onEachTick(player::doTick);
                })
                .thenIdle(LONG_ENOUGH_TO_SKIP)
                .thenExecute(() -> {
                    helper.assertTrue(level.getDayTime() - night < 1_000, "sleeping skipped the night");
                    helper.assertTrue(player.isSleepingLongEnough(), "the player never fell deeply asleep");
                    level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                })
                .thenSucceed();
    }
}
