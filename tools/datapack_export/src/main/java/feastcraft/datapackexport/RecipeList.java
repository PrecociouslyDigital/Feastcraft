package feastcraft.datapackexport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@code recipes.tsv}: the recipes the game is left with after loading, each with its serializer,
 * result and ingredients. Mods drop and rewrite recipes as they load, so some recipe files have no
 * line here.
 */
final class RecipeList {
    private RecipeList() {
    }

    static void write(Path output, RecipeManager recipes, RegistryAccess registries) throws IOException {
        Files.write(output.resolve("recipes.tsv"), recipes.getRecipes().stream()
                .map(recipe -> {
                    ItemStack result = result(recipe, registries);
                    return recipe.getId() + "\t" + ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer())
                            + "\t" + (result.isEmpty() ? "\t" : ForgeRegistries.ITEMS.getKey(result.getItem()) + "\t" + result.getCount())
                            + "\t" + ingredients(recipe);
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

    /**
     * One ingredient per slot, space separated, each listing the items that fit it with {@code |}
     * between them: {@code minecraft:stick minecraft:oak_planks|minecraft:birch_planks}.
     */
    private static String ingredients(Recipe<?> recipe) {
        List<Ingredient> ingredients;
        try {
            ingredients = recipe.getIngredients();
        } catch (RuntimeException e) {
            return "";
        }
        return ingredients.stream()
                .map(RecipeList::alternatives)
                .filter(alternatives -> !alternatives.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private static String alternatives(Ingredient ingredient) {
        try {
            return Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty())
                    .map(stack -> stack.getCount() == 1 ? key(stack) : key(stack) + "*" + stack.getCount())
                    .distinct()
                    .collect(Collectors.joining("|"));
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String key(ItemStack stack) {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
