package feastcraft.foodadvancements.advancement;

import com.cazsius.solcarrot.SOLCarrotConfig;
import com.cazsius.solcarrot.api.FoodCapability;
import com.cazsius.solcarrot.api.SOLCarrotAPI;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Spice of Life: Carrot Edition's view of food. Carrot decides what counts as a food (its server
 * config's blacklist, whitelist and minimum food value), so a menu holds the foods its book lists
 * and a food the server excludes never blocks an advancement.
 */
final class CarrotFoods {
    private CarrotFoods() {
    }

    /** Every counted food matching the predicate. Configs and tags can change, so callers must not cache it. */
    static Set<Item> menu(ItemPredicate predicate) {
        return BuiltInRegistries.ITEM.stream()
                .filter(SOLCarrotConfig::shouldCount)
                .filter(item -> predicate.matches(new ItemStack(item)))
                .collect(Collectors.toUnmodifiableSet());
    }

    static Set<Item> eatenFrom(Player player, Set<Item> menu) {
        FoodCapability eaten = SOLCarrotAPI.getFoodCapability(player);
        return menu.stream().filter(eaten::hasEaten).collect(Collectors.toUnmodifiableSet());
    }
}
