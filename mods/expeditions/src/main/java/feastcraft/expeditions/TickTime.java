package feastcraft.expeditions;

public final class TickTime {
    private TickTime() {
    }

    /** Game ticks as a clock reading, {@code m:ss} or {@code h:mm:ss}, rounded up to the second. */
    public static String format(long ticks) {
        long seconds = (ticks + 19) / 20;
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds % 60)
                : String.format("%d:%02d", minutes, seconds % 60);
    }
}
