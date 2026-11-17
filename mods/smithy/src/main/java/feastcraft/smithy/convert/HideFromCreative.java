package feastcraft.smithy.convert;

import feastcraft.smithy.Smithy;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/** Retired items don't show in the creative menu or its search. */
public final class HideFromCreative {
    public static final TagKey<Item> DISABLED = TagKey.create(Registries.ITEM, Smithy.id("disabled"));

    private HideFromCreative() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(HideFromCreative::onTabContents);
    }

    private static void onTabContents(BuildCreativeModeTabContentsEvent event) {
        var entries = event.getEntries().iterator();
        while (entries.hasNext()) {
            if (HideFromCreative.isDisabled(entries.next().getKey())) {
                entries.remove();
            }
        }
    }

    static boolean isDisabled(ItemStack stack) {
        return stack.is(DISABLED);
    }

    /** The tag decides; both the datapack and this mod read it. */
    public static boolean isDisabled(Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.wrapAsHolder(item).is(DISABLED);
    }
}
