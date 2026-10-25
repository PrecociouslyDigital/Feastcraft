package feastcraft.expeditions.base;

import java.util.ArrayList;
import java.util.List;

/**
 * The square of chunks the base occupies. It is the single source for both the base's world border
 * and the chunks that fuel keeps loaded, so nothing can be built outside the area that runs.
 */
public record BaseBounds(int centerChunkX, int centerChunkZ, int radius) {
    public record Chunk(int x, int z) {
    }

    public BaseBounds {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be non-negative, got " + radius);
        }
    }

    public int widthInChunks() {
        return 2 * radius + 1;
    }

    public int minBlockX() {
        return (centerChunkX - radius) * 16;
    }

    public int minBlockZ() {
        return (centerChunkZ - radius) * 16;
    }

    public double borderSize() {
        return widthInChunks() * 16.0;
    }

    public double borderCenterX() {
        return minBlockX() + borderSize() / 2;
    }

    public double borderCenterZ() {
        return minBlockZ() + borderSize() / 2;
    }

    public List<Chunk> chunks() {
        var chunks = new ArrayList<Chunk>(widthInChunks() * widthInChunks());
        for (int x = centerChunkX - radius; x <= centerChunkX + radius; x++) {
            for (int z = centerChunkZ - radius; z <= centerChunkZ + radius; z++) {
                chunks.add(new Chunk(x, z));
            }
        }
        return chunks;
    }
}
