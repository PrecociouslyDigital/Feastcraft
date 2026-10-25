package feastcraft.expeditions.expedition;

/** Where around world spawn a deployment may land: anywhere in an annulus, uniformly by area. */
public final class DropGeometry {
    public record Offset(double x, double z) {
        public double distance() {
            return Math.hypot(x, z);
        }
    }

    private DropGeometry() {
    }

    /**
     * Turns two uniform draws into an offset from the annulus center.
     *
     * @param u uniform in [0, 1), picks the distance
     * @param v uniform in [0, 1), picks the bearing
     */
    public static Offset sample(double u, double v, double minRadius, double maxRadius) {
        // Area grows with r², so drawing r² uniformly spreads drops evenly over the ring.
        double radius = Math.sqrt(minRadius * minRadius + u * (maxRadius * maxRadius - minRadius * minRadius));
        double bearing = 2 * Math.PI * v;
        return new Offset(radius * Math.cos(bearing), radius * Math.sin(bearing));
    }
}
