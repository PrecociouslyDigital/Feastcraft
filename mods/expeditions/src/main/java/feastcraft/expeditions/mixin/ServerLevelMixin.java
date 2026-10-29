package feastcraft.expeditions.mixin;

import feastcraft.expeditions.base.BaseStasis;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    // Redirecting the per-position randomTick calls (rather than zeroing the speed argument) makes
    // the gate per-chunk: a chunk random-ticks only while a fueled hearth powers it. The two calls
    // cover the same loop as the old randomTickSpeed argument, block ticks and fluid ticks alike.
    @Redirect(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void feastcraft$stopBlockRandomTicksInFrozenBase(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!BaseStasis.isFrozen(level, pos)) {
            state.randomTick(level, pos, random);
        }
    }

    @Redirect(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void feastcraft$stopFluidRandomTicksInFrozenBase(FluidState state, Level level, BlockPos pos, RandomSource random) {
        if (!BaseStasis.isFrozen(level, pos)) {
            state.randomTick(level, pos, random);
        }
    }
}
