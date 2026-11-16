package feastcraft.expeditions.gametest;

import feastcraft.expeditions.ExpeditionsRegistry;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.base.FuelTank;
import feastcraft.expeditions.base.HearthBlockEntity;
import feastcraft.expeditions.base.HearthIndex;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

final class TestFixtures {
    static final long PLENTY_OF_FUEL = 1_000_000;

    /** A standing spot for the fixture hearth that non-stasis tests fuel without caring where. */
    private static final BlockPos FUEL_HEARTH_POS = new BlockPos(8, 100, 8);

    private TestFixtures() {
    }

    static ServerLevel base(GameTestHelper helper) {
        return BaseDimension.level(helper.getLevel().getServer());
    }

    /** Cools every burning hearth, so each test starts from full stasis. */
    static void unfuelAll(ServerLevel base) {
        for (ChunkPos chunk : HearthIndex.fueledChunks()) {
            for (BlockEntity entity : List.copyOf(base.getChunk(chunk.x, chunk.z).getBlockEntities().values())) {
                if (entity instanceof HearthBlockEntity hearth) {
                    hearth.setFuel(FuelTank.EMPTY);
                }
            }
        }
    }

    /** Places a hearth and fills its own tank; the fuel transition wakes its chunk. */
    static void placeHearth(ServerLevel base, BlockPos pos, long ticks) {
        base.setBlockAndUpdate(pos, ExpeditionsRegistry.HEARTH.get().defaultBlockState());
        ((HearthBlockEntity) base.getBlockEntity(pos)).setFuel(new FuelTank(ticks));
    }

    /** The old shared-tank behavior for tests that do not care which hearth burns: fuel or freeze. */
    static void fuelBase(ServerLevel base, long ticks) {
        if (ticks > 0) {
            placeHearth(base, FUEL_HEARTH_POS, ticks);
        } else {
            unfuelAll(base);
        }
    }
}
