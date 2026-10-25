package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The bare stone platform every base starts as. It is built once, on the first server start. */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class BasePlatform extends SavedData {
    private static final String NAME = Expeditions.MOD_ID + "_platform";
    private static final int HALF_WIDTH = 4;

    private boolean built;

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        ServerLevel base = BaseDimension.level(event.getServer());
        BasePlatform platform = base.getDataStorage().computeIfAbsent(BasePlatform::load, BasePlatform::new, NAME);
        if (!platform.built) {
            build(base);
            platform.built = true;
            platform.setDirty();
        }
    }

    private static void build(ServerLevel base) {
        BlockPos center = BaseDimension.SPAWN.below();
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_WIDTH; dz <= HALF_WIDTH; dz++) {
                base.setBlockAndUpdate(center.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
            }
        }
        base.setBlockAndUpdate(BaseDimension.SPAWN.east(2), ExpeditionsRegistry.HEARTH.get().defaultBlockState());
        base.setBlockAndUpdate(BaseDimension.SPAWN.west(2), ExpeditionsRegistry.DEPARTURE_GATE.get().defaultBlockState());
    }

    private static BasePlatform load(CompoundTag tag) {
        BasePlatform platform = new BasePlatform();
        platform.built = tag.getBoolean("built");
        return platform;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("built", built);
        return tag;
    }
}
