package feastcraft.arsenal.spear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

class ChargeProperties {
    @Property
    void noDamageBelowThreshold(@ForAll ChargeTier tier, @ForAll Charge.Stage stage, @ForAll @DoubleRange(min = -50, max = 4.6, maxIncluded = false) double speed) {
        assertTrue(Charge.impact(tier, stage, speed).isEmpty());
    }

    @Property
    void damageIsLinearAboveThreshold(@ForAll ChargeTier tier, @ForAll @DoubleRange(min = 4.6, max = 50) double speed) {
        float damage = Charge.impact(tier, Charge.Stage.DISENGAGED, speed).orElseThrow().damage();
        assertEquals(tier.damagePerSpeed * speed, damage, 1e-4);
    }

    @Property
    void onlyEngagedChargesDismount(@ForAll ChargeTier tier, @ForAll Charge.Stage stage, @ForAll @DoubleRange(min = 4.6, max = 50) double speed) {
        Charge.impact(tier, stage, speed).ifPresent(impact ->
                assertEquals(stage == Charge.Stage.ENGAGED && speed >= tier.dismountSpeed, impact.dismount()));
    }

    @Property
    void stagesRunInOrder(@ForAll ChargeTier tier, @ForAll @IntRange(min = 0, max = 400) int ticks) {
        assertTrue(Charge.stage(tier, ticks).compareTo(Charge.stage(tier, ticks + 1)) <= 0);
    }

    @Property
    void chargeIsSpentAfterItsTotal(@ForAll ChargeTier tier) {
        assertEquals(Charge.Stage.DISENGAGED, Charge.stage(tier, tier.totalTicks() - 1));
        assertEquals(Charge.Stage.SPENT, Charge.stage(tier, tier.totalTicks()));
    }
}
