package feastcraft.arsenal.mace;

/** Vanilla's mace numbers. Distances are in blocks. */
public final class Smash {
    static final double MIN_FALL = 1.5;
    static final double SHOCKWAVE_RADIUS = 2.5;
    private static final double HEAVY_FALL = 5;

    private Smash() {
    }

    /** Whether a fall is long enough to count. */
    public static boolean smashes(double fallDistance) {
        return fallDistance >= MIN_FALL;
    }

    /** 4 a block for the first 3, 2 a block for the next 5, then 1 a block. */
    public static float bonusDamage(double fallDistance) {
        double steep = Math.min(fallDistance, 3);
        double middle = Math.max(0, Math.min(fallDistance, 8) - 3);
        double rest = Math.max(0, fallDistance - 8);
        return (float) (4 * steep + 2 * middle + rest);
    }

    /** Falls this long hit twice as hard in the shockwave. */
    public static boolean heavy(double fallDistance) {
        return fallDistance > HEAVY_FALL;
    }

    /** How hard the shockwave throws an entity this far from the target. */
    public static double shockwavePower(double distance, double fallDistance, double knockbackResistance) {
        return (3.5 - distance) * 0.7 * (heavy(fallDistance) ? 2 : 1) * (1 - knockbackResistance);
    }
}
