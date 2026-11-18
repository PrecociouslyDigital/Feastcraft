package feastcraft.advancementpoints.rules;

/** A completed advancement, by id, frame, whether its only criterion is a vanilla item check, and whether it is a tab root. */
public record Earned(String id, Frame frame, boolean singleItem, boolean root) {
}
