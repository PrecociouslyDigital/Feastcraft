package feastcraft.expeditions.base;

import feastcraft.expeditions.ExpeditionsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Whether a chunk of the base is out of fuel. Each hearth powers only its own chunk: an unfueled
 * chunk still lets players build, craft and store, while the stasis mixins stop only its random
 * ticks (crops) and block entity ticks (machines).
 *
 * <p>Not yet stopped: baby animals still age, bonemeal still works, and machines driven by entities
 * or by their own global schedulers (AE2 grids, for example) keep running.
 */
public final class BaseStasis {
    private BaseStasis() {
    }

    public static boolean isFrozen(Level level, BlockPos pos) {
        if (!ExpeditionsConfig.EMBER_ENABLED.get()) {
            return false;
        }
        return level instanceof ServerLevel server && isFrozen(server, new net.minecraft.world.level.ChunkPos(pos));
    }

    public static boolean isFrozen(ServerLevel server, LevelChunk chunk) {
        if (!ExpeditionsConfig.EMBER_ENABLED.get()) {
            return false;
        }
        return BaseDimension.isBase(server) && !HearthIndex.isFueled(chunk.getPos());
    }

    /** Chunk-pos overload for positions the caller has already converted. */
    public static boolean isFrozen(ServerLevel server, net.minecraft.world.level.ChunkPos chunk) {
        if (!ExpeditionsConfig.EMBER_ENABLED.get()) {
            return false;
        }
        return BaseDimension.isBase(server) && !HearthIndex.isFueled(chunk);
    }
}
