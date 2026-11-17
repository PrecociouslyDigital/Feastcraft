package feastcraft.arsenal.motion;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

/**
 * Entity velocities in blocks per tick. The server never learns a player's real velocity, since their
 * client moves them, so players get the distance they (or what they ride) moved last tick.
 */
public final class Motion {
    private static final Map<Player, Track> PLAYERS = new WeakHashMap<>();

    private Motion() {
    }

    private static final class Track {
        Vec3 last;
        Vec3 velocity = Vec3.ZERO;

        Track(Vec3 position) {
            last = position;
        }
    }

    /** Blocks moved in the last tick. */
    public static Vec3 velocity(Entity entity) {
        if (entity instanceof Player player) {
            Track track = PLAYERS.get(player);
            return track == null ? Vec3.ZERO : track.velocity;
        }
        return entity.getRootVehicle().getDeltaMovement();
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            Vec3 position = player.getRootVehicle().position();
            Track track = PLAYERS.computeIfAbsent(player, p -> new Track(position));
            track.velocity = position.subtract(track.last);
            track.last = position;
        });
    }
}
