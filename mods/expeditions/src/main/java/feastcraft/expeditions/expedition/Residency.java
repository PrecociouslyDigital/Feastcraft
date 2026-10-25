package feastcraft.expeditions.expedition;

/**
 * The rules behind the invariant everything else relies on: a bound player is in the base exactly
 * when they have no active raid.
 */
public final class Residency {
    public enum Correction { NONE, SEND_HOME, SEND_TO_DROP }

    private Residency() {
    }

    /** Entering the base requires having no raid; leaving it requires having one. */
    public static boolean mayTravel(boolean hasRaid, boolean fromBase, boolean toBase) {
        if (toBase && !fromBase) {
            return !hasRaid;
        }
        if (fromBase && !toBase) {
            return hasRaid;
        }
        return true;
    }

    /** What to do with a player who has arrived somewhere without traveling, such as a respawn or a login. */
    public static Correction reconcile(boolean hasRaid, boolean inBase) {
        if (hasRaid && inBase) {
            return Correction.SEND_TO_DROP;
        }
        if (!hasRaid && !inBase) {
            return Correction.SEND_HOME;
        }
        return Correction.NONE;
    }
}
