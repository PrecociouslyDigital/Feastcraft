package feastcraft.arsenal.mace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;

class SmashProperties {
    @Example
    void bonusMatchesVanillasExamples() {
        assertEquals(6, Smash.bonusDamage(1.5));
        assertEquals(12, Smash.bonusDamage(3));
        assertEquals(22, Smash.bonusDamage(8));
        assertEquals(34, Smash.bonusDamage(20));
    }

    @Property
    void higherFallsHitHarder(@ForAll @DoubleRange(min = 0, max = 200) double fall, @ForAll @DoubleRange(min = 0, max = 50) double more) {
        assertTrue(Smash.bonusDamage(fall + more) >= Smash.bonusDamage(fall));
    }

    @Property
    void shockwaveWeakensWithDistance(@ForAll @DoubleRange(min = 0, max = 2.5) double near, @ForAll @DoubleRange(min = 0, max = 1) double further, @ForAll @DoubleRange(min = 1.5, max = 50) double fall) {
        assertTrue(Smash.shockwavePower(near + further, fall, 0) <= Smash.shockwavePower(near, fall, 0));
    }
}
