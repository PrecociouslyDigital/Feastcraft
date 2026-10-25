package feastcraft.expeditions.expedition;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

class DropGeometryProperties {
    @Property
    void everySampleLiesInItsAnnulus(
            @ForAll @DoubleRange(min = 0, max = 1, maxIncluded = false) double u,
            @ForAll @DoubleRange(min = 0, max = 1, maxIncluded = false) double v,
            @ForAll @IntRange(min = 0, max = 1_000_000) int minRadius,
            @ForAll @IntRange(min = 0, max = 1_000_000) int extra) {
        int maxRadius = minRadius + extra;
        double distance = DropGeometry.sample(u, v, minRadius, maxRadius).distance();
        double tolerance = 1e-9 * Math.max(1, maxRadius);
        assertTrue(distance >= minRadius - tolerance && distance <= maxRadius + tolerance,
                () -> distance + " is outside [" + minRadius + ", " + maxRadius + "]");
    }
}
