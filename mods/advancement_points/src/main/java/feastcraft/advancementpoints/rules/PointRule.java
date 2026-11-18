package feastcraft.advancementpoints.rules;

import java.util.List;

/**
 * Pays a skill category for each completed advancement matching any of the patterns and none of the
 * exceptions. An optional {@code itemPoints} pays the plain "have this item" advancements instead:
 * an advancement whose only criterion is a vanilla inventory check is a checklist entry, worth less.
 */
public record PointRule(String category, List<AdvancementPattern> advancements, List<AdvancementPattern> except,
        FramePoints points, Integer itemPoints) {
    public PointRule {
        advancements = List.copyOf(advancements);
        except = List.copyOf(except);
    }

    public int worth(Earned earned) {
        if (!covers(earned.id())) {
            return 0;
        }
        if (earned.singleItem() && itemPoints != null) {
            return itemPoints;
        }
        return points.of(earned.frame());
    }

    private boolean covers(String advancement) {
        return advancements.stream().anyMatch(pattern -> pattern.matches(advancement))
                && except.stream().noneMatch(pattern -> pattern.matches(advancement));
    }
}
