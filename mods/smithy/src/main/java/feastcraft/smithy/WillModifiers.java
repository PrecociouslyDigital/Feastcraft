package feastcraft.smithy;

import java.util.Arrays;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import vazkii.botania.api.item.AncientWillContainer.AncientWillType;

/**
 * The six ancient wills as Tinkers modifiers. `hasTerrestrialSet` reads the
 * armor a player wears, the rest a single stack, so both Botania and the
 * smithy's own effects can ask the same questions.
 */
public final class WillModifiers {
    public static final ModifierId TERRESTRIAL = new ModifierId("tcintegrations", "terrestrial");

    private WillModifiers() {
    }

    /** Every armor slot carries `tcintegrations:terrestrial`, which is the Terrestrial set. */
    public static boolean hasTerrestrialSet(Player player) {
        return Arrays.stream(EquipmentSlot.values())
                .filter(slot -> slot.getType() == EquipmentSlot.Type.ARMOR)
                .allMatch(slot -> level(player.getItemBySlot(slot), TERRESTRIAL) > 0);
    }

    /** The stack is a Tinkers helmet with `will_<type>`, Botania's will helmet logic applies. */
    public static boolean hasWill(ItemStack stack, AncientWillType type) {
        return level(stack, will(type)) > 0;
    }

    /** A Tinkers helmet wearing any will, which is what Botania's instanceof checks want. */
    public static boolean isWillHelmet(ItemStack stack) {
        return Arrays.stream(AncientWillType.values())
                .anyMatch(type -> hasWill(stack, type));
    }

    public static ModifierId will(AncientWillType type) {
        return new ModifierId(Smithy.MOD_ID, "will_" + type.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static int level(ItemStack stack, ModifierId modifier) {
        if (stack.isEmpty() || ToolStack.from(stack).isBroken()) {
            return 0;
        }
        return ToolStack.from(stack).getUpgrades().getLevel(modifier);
    }
}
