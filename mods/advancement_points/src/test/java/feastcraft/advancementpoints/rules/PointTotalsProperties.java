package feastcraft.advancementpoints.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class PointTotalsProperties {
    private static final List<String> ADVANCEMENTS = List.of(
            "minecraft:story/root", "minecraft:story/mine_stone", "minecraft:nether/root",
            "food:root", "food:tasted_10", "food:every_fish", "other:a/b");
    private static final List<String> PATTERNS = List.of(
            "*:*", "minecraft:*", "minecraft:story/*", "food:*", "food:tasted_*", "food:root", "*:root", "other:a/*");

    @Provide
    Arbitrary<Earned> earned() {
        return Combinators.combine(Arbitraries.of(ADVANCEMENTS), Arbitraries.of(Frame.class)).as(Earned::new);
    }

    @Provide
    Arbitrary<PointRule> rule() {
        Arbitrary<Integer> points = Arbitraries.integers().between(0, 50);
        return Combinators.combine(
                Arbitraries.of("pack:gourmet", "pack:combat"),
                Arbitraries.of(PATTERNS).map(AdvancementPattern::new).list().ofMaxSize(3),
                Combinators.combine(points, points, points).as(FramePoints::new)
        ).as(PointRule::new);
    }

    @Property
    void everyRuledCategoryIsPresentEvenWhenNothingPays(@ForAll List<@From("rule") PointRule> rules) {
        var totals = PointTotals.of(rules, List.of());
        rules.forEach(rule -> assertEquals(0, totals.get(rule.category())));
        assertEquals(rules.stream().map(PointRule::category).collect(java.util.stream.Collectors.toSet()), totals.keySet());
    }

    @Property
    void rulesFromSeparatePacksAddUp(@ForAll List<@From("rule") PointRule> first, @ForAll List<@From("rule") PointRule> second,
            @ForAll Set<@From("earned") Earned> earned) {
        var both = new ArrayList<>(first);
        both.addAll(second);
        var together = PointTotals.of(both, earned);
        var apart = PointTotals.of(first, earned);
        PointTotals.of(second, earned).forEach((category, points) -> apart.merge(category, points, Integer::sum));
        assertEquals(apart, together);
    }

    @Property
    void earningMoreNeverLosesPoints(@ForAll List<@From("rule") PointRule> rules, @ForAll Set<@From("earned") Earned> earned,
            @ForAll Set<@From("earned") Earned> more) {
        var all = new HashSet<>(earned);
        all.addAll(more);
        var before = PointTotals.of(rules, earned);
        PointTotals.of(rules, all).forEach((category, points) -> assertTrue(points >= before.get(category)));
    }

    @Property
    void aWildcardFreePatternMatchesOnlyItself(@ForAll("advancement") String pattern, @ForAll("advancement") String advancement) {
        assertEquals(pattern.equals(advancement), new AdvancementPattern(pattern).matches(advancement));
    }

    @Property
    void aTrailingWildcardMatchesEverythingWithThatPrefix(@ForAll("advancement") String prefixOf, @ForAll("advancement") String advancement) {
        String prefix = prefixOf.substring(0, prefixOf.indexOf(':') + 1);
        assertEquals(advancement.startsWith(prefix), new AdvancementPattern(prefix + "*").matches(advancement));
    }

    @Provide
    Arbitrary<String> advancement() {
        return Arbitraries.of(ADVANCEMENTS);
    }
}
