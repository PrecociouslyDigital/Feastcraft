package feastcraft.foodbooks.mixin;

import com.cazsius.solcarrot.client.gui.FoodBookScreen;
import feastcraft.foodbooks.Chapters;
import feastcraft.foodbooks.PageCount;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Turning past Carrot's last page opens Onion's book. Only {@code init} is Minecraft's, so only it is remapped. */
@Mixin(FoodBookScreen.class)
abstract class CarrotFoodBookScreenMixin implements PageCount {
    /** Carrot's page type is package-private. */
    @Shadow(remap = false)
    @Final
    private List<?> pages;

    @Shadow(remap = false)
    private Player player;

    @Inject(method = "isWithinRange", at = @At("HEAD"), cancellable = true, remap = false)
    private void feastcraft_food_books$onionFollows(int page, CallbackInfoReturnable<Boolean> callback) {
        if (page == pages.size()) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "switchToPage", at = @At("HEAD"), cancellable = true, remap = false)
    private void feastcraft_food_books$turnToOnion(int page, CallbackInfo callback) {
        if (page == pages.size()) {
            Chapters.openOnion(player);
            callback.cancel();
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void feastcraft_food_books$labelPageButtons(CallbackInfo callback) {
        Chapters.labelPageButtons((Screen) (Object) this);
    }

    @Override
    public int feastcraft_food_books$pageCount() {
        return pages.size();
    }
}
