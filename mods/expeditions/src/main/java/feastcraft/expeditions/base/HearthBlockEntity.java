package feastcraft.expeditions.base;

import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.ExpeditionsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One hearth's own fuel tank. A hearth keeps exactly its own chunk running: while fueled it holds a
 * ticking ticket on its chunk, and when the tank empties the chunk falls back into stasis. Fuel is
 * drained one tick per game tick by {@link #serverTick}, which only runs while the chunk itself runs.
 */
public final class HearthBlockEntity extends BlockEntity {
    private FuelTank tank = FuelTank.EMPTY;

    /** True while {@link HearthIndex} counts this fueled hearth; guards double unload handling. */
    boolean indexed;

    public HearthBlockEntity(BlockPos pos, BlockState state) {
        super(ExpeditionsRegistry.HEARTH_ENTITY.get(), pos, state);
    }

    public boolean fueled() {
        return tank.running();
    }

    public FuelTank tank() {
        return tank;
    }

    /** The chunk this hearth powers. */
    public net.minecraft.world.level.ChunkPos chunkPos() {
        return new net.minecraft.world.level.ChunkPos(worldPosition);
    }

    /** Adds fuel, waking the hearth's chunk if it was cold. */
    public void addFuel(long ticks) {
        setFuel(tank.add(ticks));
    }

    /** Replaces the fuel, waking or freezing the chunk to match. */
    public void setFuel(FuelTank newTank) {
        boolean wasFueled = fueled();
        tank = newTank;
        setChanged();
        sync();
        if (fueled() != wasFueled && ExpeditionsConfig.EMBER_ENABLED.get() && level instanceof ServerLevel server) {
            if (fueled()) {
                HearthIndex.onLit(server, this);
            } else {
                HearthIndex.onDied(server, this);
            }
        }
    }

    /** Marks the hearth as present in {@link HearthIndex} and, if fueled, tickets its chunk. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            HearthIndex.onLoaded(server, this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            HearthIndex.onUnloaded(server, this);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel server) {
            HearthIndex.onUnloaded(server, this);
        }
        super.onChunkUnloaded();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HearthBlockEntity hearth) {
        if (!ExpeditionsConfig.EMBER_ENABLED.get() || !hearth.fueled()) {
            return;
        }
        hearth.tank = hearth.tank.drain(1);
        hearth.setChanged();
        if (!hearth.fueled() && level instanceof ServerLevel server) {
            HearthIndex.onDied(server, hearth);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("ticksRemaining", tank.ticksRemaining());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank = new FuelTank(Math.max(0, tag.getLong("ticksRemaining")));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
