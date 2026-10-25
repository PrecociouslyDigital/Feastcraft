package feastcraft.expeditions.exfil;

import feastcraft.expeditions.ExpeditionsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds no state; it exists so the client can draw the beacon's beam. */
public final class ExfilBeaconBlockEntity extends BlockEntity {
    public ExfilBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ExpeditionsRegistry.EXFIL_BEACON_ENTITY.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
