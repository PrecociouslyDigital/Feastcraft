package feastcraft.expeditions.expedition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

/** The ramp is monotonic, anchored at hard and extreme, and never dips below easy levels. */
class ThreatCurveProperties {
    private static final long HARD = ThreatCurve.HARD_TICKS;
    private static final long EXTREME = ThreatCurve.EXTREME_TICKS;

    @Property
    void threatIsMonotonic(@ForAll @LongRange(min = 0, max = 500_000) long a, @ForAll @LongRange(min = 0, max = 500_000) long b) {
        double atA = ThreatCurve.threat(a);
        double atB = ThreatCurve.threat(b);
        if (a < b) {
            assertTrue(atA <= atB, () -> "threat fell from " + atA + " to " + atB + " as a raid got longer");
        }
    }

    @Property
    void hungerIsMonotonic(@ForAll @LongRange(min = 0, max = 500_000) long a, @ForAll @LongRange(min = 0, max = 500_000) long b) {
        double atA = ThreatCurve.hunger(a);
        double atB = ThreatCurve.hunger(b);
        if (a < b) {
            assertTrue(atA <= atB, () -> "hunger fell from " + atA + " to " + atB + " as a raid got longer");
        }
    }

    @Property
    void threatNeverBelowEasyLevels(@ForAll @LongRange(min = 0, max = 500_000) long ticks) {
        assertTrue(ThreatCurve.threat(ticks) >= 0.7, () -> "threat dropped below easy at " + ticks + " ticks");
    }

    @Property
    void hungerNeverBelowOne(@ForAll @LongRange(min = 0, max = 500_000) long ticks) {
        assertTrue(ThreatCurve.hunger(ticks) >= 1, () -> "hunger multiplier dropped below one at " + ticks + " ticks");
    }

    @Property
    void threatClimbsHarderThanHungerAfterHard(@ForAll @LongRange(min = (int) HARD, max = (int) EXTREME) long ticks) {
        // Both segments are linear; between the anchors the threat slope is exactly twice the hunger slope.
        double threatGain = ThreatCurve.threat(ticks) - ThreatCurve.threat(HARD);
        double hungerGain = ThreatCurve.hunger(ticks) - ThreatCurve.hunger(HARD);
        assertEquals(threatGain, 2 * hungerGain, 1e-9, "slope ratio broke between the anchors");
    }

    @Property
    void curvesAreExactlyLinearBetweenAnchors(@ForAll @LongRange(min = 0, max = (int) HARD) long ticks) {
        double threatFraction = (double) ticks / HARD;
        assertEquals(0.7 + (5 - 0.7) * threatFraction, ThreatCurve.threat(ticks), 1e-9);
        assertEquals(1 + (2.5 - 1) * threatFraction, ThreatCurve.hunger(ticks), 1e-9);
    }

    @Property
    void curvesKeepClimbingBeyondExtreme(@ForAll @LongRange(min = (int) EXTREME, max = 500_000) long ticks) {
        assertTrue(ThreatCurve.threat(ticks) >= 10, "threat fell below the extreme anchor past ninety minutes");
        assertTrue(ThreatCurve.hunger(ticks) >= 5, "hunger fell below the extreme anchor past ninety minutes");
    }
}
