package feastcraft.arsenal.spear;

/**
 * Vanilla's spear rows, from the wiki. Times are in ticks; speeds are in blocks per second.
 * Constructor arguments run damage per speed, dismount speed, then the lowering, engaged, tired and
 * disengaged durations.
 */
public enum ChargeTier {
    WOOD(0.7, 14, 15, 100, 100, 100),
    STONE(0.82, 13, 14, 90, 90, 95),
    IRON(0.95, 11, 12, 50, 85, 90),
    DIAMOND(1.075, 10, 10, 60, 70, 70),
    NETHERITE(1.2, 9, 8, 50, 60, 65);

    final double damagePerSpeed;
    final double dismountSpeed;
    final int loweringTicks;
    final int engagedTicks;
    final int tiredTicks;
    final int disengagedTicks;

    ChargeTier(double damagePerSpeed, double dismountSpeed, int loweringTicks, int engagedTicks, int tiredTicks, int disengagedTicks) {
        this.damagePerSpeed = damagePerSpeed;
        this.dismountSpeed = dismountSpeed;
        this.loweringTicks = loweringTicks;
        this.engagedTicks = engagedTicks;
        this.tiredTicks = tiredTicks;
        this.disengagedTicks = disengagedTicks;
    }

    /** Harvest levels: wood and gold 0, stone 1, iron 2, diamond 3, netherite 4. Modded tiers above 4 charge like netherite. */
    public static ChargeTier forHarvestLevel(int level) {
        ChargeTier[] tiers = values();
        return tiers[Math.max(0, Math.min(level, tiers.length - 1))];
    }

    /** How long the spear can be held before the charge is spent. */
    public int totalTicks() {
        return loweringTicks + engagedTicks + tiredTicks + disengagedTicks;
    }
}
