package feastcraft.thaumcraftaspects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import feastcraft.thaumcraftaspects.AspectEntry.Aspects;
import feastcraft.thaumcraftaspects.AspectEntry.Kind;
import feastcraft.thaumcraftaspects.AspectEntry.Parent;
import feastcraft.thaumcraftaspects.AspectEntry.Source;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Parses one JSON entry into an {@link AspectEntry}; see the README for the format. Throws
 * {@link JsonParseException} on a malformed one.
 */
public final class AspectFiles {
    private AspectFiles() {
    }

    public static AspectEntry entry(JsonObject json) {
        List<Kind> kinds = List.of(Kind.values()).stream().filter(kind -> json.has(key(kind))).toList();
        if (kinds.size() != 1) {
            throw new JsonParseException("an entry names exactly one of item, block or entity");
        }
        Kind kind = kinds.get(0);
        return new AspectEntry(kind, id(GsonHelper.getAsString(json, key(kind))), source(json));
    }

    private static Source source(JsonObject json) {
        boolean parent = json.has("parent");
        if (parent == json.has("aspects")) {
            throw new JsonParseException("an entry has either a parent or aspects");
        }
        if (parent) {
            return new Parent(id(GsonHelper.getAsString(json, "parent")));
        }
        Map<String, Integer> amounts = new HashMap<>();
        for (Map.Entry<String, JsonElement> aspect : GsonHelper.getAsJsonObject(json, "aspects").entrySet()) {
            amounts.put(aspect.getKey(), GsonHelper.convertToInt(aspect.getValue(), aspect.getKey()));
        }
        return new Aspects(amounts);
    }

    private static String key(Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation id(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new JsonParseException(id + " is not a valid id");
        }
        return parsed;
    }
}
