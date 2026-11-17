package feastcraft.arsenal.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

/** Nested tags such as #minecraft:swords aren't resolved here; the harness covers those. */
class WeaponClassTagProperties {
    @Test
    void everyListedWeaponBelongsToOneClass() {
        Map<String, List<WeaponClass>> classesByEntry = Arrays.stream(WeaponClass.values())
                .filter(weapon -> weapon.delivery == WeaponClass.Delivery.HELD)
                .flatMap(weapon -> entries(weapon).stream().map(entry -> Map.entry(entry, weapon)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        classesByEntry.forEach((entry, classes) -> assertEquals(1, classes.size(), entry + " is in " + classes));
    }

    private static List<String> entries(WeaponClass weapon) {
        String path = "/data/feastcraft_arsenal/tags/items/weapon_class/" + weapon.id() + ".json";
        var stream = WeaponClassTagProperties.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var values = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values");
            return StreamSupport.stream(values.spliterator(), false)
                    .map(WeaponClassTagProperties::entryId)
                    .toList();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static String entryId(JsonElement value) {
        return value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("id").getAsString();
    }
}
