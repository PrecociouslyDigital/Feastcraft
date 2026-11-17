package feastcraft.datapackexport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** {@code items.tsv}: every registered item, for a food its hunger points and saturation modifier, and its English name. */
final class ItemList {
    private ItemList() {
    }

    static void write(Path output) {
        try {
            Files.createDirectories(output);
            Files.write(output.resolve("items.tsv"), ForgeRegistries.ITEMS.getEntries().stream()
                    .map(entry -> entry.getKey().location() + "\t" + food(entry.getValue())
                            + "\t" + name(entry.getValue()))
                    .sorted()
                    .toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String name(Item item) {
        try {
            return new ItemStack(item).getHoverName().getString();
        } catch (RuntimeException e) {
            // Bountiful's bounty reads its own NBT for a name and throws without any.
            return item.getDescription().getString();
        }
    }

    private static String food(Item item) {
        FoodProperties food = item.getFoodProperties();
        return food == null ? "\t" : food.getNutrition() + "\t" + food.getSaturationModifier();
    }
}
