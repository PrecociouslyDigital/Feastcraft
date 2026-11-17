package feastcraft.advancementpoints.rules;

import java.util.List;

/** Pays a skill category for each completed advancement matching any of the patterns and none of the exceptions. */
public record PointRule(String category, List<AdvancementPattern> advancements, List<AdvancementPattern> except, FramePoints points) {
    public PointRule {
        advancements = List.copyOf(advancements);
        except = List.copyOf(except);
    }

    public int worth(Earned earned) {
        return covers(earned.id()) ? points.of(earned.frame()) : 0;
    }

    private boolean covers(String advancement) {
        return advancements.stream().anyMatch(pattern -> pattern.matches(advancement))
                && except.stream().noneMatch(pattern -> pattern.matches(advancement));
    }
}
