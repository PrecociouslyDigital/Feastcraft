package feastcraft.expeditions.exfil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class SpreadRingsProperties {
    @Property
    void everyCellWithinRangeComesUpExactlyOnceInRingOrder(@ForAll @IntRange(min = 0, max = 40) int maxRing) {
        List<SpreadRings.Cell> cells = SpreadRings.upTo(maxRing).toList();

        var expected = new HashSet<SpreadRings.Cell>();
        for (int dx = -maxRing; dx <= maxRing; dx++) {
            for (int dz = -maxRing; dz <= maxRing; dz++) {
                expected.add(new SpreadRings.Cell(dx, dz));
            }
        }
        assertEquals(expected, new HashSet<>(cells));
        assertEquals(expected.size(), cells.size(), "a cell came up twice");
        for (int i = 1; i < cells.size(); i++) {
            assertTrue(cells.get(i - 1).ring() <= cells.get(i).ring(), "rings went backwards at " + i);
        }
    }
}
