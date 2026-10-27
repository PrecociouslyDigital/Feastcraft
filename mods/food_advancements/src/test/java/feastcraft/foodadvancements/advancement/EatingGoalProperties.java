package feastcraft.foodadvancements.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.util.HashSet;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

class EatingGoalProperties {
    @Provide
    Arbitrary<EatingGoal> goal() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(1, Integer.MAX_VALUE).map(EatingGoal.AtLeast::new),
                Arbitraries.just(new EatingGoal.Every()));
    }

    @Property
    void jsonRoundTrips(@ForAll("goal") EatingGoal goal) {
        assertEquals(goal, EatingGoal.fromJson(goal.toJson()));
    }

    @Property
    void atLeastIsMetByEnoughFoodsWhateverTheMenu(@ForAll @IntRange(min = 1, max = 20) int foods,
            @ForAll @Size(max = 30) Set<@IntRange(max = 40) Integer> eaten, @ForAll Set<@IntRange(max = 40) Integer> menu) {
        assertEquals(eaten.size() >= foods, new EatingGoal.AtLeast(foods).isMet(eaten, menu));
    }

    @Property
    void everyIsMetExactlyByEatingAllOfANonEmptyMenu(@ForAll Set<@IntRange(max = 20) Integer> eaten, @ForAll Set<@IntRange(max = 20) Integer> menu) {
        var fromMenu = new HashSet<>(eaten);
        fromMenu.retainAll(menu);
        assertEquals(!menu.isEmpty() && fromMenu.equals(menu), new EatingGoal.Every().isMet(fromMenu, menu));
    }

    @Property
    void anEmptyMenuIsNeverFinished(@ForAll Set<Integer> eaten) {
        assertFalse(new EatingGoal.Every().isMet(eaten, Set.of()));
    }

    @Property
    void malformedCountsAreRejected(@ForAll @IntRange(min = -1000, max = 0) int notPositive) {
        assertThrows(JsonParseException.class, () -> EatingGoal.fromJson(new JsonPrimitive(notPositive)));
        assertThrows(JsonParseException.class, () -> EatingGoal.fromJson(new JsonPrimitive(notPositive + 0.5)));
        assertThrows(JsonParseException.class, () -> EatingGoal.fromJson(new JsonPrimitive("most")));
    }
}
