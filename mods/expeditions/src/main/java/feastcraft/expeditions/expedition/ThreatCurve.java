package feastcraft.expeditions.expedition;

/**
 * The difficulty ramp over elapsed raid ticks. Both curves are piecewise linear:
 *
 * <ul>
 *   <li>threat: {@code 0.7x} at deploy, {@code 5x} hard-equivalent at one hour, {@code 10x} at ninety minutes;</li>
 *   <li>hunger: {@code 1x} at deploy, {@code 2.5x} at one hour, {@code 5x} at ninety minutes.</li>
 * </ul>
 *
 * <p>Past ninety minutes the multipliers keep climbing on the last segment's slope.
 */
public final class ThreatCurve {
    /** Ticks of elapsed raid time at the hard anchor: one hour. */
    public static final long HARD_TICKS = 72_000;
    /** Ticks of elapsed raid time at the extreme anchor: ninety minutes. */
    public static final long EXTREME_TICKS = 108_000;

    public static final double EASY_THREAT_AT = 0.7;
    public static final double HARD_THREAT_AT = 5;
    public static final double EXTREME_THREAT_AT = 10;

    private static final double EASY_THREAT = EASY_THREAT_AT;
    private static final double HARD_THREAT = HARD_THREAT_AT;
    private static final double EXTREME_THREAT = EXTREME_THREAT_AT;

    private static final double EASY_HUNGER = 1;
    private static final double HARD_HUNGER = 2.5;
    private static final double EXTREME_HUNGER = 5;

    private ThreatCurve() {
    }

    /** The threat multiplier at {@code ticks} of elapsed raid time: hostile stats and mob XP scale by this. */
    public static double threat(long ticks) {
        return ramp(ticks, EASY_THREAT, HARD_THREAT, EXTREME_THREAT, HARD_TICKS, EXTREME_TICKS);
    }

    /** The hunger multiplier at {@code ticks} of elapsed raid time: a raider's food drains this much faster. */
    public static double hunger(long ticks) {
        return ramp(ticks, EASY_HUNGER, HARD_HUNGER, EXTREME_HUNGER, HARD_TICKS, EXTREME_TICKS);
    }

    private static double ramp(long ticks, double easy, double hard, double extreme, long hardTicks, long extremeTicks) {
        if (ticks <= 0) {
            return easy;
        }
        if (ticks <= hardTicks) {
            return lerp(easy, hard, ticks, hardTicks);
        }
        return lerp(hard, extreme, ticks - hardTicks, extremeTicks - hardTicks);
    }

    private static double lerp(double from, double to, long ticks, long spanTicks) {
        return from + (to - from) * ((double) ticks / spanTicks);
    }
}
