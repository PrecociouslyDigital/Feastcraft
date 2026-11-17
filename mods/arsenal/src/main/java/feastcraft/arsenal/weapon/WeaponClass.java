package feastcraft.arsenal.weapon;

import feastcraft.arsenal.Arsenal;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.Item;

/**
 * The kinds of weapon the skill tree scales. A held class is the item tag
 * {@code feastcraft_arsenal:weapon_class/<name>}.
 */
public enum WeaponClass {
    BLADE(Delivery.HELD),
    AXE(Delivery.HELD),
    SPEAR(Delivery.HELD),
    MACE(Delivery.HELD),
    BOW(Delivery.PROJECTILE),
    CROSSBOW(Delivery.PROJECTILE),
    THROWN(Delivery.PROJECTILE);

    public enum Delivery {
        /** The weapon in the attacker's main hand, matched by its class tag. */
        HELD,
        /** The projectile that hit. */
        PROJECTILE
    }

    public final Delivery delivery;

    WeaponClass(Delivery delivery) {
        this.delivery = delivery;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String attributeId() {
        return id() + "_damage";
    }

    /** Only read for held classes. */
    TagKey<Item> heldTag() {
        return TagKey.create(Registries.ITEM, Arsenal.id("weapon_class/" + id()));
    }

    /** A damage multiplier where 1 is unchanged. */
    public Attribute createAttribute() {
        return new RangedAttribute("attribute.name." + Arsenal.MOD_ID + "." + attributeId(), 1.0, 0.0, 1024.0)
                .setSyncable(true);
    }
}
