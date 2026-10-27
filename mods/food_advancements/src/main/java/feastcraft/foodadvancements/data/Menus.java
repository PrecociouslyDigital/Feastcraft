package feastcraft.foodadvancements.data;

import feastcraft.foodadvancements.FoodAdvancements;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * The menus behind the food advancements, as item tags. A modpack puts its own foods on a menu by
 * adding them to its tag. Whether something counts as a food at all is Spice of Life: Carrot Edition's call.
 */
final class Menus extends ItemTagsProvider {
    static final TagKey<Item> SOUPS = ItemTags.create(FoodAdvancements.id("soups"));
    static final TagKey<Item> COOKED_MEATS = ItemTags.create(FoodAdvancements.id("cooked_meats"));
    static final TagKey<Item> SWEETS = ItemTags.create(FoodAdvancements.id("sweets"));
    static final TagKey<Item> GOLDEN_FOODS = ItemTags.create(FoodAdvancements.id("golden_foods"));
    static final TagKey<Item> RISKY_FOODS = ItemTags.create(FoodAdvancements.id("risky_foods"));

    Menus(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFiles) {
        super(output, registries, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), FoodAdvancements.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(SOUPS).add(Items.MUSHROOM_STEW, Items.RABBIT_STEW, Items.BEETROOT_SOUP, Items.SUSPICIOUS_STEW);
        tag(COOKED_MEATS).add(Items.COOKED_PORKCHOP, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_MUTTON,
                Items.COOKED_RABBIT, Items.COOKED_COD, Items.COOKED_SALMON);
        tag(SWEETS).add(Items.COOKIE, Items.PUMPKIN_PIE, Items.HONEY_BOTTLE);
        tag(GOLDEN_FOODS).add(Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
        tag(RISKY_FOODS).add(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.CHICKEN);
    }
}
