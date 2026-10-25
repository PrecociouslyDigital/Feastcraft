package feastcraft.expeditions.mixin;

import feastcraft.expeditions.base.BaseStasis;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @ModifyVariable(method = "tickChunk", at = @At("HEAD"), argsOnly = true)
    private int feastcraft$stopRandomTicksInFrozenBase(int randomTickSpeed) {
        return BaseStasis.isFrozen((ServerLevel) (Object) this) ? 0 : randomTickSpeed;
    }
}
