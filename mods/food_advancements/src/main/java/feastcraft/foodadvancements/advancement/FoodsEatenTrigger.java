package feastcraft.foodadvancements.advancement;

import com.google.gson.JsonObject;
import feastcraft.foodadvancements.FoodAdvancements;
import java.util.Set;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * {@code feastcraft_food_advancements:foods_eaten}: met by a player who has eaten enough of a menu.
 * <ul>
 *   <li>{@code foods}: an item predicate choosing the menu, usually a tag. Without one, every food is on it.</li>
 *   <li>{@code count}: how many foods from the menu to have eaten, or {@code "all"} for every one. Defaults to 1.</li>
 * </ul>
 */
public final class FoodsEatenTrigger extends SimpleCriterionTrigger<FoodsEatenTrigger.Instance> {
    public static final FoodsEatenTrigger INSTANCE = new FoodsEatenTrigger();

    private static final ResourceLocation ID = FoodAdvancements.id("foods_eaten");

    private FoodsEatenTrigger() {
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(player, ItemPredicate.fromJson(json.get("foods")), EatingGoal.fromJson(json.get("count")));
    }

    public void trigger(ServerPlayer player) {
        trigger(player, instance -> instance.matches(player));
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final ItemPredicate menu;
        private final EatingGoal goal;

        public Instance(ContextAwarePredicate player, ItemPredicate menu, EatingGoal goal) {
            super(ID, player);
            this.menu = menu;
            this.goal = goal;
        }

        public static Instance eaten(EatingGoal goal, ItemPredicate menu) {
            return new Instance(ContextAwarePredicate.ANY, menu, goal);
        }

        boolean matches(ServerPlayer player) {
            Set<Item> foods = CarrotFoods.menu(menu);
            return goal.isMet(CarrotFoods.eatenFrom(player, foods), foods);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (menu != ItemPredicate.ANY) {
                json.add("foods", menu.serializeToJson());
            }
            json.add("count", goal.toJson());
            return json;
        }
    }
}
