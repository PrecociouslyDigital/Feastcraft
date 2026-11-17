package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Whether a chunk of the base is out of fuel. Each hearth powers only its own chunk: an unfueled
 * chunk still lets players build, craft and store, while the stasis mixins stop only its random
 * ticks (crops) and block entity ticks (machines). Block entity types in the {@code ticks_in_stasis}
 * tag are exempt.
 *
 * <p>Not yet stopped: baby animals still age, bonemeal still works, and machines driven by entities
 * or by their own global schedulers (AE2 grids, for example) keep running.
 */
public final class BaseStasis {
    public static final TagKey<BlockEntityType<?>> TICKS_IN_STASIS = TagKey.create(Registries.BLOCK_ENTITY_TYPE, Expeditions.id("ticks_in_stasis"));

    private BaseStasis() {
    }

    public static boolean freezesBlockEntity(Level level, BlockPos pos) {
        if (!isFrozen(level, pos)) {
            return false;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null || !BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(entity.getType()).is(TICKS_IN_STASIS);
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
