package feastcraft.expeditions.exfil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The order to visit a random-spread placement's grid cells when searching outward: ring by ring,
 * where ring k is every cell at Chebyshev distance k from the start.
 */
public final class SpreadRings {
    public record Cell(int dx, int dz) {
        public int ring() {
            return Math.max(Math.abs(dx), Math.abs(dz));
        }
    }

    private SpreadRings() {
    }

    public static List<Cell> ring(int k) {
        if (k == 0) {
            return List.of(new Cell(0, 0));
        }
        var cells = new ArrayList<Cell>(8 * k);
        for (int d = -k; d <= k; d++) {
            cells.add(new Cell(d, -k));
            cells.add(new Cell(d, k));
        }
        for (int d = -k + 1; d <= k - 1; d++) {
            cells.add(new Cell(-k, d));
            cells.add(new Cell(k, d));
        }
        return cells;
    }

    public static Stream<Cell> upTo(int maxRing) {
        return IntStream.rangeClosed(0, maxRing).boxed().flatMap(k -> ring(k).stream());
    }
}
