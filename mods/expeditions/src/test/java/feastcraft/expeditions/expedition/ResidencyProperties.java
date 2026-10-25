package feastcraft.expeditions.expedition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Size;

/**
 * A model of how ResidencyEvents and Expedition apply the residency rules. From any legal state,
 * every sequence of lifecycle steps and stray teleports keeps "in base iff no raid".
 */
class ResidencyProperties {
    enum Place { BASE, OVERWORLD, NETHER, END }

    interface Step {
        void applyTo(Player player);
    }

    record Deploy() implements Step {
        public void applyTo(Player player) {
            if (player.place == Place.BASE && !player.hasRaid) {
                player.hasRaid = true;
                player.travel(Place.OVERWORLD);
            }
        }
    }

    record Extract() implements Step {
        public void applyTo(Player player) {
            if (player.hasRaid) {
                player.hasRaid = false;
                player.travel(Place.BASE);
            }
        }
    }

    /** {@code spawnObstructed}: vanilla falls back to the overworld spawn when the forced spawn is blocked. */
    record Die(boolean spawnObstructed) implements Step {
        public void applyTo(Player player) {
            player.place = spawnObstructed ? Place.OVERWORLD : Place.BASE;
            player.hasRaid = false;
            player.reconcile();
        }
    }

    /** The End's exit portal respawns the player without ending their raid. */
    record EndExit(boolean spawnObstructed) implements Step {
        public void applyTo(Player player) {
            if (player.place == Place.END) {
                player.place = spawnObstructed ? Place.OVERWORLD : Place.BASE;
                player.reconcile();
            }
        }
    }

    record Teleport(Place to) implements Step {
        public void applyTo(Player player) {
            player.travel(to);
        }
    }

    record Login() implements Step {
        public void applyTo(Player player) {
            player.reconcile();
        }
    }

    static final class Player {
        boolean hasRaid;
        Place place;

        Player(boolean hasRaid, Place place) {
            this.hasRaid = hasRaid;
            this.place = place;
        }

        void travel(Place to) {
            if (Residency.mayTravel(hasRaid, place == Place.BASE, to == Place.BASE)) {
                place = to;
            }
        }

        void reconcile() {
            switch (Residency.reconcile(hasRaid, place == Place.BASE)) {
                case NONE -> { }
                case SEND_HOME -> travel(Place.BASE);
                case SEND_TO_DROP -> travel(Place.OVERWORLD);
            }
        }
    }

    @Provide
    Arbitrary<Player> legalStart() {
        Arbitrary<Player> atBase = Arbitraries.just(Place.BASE).map(place -> new Player(false, place));
        Arbitrary<Player> raiding = Arbitraries.of(Place.OVERWORLD, Place.NETHER, Place.END)
                .map(place -> new Player(true, place));
        return Arbitraries.oneOf(atBase, raiding);
    }

    @Provide
    Arbitrary<Step> step() {
        return Arbitraries.oneOf(
                Arbitraries.just(new Deploy()),
                Arbitraries.just(new Extract()),
                Arbitraries.of(true, false).map(Die::new),
                Arbitraries.of(true, false).map(EndExit::new),
                Arbitraries.of(Place.class).map(Teleport::new),
                Arbitraries.just(new Login()));
    }

    @Property
    void inBaseIffNoRaid(@ForAll("legalStart") Player player, @ForAll @Size(max = 40) List<@From("step") Step> steps) {
        for (Step step : steps) {
            step.applyTo(player);
            assertEquals(!player.hasRaid, player.place == Place.BASE, () -> "after " + step);
        }
    }
}
