package feastcraft.foodadvancements.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * How much of a menu a player must have eaten. The menu is whichever foods an advancement asks about.
 * In JSON it is a number of foods, or {@code "all"}.
 */
public sealed interface EatingGoal {
    String ALL = "all";

    <T> boolean isMet(Set<T> eatenFromMenu, Set<T> menu);

    JsonElement toJson();

    /** A missing goal asks for one food. */
    static EatingGoal fromJson(@Nullable JsonElement json) {
        if (json == null) {
            return new AtLeast(1);
        }
        if (!json.isJsonPrimitive()) {
            throw new JsonParseException("count is a number or \"" + ALL + "\", got " + json);
        }
        JsonPrimitive value = json.getAsJsonPrimitive();
        if (value.isString() && ALL.equals(value.getAsString())) {
            return new Every();
        }
        if (!value.isNumber() || value.getAsDouble() != value.getAsInt()) {
            throw new JsonParseException("count is a whole number or \"" + ALL + "\", got " + json);
        }
        try {
            return new AtLeast(value.getAsInt());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e.getMessage());
        }
    }

    record AtLeast(int foods) implements EatingGoal {
        public AtLeast {
            if (foods < 1) {
                throw new IllegalArgumentException("a goal needs at least one food, got " + foods);
            }
        }

        @Override
        public <T> boolean isMet(Set<T> eatenFromMenu, Set<T> menu) {
            return eatenFromMenu.size() >= foods;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(foods);
        }
    }

    /** Every food on the menu. An empty menu, such as a tag nothing has joined yet, is never finished. */
    record Every() implements EatingGoal {
        @Override
        public <T> boolean isMet(Set<T> eatenFromMenu, Set<T> menu) {
            return !menu.isEmpty() && eatenFromMenu.containsAll(menu);
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(ALL);
        }
    }
}
