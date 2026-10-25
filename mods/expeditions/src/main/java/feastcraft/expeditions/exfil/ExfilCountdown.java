package feastcraft.expeditions.exfil;

/**
 * An exfil's countdown. It reaches zero after its duration, then departs on the first step whose
 * departure is not vetoed; a veto holds it at zero.
 */
public sealed interface ExfilCountdown {
    record Counting(int ticksLeft) implements ExfilCountdown {
        public Counting {
            if (ticksLeft < 1) {
                throw new IllegalArgumentException("a running countdown has at least one tick left, got " + ticksLeft);
            }
        }
    }

    record AtZero() implements ExfilCountdown {
    }

    record Departed() implements ExfilCountdown {
    }

    static ExfilCountdown start(int duration) {
        return new Counting(duration);
    }

    /** Restores a countdown saved by {@link #ticksLeft()}. */
    static ExfilCountdown resume(int ticksLeft) {
        return ticksLeft > 0 ? new Counting(ticksLeft) : new AtZero();
    }

    default int ticksLeft() {
        return this instanceof Counting counting ? counting.ticksLeft() : 0;
    }

    /** Whether the next step departs unless vetoed, so listeners must be asked before stepping. */
    default boolean isDue() {
        return this instanceof AtZero || (this instanceof Counting counting && counting.ticksLeft() == 1);
    }

    default ExfilCountdown step(boolean departureVetoed) {
        if (this instanceof Departed) {
            return this;
        }
        if (!isDue()) {
            return new Counting(ticksLeft() - 1);
        }
        return departureVetoed ? new AtZero() : new Departed();
    }
}
