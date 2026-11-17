package feastcraft.arsenal.spear;

import java.util.Optional;

/** What a lowered spear does to a target it meets, given how long it's been held and how fast they're closing. */
public final class Charge {
    static final double DAMAGE_SPEED = 4.6;
    static final double KNOCKBACK_SPEED = 5.1;

    private Charge() {
    }

    /** Where a held charge is in its life: lowering, then engaged, tired, disengaged, and spent. Each weakens what a hit does. */
    public enum Stage {
        LOWERING,
        ENGAGED,
        TIRED,
        DISENGAGED,
        SPENT;

        boolean damages() {
            return this == ENGAGED || this == TIRED || this == DISENGAGED;
        }

        boolean knocksBack() {
            return this == ENGAGED || this == TIRED;
        }

        boolean dismounts() {
            return this == ENGAGED;
        }
    }

    public record Impact(float damage, boolean knockback, boolean dismount) {
    }

    /** The stage a charge is in after being held this many ticks. */
    public static Stage stage(ChargeTier tier, int ticksHeld) {
        int left = ticksHeld - tier.loweringTicks;
        if (left < 0) {
            return Stage.LOWERING;
        }
        if ((left -= tier.engagedTicks) < 0) {
            return Stage.ENGAGED;
        }
        if ((left -= tier.tiredTicks) < 0) {
            return Stage.TIRED;
        }
        return left < tier.disengagedTicks ? Stage.DISENGAGED : Stage.SPENT;
    }

    /** What a hit does, or nothing if the closing speed is too low for this stage to damage. */
    public static Optional<Impact> impact(ChargeTier tier, Stage stage, double closingSpeed) {
        if (!stage.damages() || closingSpeed < DAMAGE_SPEED) {
            return Optional.empty();
        }
        return Optional.of(new Impact(
                (float) (tier.damagePerSpeed * closingSpeed),
                stage.knocksBack() && closingSpeed >= KNOCKBACK_SPEED,
                stage.dismounts() && closingSpeed >= tier.dismountSpeed));
    }

    /** About half a block at the knockback threshold, growing with speed. */
    public static float knockbackStrength(double closingSpeed) {
        return (float) Math.min(closingSpeed / 10.0, 2.0);
    }
}
