package feastcraft.expeditions.mixin;

import feastcraft.expeditions.exfil.ExfilWaves;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
abstract class MobMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void feastcraft$sunProofExfilWaves(CallbackInfoReturnable<Boolean> cir) {
        if (ExfilWaves.isSunProof((Mob) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
