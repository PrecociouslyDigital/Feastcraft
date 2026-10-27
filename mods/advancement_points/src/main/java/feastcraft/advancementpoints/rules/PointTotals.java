package feastcraft.advancementpoints.rules;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PointTotals {
    private PointTotals() {
    }

    /**
     * Each category's points from these advancements: the sum over every rule for it, so rules from
     * different data packs add up, capped at {@link Integer#MAX_VALUE}. Every category with a rule
     * is present, at zero if nothing pays, so that losing an advancement takes its points back.
     */
    public static Map<String, Integer> of(List<PointRule> rules, Collection<Earned> earned) {
        var totals = new LinkedHashMap<String, Long>();
        for (PointRule rule : rules) {
            long paid = earned.stream().mapToLong(rule::worth).sum();
            totals.merge(rule.category(), paid, Long::sum);
        }
        var clamped = new LinkedHashMap<String, Integer>();
        totals.forEach((category, points) -> clamped.put(category, (int) Math.min(points, Integer.MAX_VALUE)));
        return clamped;
    }
}
