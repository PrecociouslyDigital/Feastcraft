package feastcraft.expeditions.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Whether the base is out of fuel. A frozen base still lets players build, craft and store; the
 * stasis mixins stop only random ticks (crops) and block entity ticks (machines).
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
