package feastcraft.arsenal;

import feastcraft.arsenal.weapon.WeaponClass;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ArsenalRegistry {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Arsenal.MOD_ID);

    private static final Map<WeaponClass, RegistryObject<Attribute>> WEAPON_CLASS_DAMAGE = new EnumMap<>(WeaponClass.class);

    static {
        Arrays.stream(WeaponClass.values()).forEach(weapon ->
                WEAPON_CLASS_DAMAGE.put(weapon, ATTRIBUTES.register(weapon.attributeId(), weapon::createAttribute)));
    }

    private ArsenalRegistry() {
    }

    /** The registered damage attribute for a weapon class. */
    public static Attribute attribute(WeaponClass weapon) {
        return WEAPON_CLASS_DAMAGE.get(weapon).get();
    }

    static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener((EntityAttributeModificationEvent event) ->
                WEAPON_CLASS_DAMAGE.values().forEach(attribute -> event.add(EntityType.PLAYER, attribute.get())));
    }
}
