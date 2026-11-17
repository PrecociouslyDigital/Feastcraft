package feastcraft.expeditions.mixin;

import feastcraft.expeditions.base.BaseStasis;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidState.class)
abstract class FluidStateMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void feastcraft$stopRandomTicksInFrozenBase(Level level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (BaseStasis.isFrozen(level, pos)) {
            callback.cancel();
        }
    }
}
