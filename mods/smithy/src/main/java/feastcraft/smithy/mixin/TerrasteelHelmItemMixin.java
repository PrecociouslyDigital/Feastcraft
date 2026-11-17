package feastcraft.smithy.mixin;

import feastcraft.smithy.WillModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.item.AncientWillContainer.AncientWillType;
import vazkii.botania.common.item.equipment.armor.terrasteel.TerrasteelHelmItem;

/**
 * Ancient wills work on a full Terrestrial Tinkers set, the way Botania's
 * TerrasteelArmorItem would: the set check accepts four plated pieces carrying
 * `tcintegrations:terrestrial`, and a Tinkers helmet wearing `will_<type>`
 * counts as a helm with that will. Botania's own will effects then run unchanged.
 */
@Mixin(TerrasteelHelmItem.class)
abstract class TerrasteelHelmItemMixin {
    @Inject(method = "hasTerraArmorSet(Lnet/minecraft/world/entity/player/Player;)Z", at = @At(value = "HEAD", remap = false), cancellable = true, remap = false)
    private static void feastcraft$terrestrialSetCounts(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (WillModifiers.hasTerrestrialSet(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasAncientWill_(Lnet/minecraft/world/item/ItemStack;Lvazkii/botania/api/item/AncientWillContainer$AncientWillType;)Z", at = @At(value = "HEAD", remap = false), cancellable = true, remap = false)
    private static void feastcraft$willModifierCounts(ItemStack stack, AncientWillType type, CallbackInfoReturnable<Boolean> cir) {
        if (WillModifiers.hasWill(stack, type)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(
            method = {
                    "getCritDamageMult(Lnet/minecraft/world/entity/player/Player;)F",
                    "onEntityAttacked(Lnet/minecraft/world/damagesource/DamageSource;FLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/damagesource/DamageSource;"
            },
            at = @At(value = "INSTANCEOF", target = "Lvazkii/botania/common/item/equipment/armor/terrasteel/TerrasteelHelmItem;", remap = false),
            require = 2, remap = false)
    private static boolean feastcraft$terrestrialHelmCounts(Object stack) {
        return stack instanceof ItemStack itemStack && WillModifiers.isWillHelmet(itemStack);
    }
}
