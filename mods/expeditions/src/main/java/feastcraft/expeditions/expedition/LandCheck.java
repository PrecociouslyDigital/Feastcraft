package feastcraft.expeditions.expedition;

import feastcraft.expeditions.Expeditions;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** Asks the world generator whether a column is dry land, without generating any chunk. */
public final class LandCheck {
    public static final TagKey<Biome> UNSAFE_DROP = TagKey.create(Registries.BIOME, Expeditions.id("unsafe_drop"));

    private LandCheck() {
    }

    public static boolean isDryLand(ServerLevel level, int x, int z) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
        return floor >= generator.getSeaLevel()
                && !generator.getBiomeSource()
                        .getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(floor), QuartPos.fromBlock(z), random.sampler())
                        .is(UNSAFE_DROP);
    }
}
