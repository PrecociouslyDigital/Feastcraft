package feastcraft.foodadvancements.data;

import feastcraft.foodadvancements.FoodAdvancements;
import feastcraft.foodadvancements.advancement.EatingGoal;
import feastcraft.foodadvancements.advancement.FoodsEatenTrigger;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;

/**
 * The food advancements. Every one but the root is hidden until earned.
 * Milestones stop at 40 because vanilla has 40 edible foods; a modpack with more adds its own.
 */
final class EatingAdvancements implements ForgeAdvancementProvider.AdvancementGenerator {
    /** One advancement. Its title and description double as the English translations. */
    record Entry(String name, Optional<String> parent, ItemLike icon, FrameType frame, EatingGoal goal,
            Optional<TagKey<Item>> menu, String title, String description) {
        String key(String part) {
            return "advancements." + FoodAdvancements.MOD_ID + "." + name + "." + part;
        }
    }

    static final String ROOT = "root";

    static final List<Entry> ENTRIES = List.of(
            root(),
            milestone("eaten_10", ROOT, Items.BREAD, FrameType.TASK, 10, "Curious Palate"),
            milestone("eaten_20", "eaten_10", Items.BAKED_POTATO, FrameType.GOAL, 20, "Adventurous Eater"),
            milestone("eaten_30", "eaten_20", Items.PUMPKIN_PIE, FrameType.GOAL, 30, "Connoisseur"),
            milestone("eaten_40", "eaten_30", Items.GLOW_BERRIES, FrameType.CHALLENGE, 40, "Omnivore"),
            new Entry("every_food", Optional.of("eaten_40"), Items.ENCHANTED_GOLDEN_APPLE, FrameType.CHALLENGE,
                    new EatingGoal.Every(), Optional.empty(), "The Whole Menu", "Eat every food there is"),
            menu("every_soup", Items.RABBIT_STEW, FrameType.TASK, Menus.SOUPS, "Soup of the Day", "Eat every soup and stew"),
            menu("every_cooked_meat", Items.COOKED_PORKCHOP, FrameType.TASK, Menus.COOKED_MEATS, "Grill Master", "Eat every cooked meat"),
            menu("every_sweet", Items.COOKIE, FrameType.TASK, Menus.SWEETS, "Sweet Tooth", "Eat every sweet"),
            menu("every_fish", Items.TROPICAL_FISH, FrameType.GOAL, ItemTags.FISHES, "Catch of the Day", "Eat every kind of fish"),
            menu("every_golden_food", Items.GOLDEN_APPLE, FrameType.GOAL, Menus.GOLDEN_FOODS, "Gilded Gourmet", "Eat every golden food"),
            menu("every_risky_food", Items.SPIDER_EYE, FrameType.GOAL, Menus.RISKY_FOODS, "Iron Stomach", "Eat every food that bites back"));

    private static Entry root() {
        return new Entry(ROOT, Optional.empty(), Items.GOLDEN_CARROT, FrameType.TASK, new EatingGoal.AtLeast(1),
                Optional.empty(), "Spice of Life", "Eat something. Anything.");
    }

    private static Entry milestone(String name, String parent, ItemLike icon, FrameType frame, int foods, String title) {
        return new Entry(name, Optional.of(parent), icon, frame, new EatingGoal.AtLeast(foods), Optional.empty(),
                title, "Eat " + foods + " different foods");
    }

    private static Entry menu(String name, ItemLike icon, FrameType frame, TagKey<Item> menu, String title, String description) {
        return new Entry(name, Optional.of(ROOT), icon, frame, new EatingGoal.Every(), Optional.of(menu), title, description);
    }

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<Advancement> saver, ExistingFileHelper existingFiles) {
        var saved = new HashMap<String, Advancement>();
        for (Entry entry : ENTRIES) {
            boolean root = entry.parent().isEmpty();
            ItemPredicate menu = entry.menu().map(tag -> ItemPredicate.Builder.item().of(tag).build()).orElse(ItemPredicate.ANY);
            Advancement.Builder builder = Advancement.Builder.advancement()
                    .display(entry.icon(), Component.translatable(entry.key("title")), Component.translatable(entry.key("description")),
                            root ? ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/husbandry.png") : null,
                            entry.frame(), !root, !root, !root)
                    .addCriterion("eaten", FoodsEatenTrigger.Instance.eaten(entry.goal(), menu));
            entry.parent().map(saved::get).ifPresent(builder::parent);
            saved.put(entry.name(), builder.save(saver, FoodAdvancements.id(entry.name()), existingFiles));
        }
    }
}
