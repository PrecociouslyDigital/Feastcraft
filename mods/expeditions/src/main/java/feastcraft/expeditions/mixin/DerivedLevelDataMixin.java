package feastcraft.expeditions.mixin;

import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.base.BaseLevelData;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.DerivedLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Makes the base peaceful so hostiles can't spawn and nobody starves at home, while the overworld
// stays hard (see DifficultyGuard). Difficulty is read from level data, and non-overworld
// dimensions each have their own DerivedLevelData, so this is the only place the base can differ.
@Mixin(DerivedLevelData.class)
abstract class DerivedLevelDataMixin implements BaseLevelData {
    @Unique
    private boolean feastcraft$isBase;

    @Override
    public void feastcraft$markAsBase() {
        feastcraft$isBase = true;
    }

    @Inject(method = "getDifficulty", at = @At("HEAD"), cancellable = true)
    private void feastcraft$baseIsPeaceful(CallbackInfoReturnable<Difficulty> cir) {
        if (feastcraft$isBase && ExpeditionsConfig.THREAT_ENABLED.get()) {
            cir.setReturnValue(Difficulty.PEACEFUL);
        }
    }
}
