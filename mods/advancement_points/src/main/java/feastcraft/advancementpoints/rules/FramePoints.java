package feastcraft.advancementpoints.rules;

/** What an advancement is worth, by its frame. */
public record FramePoints(int task, int goal, int challenge) {
    public FramePoints {
        if (task < 0 || goal < 0 || challenge < 0) {
            throw new IllegalArgumentException("points are never negative, got " + task + "/" + goal + "/" + challenge);
        }
    }

    public static FramePoints uniform(int points) {
        return new FramePoints(points, points, points);
    }

    public int of(Frame frame) {
        return switch (frame) {
            case TASK -> task;
            case GOAL -> goal;
            case CHALLENGE -> challenge;
        };
    }
}
