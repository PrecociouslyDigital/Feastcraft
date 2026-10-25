package feastcraft.expeditions.base;

/** How long the base can keep running. The base runs exactly while the tank is not empty. */
public record FuelTank(long ticksRemaining) {
    public static final FuelTank EMPTY = new FuelTank(0);

    public FuelTank {
        if (ticksRemaining < 0) {
            throw new IllegalArgumentException("a tank cannot hold negative fuel, got " + ticksRemaining);
        }
    }

    public boolean running() {
        return ticksRemaining > 0;
    }

    public FuelTank add(long ticks) {
        requireNonNegative(ticks);
        long sum = ticksRemaining + ticks;
        return new FuelTank(sum < 0 ? Long.MAX_VALUE : sum);
    }

    public FuelTank drain(long ticks) {
        requireNonNegative(ticks);
        return new FuelTank(Math.max(0, ticksRemaining - ticks));
    }

    private static void requireNonNegative(long ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("fuel amounts are non-negative, got " + ticks);
        }
    }
}
