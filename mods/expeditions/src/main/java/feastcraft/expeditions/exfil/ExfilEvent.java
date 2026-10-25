package feastcraft.expeditions.exfil;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Hooks into an exfil's life, posted on {@code MinecraftForge.EVENT_BUS} so other mods and scripts
 * can lock beacons, spawn waves during a countdown, or set conditions for leaving.
 */
public abstract class ExfilEvent extends Event {
    private final ServerLevel level;
    private final BlockPos beacon;

    protected ExfilEvent(ServerLevel level, BlockPos beacon) {
        this.level = level;
        this.beacon = beacon;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getBeacon() {
        return beacon;
    }

    /** A raider is calling extraction. Cancel to refuse the call; the beacon stays unspent. */
    @Cancelable
    public static final class Call extends ExfilEvent {
        private final ServerPlayer caller;

        public Call(ServerLevel level, BlockPos beacon, ServerPlayer caller) {
            super(level, beacon);
            this.caller = caller;
        }

        public ServerPlayer getCaller() {
            return caller;
        }
    }

    /** Posted every tick of a countdown, including while it is held at zero. */
    public static final class Countdown extends ExfilEvent {
        private final int ticksLeft;

        public Countdown(ServerLevel level, BlockPos beacon, int ticksLeft) {
            super(level, beacon);
            this.ticksLeft = ticksLeft;
        }

        public int getTicksLeft() {
            return ticksLeft;
        }
    }

    /**
     * The countdown is about to carry its passengers home. Cancel to hold it at zero until a later
     * tick's event goes uncancelled. The passenger list may be edited.
     */
    @Cancelable
    public static final class Depart extends ExfilEvent {
        private final List<Entity> passengers;

        public Depart(ServerLevel level, BlockPos beacon, List<Entity> passengers) {
            super(level, beacon);
            this.passengers = passengers;
        }

        public List<Entity> getPassengers() {
            return passengers;
        }
    }

    public static final class Departed extends ExfilEvent {
        private final List<Entity> passengers;

        public Departed(ServerLevel level, BlockPos beacon, List<Entity> passengers) {
            super(level, beacon);
            this.passengers = List.copyOf(passengers);
        }

        public List<Entity> getPassengers() {
            return passengers;
        }
    }
}
