package feastcraft.arsenal;

import feastcraft.arsenal.spear.ChargeModule;
import feastcraft.arsenal.weapon.WeaponClass;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

public final class ArsenalRegistry {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Arsenal.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Arsenal.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Arsenal.MOD_ID);

    private static final Map<WeaponClass, RegistryObject<Attribute>> WEAPON_CLASS_DAMAGE = new EnumMap<>(WeaponClass.class);

    static {
        Arrays.stream(WeaponClass.values()).forEach(weapon ->
                WEAPON_CLASS_DAMAGE.put(weapon, ATTRIBUTES.register(weapon.attributeId(), weapon::createAttribute)));
    }

    public static final ToolDefinition SPEAR_DEFINITION = ToolDefinition.create(Arsenal.id("spear"));
    public static final RegistryObject<ModifiableItem> SPEAR = ITEMS.register("spear",
            () -> new ModifiableItem(new Item.Properties().stacksTo(1), SPEAR_DEFINITION));


    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.feastcraft_arsenal"))
                .icon(() -> new ItemStack(SPEAR.get()))
                .displayItems((parameters, output) -> ToolBuildHandler.addVariants(output::accept, SPEAR.get(), ""))
                .build());
    }

    private ArsenalRegistry() {
    }

    /** The registered damage attribute for a weapon class. */
    public static Attribute attribute(WeaponClass weapon) {
        return WEAPON_CLASS_DAMAGE.get(weapon).get();
    }

    static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener((EntityAttributeModificationEvent event) ->
                WEAPON_CLASS_DAMAGE.values().forEach(attribute -> event.add(EntityType.PLAYER, attribute.get())));
        // Tinkers registers its own modules at this point, which is before any modifier JSON loads.
        modBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {
                ModifierModule.LOADER.register(Arsenal.id("charge"), ChargeModule.LOADER);
            }
        });
    }
}
