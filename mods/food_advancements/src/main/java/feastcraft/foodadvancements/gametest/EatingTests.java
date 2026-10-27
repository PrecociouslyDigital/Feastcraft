package feastcraft.foodadvancements.gametest;

import com.cazsius.solcarrot.api.SOLCarrotAPI;
import com.cazsius.solcarrot.tracking.FoodList;
import feastcraft.foodadvancements.FoodAdvancements;
import feastcraft.foodadvancements.advancement.FoodsEatenTrigger;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Each test eats as a fresh player, so food lists and advancements never carry over between tests. */
@GameTestHolder(FoodAdvancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EatingTests {
    private EatingTests() {
    }

    @GameTest(template = "empty")
    public static void eatingAFoodEarnsTheRoot(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.detached(helper);
        eat(player, new ItemStack(Items.BREAD));

        helper.assertTrue(SOLCarrotAPI.getFoodCapability(player).hasEaten(Items.BREAD), "Carrot did not write the bread down");
        helper.assertTrue(earned(player, "root"), "eating bread did not earn the root advancement");
        helper.assertFalse(earned(player, "eaten_10"), "one food earned the ten-food milestone");
        leave(helper, player);
    }

    @GameTest(template = "empty")
    public static void aMenuIsEarnedByEatingAllOfItAndNoSooner(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.detached(helper);
        List<ItemStack> fishes = BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(stack -> stack.is(ItemTags.FISHES)).toList();
        helper.assertTrue(fishes.size() > 1, "the fishes tag is too small to test with");

        fishes.subList(1, fishes.size()).forEach(fish -> eat(player, fish));
        helper.assertFalse(earned(player, "every_fish"), "every fish was earned with one missing");
        eat(player, fishes.get(0));
        helper.assertTrue(earned(player, "every_fish"), "eating every fish did not earn it");
        leave(helper, player);
    }

    /** What logging in and reloading do, so foods eaten before an advancement existed still count. */
    @GameTest(template = "empty")
    public static void foodsEatenBeforeAreCountedAtTheNextCheck(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.detached(helper);
        FoodList.get(player).addFood(Items.APPLE);
        helper.assertFalse(earned(player, "root"), "the root was earned before any check");

        FoodsEatenTrigger.INSTANCE.trigger(player);
        helper.assertTrue(earned(player, "root"), "the check did not count a food Carrot already had");
        leave(helper, player);
    }

    private static void eat(ServerPlayer player, ItemStack food) {
        ForgeEventFactory.onItemUseFinish(player, food.copy(), 0, ItemStack.EMPTY);
    }

    private static boolean earned(ServerPlayer player, String advancement) {
        Advancement found = player.server.getAdvancements().getAdvancement(FoodAdvancements.id(advancement));
        return found != null && player.getAdvancements().getOrStartProgress(found).isDone();
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        player.getAdvancements().stopListening();
        helper.succeed();
    }
}
