package feastcraft.expeditions.exfil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

class ExfilCountdownProperties {
    @Property
    void withoutVetoesItDepartsAfterExactlyItsDuration(@ForAll @IntRange(min = 1, max = 5_000) int duration) {
        ExfilCountdown countdown = ExfilCountdown.start(duration);
        for (int step = 1; step < duration; step++) {
            countdown = countdown.step(false);
            assertFalse(countdown instanceof ExfilCountdown.Departed, "departed early at step " + step);
        }
        assertInstanceOf(ExfilCountdown.Departed.class, countdown.step(false));
    }

    /**
     * Vetoes are drawn per step, and steps past the end of the list are unvetoed. Only vetoes on
     * due steps delay departure.
     */
    @Property
    void vetoesHoldItAtZeroAndDelayDepartureByOneStepEach(
            @ForAll @IntRange(min = 1, max = 200) int duration,
            @ForAll @Size(min = 1, max = 400) List<Boolean> vetoes) {
        // Ending with no veto guarantees the countdown eventually departs.
        List<Boolean> schedule = new java.util.ArrayList<>(vetoes);
        schedule.add(false);

        ExfilCountdown countdown = ExfilCountdown.start(duration);
        int steps = 0;
        int vetoedDueSteps = 0;
        while (!(countdown instanceof ExfilCountdown.Departed)) {
            boolean vetoed = steps < schedule.size() && schedule.get(steps);
            if (countdown.isDue() && vetoed) {
                vetoedDueSteps++;
            }
            ExfilCountdown next = countdown.step(vetoed);
            if (vetoed) {
                assertFalse(next instanceof ExfilCountdown.Departed, "departed despite a veto");
            }
            countdown = next;
            steps++;
        }
        assertEquals(duration + vetoedDueSteps, steps);
    }
}
