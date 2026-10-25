package feastcraft.expeditions.mixin;

import feastcraft.expeditions.base.BaseStasis;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
abstract class LevelMixin {
    // Gating the per-ticker check, rather than cancelling the whole method, still lets Forge run
    // onLoad for freshly placed block entities at the start of tickBlockEntities.
    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;shouldTickBlocksAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean feastcraft$stopBlockEntitiesInFrozenBase(Level level, BlockPos pos) {
        return !BaseStasis.isFrozen(level) && level.shouldTickBlocksAt(pos);
    }
}
