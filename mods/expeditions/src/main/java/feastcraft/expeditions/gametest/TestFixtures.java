package feastcraft.expeditions.gametest;

import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.base.BaseFuel;
import feastcraft.expeditions.base.FuelTank;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

final class TestFixtures {
    static final long PLENTY_OF_FUEL = 1_000_000;

    private TestFixtures() {
    }

    static ServerLevel base(GameTestHelper helper) {
        return BaseDimension.level(helper.getLevel().getServer());
    }

    static void setFuel(ServerLevel base, long ticks) {
        BaseFuel.get(base).update(base, tank -> new FuelTank(ticks));
    }
}
