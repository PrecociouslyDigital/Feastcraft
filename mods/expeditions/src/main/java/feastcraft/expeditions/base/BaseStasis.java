package feastcraft.expeditions.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Whether the base is out of fuel. A frozen base still lets players build, craft and store; the
 * stasis mixins stop only random ticks (crops) and block entity ticks (machines).
 *
 * <p>Not yet stopped: baby animals still age, bonemeal still works, and machines driven by entities
 * or by their own global schedulers (AE2 grids, for example) keep running.
 */
public final class BaseStasis {
    private BaseStasis() {
    }

    public static boolean isFrozen(Level level) {
        return level instanceof ServerLevel server
                && BaseDimension.isBase(server)
                && !BaseFuel.get(server).tank().running();
    }
}
