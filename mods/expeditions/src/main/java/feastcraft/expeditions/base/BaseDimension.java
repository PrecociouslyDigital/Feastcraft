package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

public final class BaseDimension {
    public static final ResourceKey<Level> BASE = ResourceKey.create(Registries.DIMENSION, Expeditions.id("base"));
    public static final BlockPos SPAWN = new BlockPos(0, 65, 0);

    private BaseDimension() {
    }

    public static boolean isBase(Level level) {
        return level.dimension() == BASE;
    }

    public static ServerLevel level(MinecraftServer server) {
        return Objects.requireNonNull(server.getLevel(BASE), "the base dimension is missing from the datapacks");
    }

    public static BaseBounds bounds() {
        return new BaseBounds(
                SectionPos.blockToSectionCoord(SPAWN.getX()),
                SectionPos.blockToSectionCoord(SPAWN.getZ()),
                ExpeditionsConfig.BASE_RADIUS_CHUNKS.get());
    }

    /**
     * Respawns are forced to the base spawn, so without a floor there every respawn would fall into
     * the void and die again.
     */
    public static void ensureFloor(ServerLevel base) {
        BlockPos floor = SPAWN.below();
        if (!base.getBlockState(floor).isFaceSturdy(base, floor, Direction.UP)) {
            base.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        }
    }

    /** The one way anything, player or mob, enters the base. */
    public static void arrive(Entity entity) {
        ServerLevel base = level(Objects.requireNonNull(entity.getServer()));
        ensureFloor(base);
        Vec3 spawn = Vec3.atBottomCenterOf(SPAWN);
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(base, spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
        } else if (entity.level() == base) {
            entity.teleportTo(spawn.x, spawn.y, spawn.z);
        } else {
            entity.changeDimension(base, new ToSpawn(spawn));
        }
    }

    private record ToSpawn(Vec3 spawn) implements ITeleporter {
        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(spawn, Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }
    }
}
