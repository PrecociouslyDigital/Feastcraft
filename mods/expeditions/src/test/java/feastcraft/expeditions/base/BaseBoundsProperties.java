package feastcraft.expeditions.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class BaseBoundsProperties {
    @Property
    void borderBoxIsExactlyTheUnionOfLoadedChunks(
            @ForAll @IntRange(min = -100_000, max = 100_000) int centerX,
            @ForAll @IntRange(min = -100_000, max = 100_000) int centerZ,
            @ForAll @IntRange(min = 0, max = 32) int radius) {
        BaseBounds bounds = new BaseBounds(centerX, centerZ, radius);
        double half = bounds.borderSize() / 2;
        int minX = (int) (bounds.borderCenterX() - half);
        int minZ = (int) (bounds.borderCenterZ() - half);
        int widthInChunks = (int) bounds.borderSize() / 16;

        var expected = new HashSet<BaseBounds.Chunk>();
        for (int dx = 0; dx < widthInChunks; dx++) {
            for (int dz = 0; dz < widthInChunks; dz++) {
                expected.add(new BaseBounds.Chunk(Math.floorDiv(minX, 16) + dx, Math.floorDiv(minZ, 16) + dz));
            }
        }

        assertEquals(0, Math.floorMod(minX, 16), "border edge must sit on a chunk boundary");
        assertEquals(0, Math.floorMod(minZ, 16), "border edge must sit on a chunk boundary");
        assertEquals(expected, new HashSet<>(bounds.chunks()));
        assertEquals(expected.size(), bounds.chunks().size(), "no chunk may be listed twice");
    }
}
