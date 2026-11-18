package feastcraft.foodadvancements.data;

import feastcraft.foodadvancements.FoodAdvancements;
import feastcraft.foodadvancements.advancement.EatingGoal;
import feastcraft.foodadvancements.advancement.FoodsEatenTrigger;
import java.util.ArrayList;
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
 * The food advancements. Every one but a root is hidden until earned.
 *
 * The milestone ladder runs over the pack's foods: every five foods through the front half of the
 * menu, then every twenty, then every fifty, so later milestones come slower but pay double the
 * tier before (task, goal, then challenge). Every ten milestones a board hangs the next nine
 * followers beneath it, and the boards chain along the top, which keeps the tab in rows instead of
 * one enormous column.
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

    /** How many foods the pack offers, per the datapack export's items.tsv. */
    private static final int FOODS = 2542;
    /** Milestones per board column. */
    private static final int BOARD = 10;

    private record Tier(int start, int step, int limit, FrameType frame, ItemLike icon, String title) {
    }

    private static final Tier[] TIERS = {
            new Tier(5, 5, 1270, FrameType.TASK, Items.BREAD, "Curious Palate"),
            new Tier(1290, 20, 1970, FrameType.GOAL, Items.BAKED_POTATO, "Adventurous Eater"),
            new Tier(2000, 50, 2500, FrameType.CHALLENGE, Items.PUMPKIN_PIE, "Omnivore"),
    };

    static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        out.add(root());
        out.add(menu("every_soup", Items.RABBIT_STEW, FrameType.TASK, Menus.SOUPS, "Soup of the Day", "Eat every soup and stew"));
        out.add(menu("every_cooked_meat", Items.COOKED_PORKCHOP, FrameType.TASK, Menus.COOKED_MEATS, "Grill Master", "Eat every cooked meat"));
        out.add(menu("every_sweet", Items.COOKIE, FrameType.TASK, Menus.SWEETS, "Sweet Tooth", "Eat every sweet"));
        out.add(menu("every_fish", Items.TROPICAL_FISH, FrameType.GOAL, ItemTags.FISHES, "Catch of the Day", "Eat every kind of fish"));
        out.add(menu("every_golden_food", Items.GOLDEN_APPLE, FrameType.GOAL, Menus.GOLDEN_FOODS, "Gilded Gourmet", "Eat every golden food"));
        out.add(menu("every_risky_food", Items.SPIDER_EYE, FrameType.GOAL, Menus.RISKY_FOODS, "Iron Stomach", "Eat every food that bites back"));

        record Milestone(int foods, int tier) {
        }
        List<Milestone> ladder = new ArrayList<>();
        for (int t = 0; t < TIERS.length; t++) {
            for (int foods = TIERS[t].start(); foods <= TIERS[t].limit(); foods += TIERS[t].step()) {
                ladder.add(new Milestone(foods, t));
            }
        }

        int[] perTier = new int[TIERS.length];
        String last = ROOT;
        for (int i = 0; i < ladder.size(); i++) {
            Milestone milestone = ladder.get(i);
            int tier = milestone.tier();
            String name = i % BOARD == 0 ? "board_%02d".formatted(i / BOARD + 1) : "eaten_%04d".formatted(milestone.foods());
            // A board chains to the previous board, so the columns line up; the board comes first in
            // its parent's children, which is what keeps each column starting at the top row.
            String parent = i % BOARD == 0 ? (i == 0 ? ROOT : "board_%02d".formatted(i / BOARD))
                    : "board_%02d".formatted(i / BOARD + 1);
            Tier tierOf = TIERS[tier];
            perTier[tier]++;
            out.add(new Entry(name, Optional.of(parent), tierOf.icon(), tierOf.frame(),
                    new EatingGoal.AtLeast(milestone.foods()), Optional.empty(),
                    tierOf.title() + " " + roman(perTier[tier]),
                    "Eat " + milestone.foods() + " different foods"));
            last = name;
        }
        out.add(new Entry("every_food", Optional.of(last), Items.ENCHANTED_GOLDEN_APPLE, FrameType.CHALLENGE,
                new EatingGoal.Every(), Optional.empty(), "The Whole Menu", "Eat every food there is"));
        return List.copyOf(out);
    }

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_LETTERS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private static String roman(int number) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (number >= ROMAN_VALUES[i]) {
                out.append(ROMAN_LETTERS[i]);
                number -= ROMAN_VALUES[i];
            }
        }
        return out.toString();
    }

    private static Entry root() {
        return new Entry(ROOT, Optional.empty(), Items.GOLDEN_CARROT, FrameType.TASK, new EatingGoal.AtLeast(1),
                Optional.empty(), "Spice of Life", "Eat something. Anything.");
    }

    private static Entry menu(String name, ItemLike icon, FrameType frame, TagKey<Item> menu, String title, String description) {
        return new Entry(name, Optional.of(ROOT), icon, frame, new EatingGoal.Every(), Optional.of(menu), title, description);
    }

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<Advancement> saver, ExistingFileHelper existingFiles) {
        var saved = new HashMap<String, Advancement>();
        for (Entry entry : entries()) {
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
