package feastcraft.expeditions.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class FuelTankProperties {
    /** Signed fuel changes: non-negative adds, negative drains its magnitude. Mixes extreme and realistic sizes. */
    @Provide
    Arbitrary<Long> change() {
        return Arbitraries.oneOf(
                Arbitraries.longs().between(-Long.MAX_VALUE, Long.MAX_VALUE),
                Arbitraries.longs().between(-50_000, 50_000));
    }

    @Property
    void neverNegativeAndRunningIffFuelRemains(@ForAll List<@From("change") Long> changes) {
        FuelTank tank = FuelTank.EMPTY;
        for (long change : changes) {
            tank = change >= 0 ? tank.add(change) : tank.drain(-change);
            assertTrue(tank.ticksRemaining() >= 0);
            assertEquals(tank.ticksRemaining() > 0, tank.running());
        }
    }

    @Property
    void addingSaturatesInsteadOfOverflowing(@ForAll("nonNegative") long held, @ForAll("nonNegative") long added) {
        long expected = held > Long.MAX_VALUE - added ? Long.MAX_VALUE : held + added;
        assertEquals(expected, new FuelTank(held).add(added).ticksRemaining());
    }

    @Provide
    Arbitrary<Long> nonNegative() {
        return Arbitraries.longs().between(0, Long.MAX_VALUE);
    }
}
