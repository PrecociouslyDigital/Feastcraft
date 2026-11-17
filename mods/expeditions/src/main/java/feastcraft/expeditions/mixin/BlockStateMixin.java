package feastcraft.expeditions.mixin;

import feastcraft.expeditions.base.BaseStasis;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateMixin {
    // Gated here instead of at the randomTick call in ServerLevel.tickChunk, because Radium (also in
    // the pack) redirects that call and two redirects of one call can't both apply.
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void feastcraft$stopRandomTicksInFrozenBase(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (BaseStasis.isFrozen(level, pos)) {
            callback.cancel();
        }
    }
}
