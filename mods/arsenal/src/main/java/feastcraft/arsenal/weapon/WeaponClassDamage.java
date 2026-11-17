package feastcraft.arsenal.weapon;

import feastcraft.arsenal.Arsenal;
import feastcraft.arsenal.ArsenalRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Scales a player's hits by the attribute of the weapon class that dealt them. */
public final class WeaponClassDamage {
    public static final TagKey<EntityType<?>> THROWN_WEAPONS = TagKey.create(Registries.ENTITY_TYPE, Arsenal.id("thrown_weapons"));
    private static final List<WeaponClass> HELD = Arrays.stream(WeaponClass.values())
            .filter(weapon -> weapon.delivery == WeaponClass.Delivery.HELD)
            .toList();

    private WeaponClassDamage() {
    }

    /** The weapon class behind a hit, or empty if it is not one a player scales. */
    public static Optional<WeaponClass> classify(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == null) {
            return Optional.empty();
        }
        if (direct.getType().is(THROWN_WEAPONS)) {
            return Optional.of(WeaponClass.THROWN);
        }
        if (direct instanceof AbstractArrow arrow) {
            return Optional.of(arrow.shotFromCrossbow() ? WeaponClass.CROSSBOW : WeaponClass.BOW);
        }
        if (source.is(DamageTypes.PLAYER_ATTACK) && direct instanceof Player player) {
            return HELD.stream().filter(weapon -> player.getMainHandItem().is(weapon.heldTag())).findFirst();
        }
        return Optional.empty();
    }

    /** Multiplies the damage of a player's classified hit by that class's attribute. */
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        classify(event.getSource()).ifPresent(weapon ->
                event.setAmount(event.getAmount() * (float) player.getAttributeValue(ArsenalRegistry.attribute(weapon))));
    }
}
