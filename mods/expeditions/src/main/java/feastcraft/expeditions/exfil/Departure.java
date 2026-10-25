package feastcraft.expeditions.exfil;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.expedition.Expedition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Who rides out when an exfil departs: everyone in the zone at that moment, whoever called it.
 * That means every raiding player and every mob not tagged {@code not_extractable}.
 */
final class Departure {
    static final TagKey<EntityType<?>> NOT_EXTRACTABLE = TagKey.create(Registries.ENTITY_TYPE, Expeditions.id("not_extractable"));
    static final int ZONE_RADIUS = 8;
    static final int ZONE_HALF_HEIGHT = 6;

    private Departure() {
    }

    static List<Entity> passengersAt(ServerLevel level, BlockPos beacon) {
        AABB zone = new AABB(beacon).inflate(ZONE_RADIUS, ZONE_HALF_HEIGHT, ZONE_RADIUS);
        double centerX = beacon.getX() + 0.5;
        double centerZ = beacon.getZ() + 0.5;
        return new ArrayList<>(level.getEntities((Entity) null, zone, entity ->
                Math.hypot(entity.getX() - centerX, entity.getZ() - centerZ) <= ZONE_RADIUS && rides(entity)));
    }

    private static boolean rides(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.isAlive() && Expedition.isBound(player) && Expedition.hasRaid(player);
        }
        return entity instanceof Mob mob && mob.isAlive() && !mob.getType().is(NOT_EXTRACTABLE);
    }

    /** Players go first so that leashed mobs find their holder already waiting in the base. */
    static void sendHome(List<Entity> passengers) {
        passengers.forEach(Entity::stopRiding);
        passengers.forEach(Entity::ejectPassengers);
        passengers.stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .forEach(Expedition::extract);
        passengers.stream()
                .filter(entity -> !(entity instanceof ServerPlayer) && !entity.isRemoved())
                .forEach(BaseDimension::arrive);
    }
}
