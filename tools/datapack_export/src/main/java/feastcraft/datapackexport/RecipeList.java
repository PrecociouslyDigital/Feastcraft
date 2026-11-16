package feastcraft.datapackexport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@code recipes.tsv}: the recipes the game is left with after loading, each with its serializer and
 * result. Mods drop and rewrite recipes as they load, so some recipe files have no line here.
 */
final class RecipeList {
    private RecipeList() {
    }

    static void write(Path output, RecipeManager recipes, RegistryAccess registries) throws IOException {
        Files.write(output.resolve("recipes.tsv"), recipes.getRecipes().stream()
                .map(recipe -> {
                    ItemStack result = result(recipe, registries);
                    return recipe.getId() + "\t" + ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer())
                            + "\t" + (result.isEmpty() ? "\t" : ForgeRegistries.ITEMS.getKey(result.getItem()) + "\t" + result.getCount());
                })
                .sorted()
                .toList());
    }

    private static ItemStack result(Recipe<?> recipe, RegistryAccess registries) {
        try {
            return recipe.getResultItem(registries);
        } catch (RuntimeException e) {
            // Map cloning and the like have no fixed result, and some mods' recipes throw when asked.
            return ItemStack.EMPTY;
        }
    }
}
