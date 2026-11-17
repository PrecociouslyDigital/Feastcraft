package feastcraft.smithy.convert;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import feastcraft.smithy.Smithy;

/**
 * Converts items joining the level, which covers mob drops, block drops and
 * dispensers, plus the gear a mob spawns wearing.
 */
public final class JoinConversions {
    private JoinConversions() {
    }

    /** Every item entity entering the world, so a dropped plain tool arrives as a Tinkers tool. */
    public static void onItemJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        item.setItem(Smithy.convert(item.getItem()));
    }

    /**
     * A mob's gear on spawn. Ranged weapons stay vanilla: skeleton and pillager AI
     * checks `Items.BOW` and `Items.CROSSBOW` exactly, and their drops are
     * converted by the item entity hook instead.
     */
    public static void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)
                || event.getEntity() instanceof ArmorStand || !(event.getEntity() instanceof Enemy)) {
            return;
        }
        convertHand(mob, EquipmentSlot.MAINHAND);
        convertHand(mob, EquipmentSlot.OFFHAND);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                mob.setItemSlot(slot, Smithy.convert(mob.getItemBySlot(slot)));
            }
        }
    }

    private static void convertHand(Mob mob, EquipmentSlot slot) {
        ItemStack held = mob.getItemBySlot(slot);
        Item item = held.getItem();
        if (item == Items.BOW || item == Items.CROSSBOW) {
            return;
        }
        mob.setItemSlot(slot, Smithy.convert(held));
    }
}
