package feastcraft.expeditions.exfil;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * When each wave of a countdown arrives. A wave at fraction {@code at} of the countdown arrives on
 * the tick where {@code floor(at * duration)} ticks have elapsed, so it arrives exactly once.
 */
final class WaveSchedule {
    private WaveSchedule() {
    }

    /** The waves arriving on the tick with {@code ticksLeft} to go, earliest {@code at} first. */
    static <W> List<W> due(List<W> waves, ToDoubleFunction<W> at, int duration, int ticksLeft) {
        int elapsed = duration - ticksLeft;
        return waves.stream()
                .filter(wave -> (int) Math.floor(at.applyAsDouble(wave) * duration) == elapsed)
                .sorted(Comparator.comparingDouble(at))
                .toList();
    }
}
