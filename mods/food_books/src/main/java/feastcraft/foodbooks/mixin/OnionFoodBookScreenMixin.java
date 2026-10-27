package feastcraft.foodbooks.mixin;

import feastcraft.foodbooks.Chapters;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.solonion.client.gui.screen.FoodBookScreen;

/** Turning back from Onion's first page opens Carrot's book at its last. Only {@code init} is Minecraft's, so only it is remapped. */
@Mixin(FoodBookScreen.class)
abstract class OnionFoodBookScreenMixin {
    @Shadow(remap = false)
    private Player player;

    @Inject(method = "isWithinRange", at = @At("HEAD"), cancellable = true, remap = false)
    private void feastcraft_food_books$carrotPrecedes(int page, CallbackInfoReturnable<Boolean> callback) {
        if (page == -1) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "switchToPage", at = @At("HEAD"), cancellable = true, remap = false)
    private void feastcraft_food_books$turnToCarrot(int page, CallbackInfo callback) {
        if (page == -1) {
            Chapters.openCarrotAtLastPage(player);
            callback.cancel();
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void feastcraft_food_books$labelPageButtons(CallbackInfo callback) {
        Chapters.labelPageButtons((Screen) (Object) this);
    }
}
