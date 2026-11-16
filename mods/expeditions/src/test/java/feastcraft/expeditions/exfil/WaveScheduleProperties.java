package feastcraft.expeditions.exfil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

class WaveScheduleProperties {
    /** Waves are indices into the list of their {@code at}s, so equal {@code at}s stay distinct waves. */
    @Property
    void aCountdownSendsEveryWaveOnceInOrder(
            @ForAll @IntRange(min = 1, max = 3_000) int duration,
            @ForAll @Size(max = 12) List<@DoubleRange(min = 0, max = 1, maxIncluded = false) Double> ats) {
        List<Integer> waves = new ArrayList<>();
        for (int i = 0; i < ats.size(); i++) {
            waves.add(i);
        }
        ToDoubleFunction<Integer> at = ats::get;

        List<Integer> sent = new ArrayList<>();
        for (int ticksLeft = duration; ticksLeft >= 1; ticksLeft--) {
            sent.addAll(WaveSchedule.due(waves, at, duration, ticksLeft));
        }

        List<Integer> expected = new ArrayList<>(waves);
        expected.sort(Comparator.comparingDouble(at));
        assertEquals(expected, sent);
    }
}
