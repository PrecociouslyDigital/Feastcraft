package feastcraft.advancementpoints.rules;

import java.util.List;

/** Pays a skill category for each completed advancement matching any of the patterns. */
public record PointRule(String category, List<AdvancementPattern> advancements, FramePoints points) {
    public PointRule {
        advancements = List.copyOf(advancements);
    }

    public int worth(Earned earned) {
        return advancements.stream().anyMatch(pattern -> pattern.matches(earned.id())) ? points.of(earned.frame()) : 0;
    }
}
