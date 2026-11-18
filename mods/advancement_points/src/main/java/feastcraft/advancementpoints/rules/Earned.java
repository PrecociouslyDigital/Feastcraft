package feastcraft.advancementpoints.rules;

/** A completed advancement, by id, frame, and whether its only criterion is a vanilla item check. */
public record Earned(String id, Frame frame, boolean singleItem) {
}
