package feastcraft.smithy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * The ledger in `tools/smithy/datapack` is what the runtime conversion reads, so
 * the datapack's files are what these properties check. The runtime pieces that
 * need Minecraft — the tool build, the tags, the hooks — are harness scenarios.
 */
class ConversionProperties {
    private static final Path SMITHY_PACK = Path.of("..", "..", "tools", "smithy", "datapack");

    private static List<String> disabled() {
        return values("data/feastcraft_smithy/tags/items/disabled.json", "values");
    }

    private static Set<String> convertedSources() {
        Set<String> sources = new HashSet<>();
        Path conversions = SMITHY_PACK.resolve("data/feastcraft_smithy/smithy/conversions");
        try (Stream<Path> files = Files.walk(conversions)) {
            files.filter(file -> file.toString().endsWith(".json")).forEach(file -> {
                String name = file.getFileName().toString();
                String namespace = file.getParent().getFileName().toString();
                sources.add(namespace + ":" + name.substring(0, name.length() - ".json".length()));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sources;
    }

    private static List<String> values(String path, String array) {
        Path file = SMITHY_PACK.resolve(path);
        assertTrue(Files.exists(file), path);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonArray values = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray(array);
            List<String> read = new ArrayList<>();
            for (JsonElement element : values) {
                read.add(element.getAsString());
            }
            return read;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Property
    void everyDisabledItemIsAConversionSource(@ForAll("disabledItems") String item) {
        assertTrue(convertedSources().contains(item), item + " is hidden but has no conversion");
    }

    @Provide
    Arbitrary<String> disabledItems() {
        return Arbitraries.of(disabled());
    }

    @Property
    void convertingTwiceEqualsConvertingOnce(@ForAll("conversions") String item) {
        // Converting produces a Tinkers tool, which is never a conversion source, so the
        // second pass finds nothing to do.
        JsonObject conversion = readConversion(item);
        String tool = conversion.get("tool").getAsString();
        assertFalse(convertedSources().contains(tool), item + " converts to " + tool + ", itself a conversion source");
        assertFalse(disabled().contains(tool), item + " converts to " + tool + ", itself disabled");
    }

    @Provide
    Arbitrary<String> conversions() {
        return Arbitraries.of(convertedSources());
    }

    @Property
    void convertingANonDisabledStackReturnsItUnchanged(@ForAll("nonDisabled") String item) {
        // No conversion, so `Smithy.convert` early-returns the stack as it came.
        assertFalse(convertedSources().contains(item), item + " unexpectedly converts");
    }

    @Provide
    Arbitrary<String> nonDisabled() {
        List<String> tools = new ArrayList<>();
        for (String source : convertedSources()) {
            String tool = readConversion(source).get("tool").getAsString();
            if (!disabled().contains(tool)) {
                tools.add(tool);
            }
        }
        return Arbitraries.of(tools);
    }

    private static JsonObject readConversion(String item) {
        int colon = item.indexOf(':');
        String namespace = item.substring(0, colon);
        String name = item.substring(colon + 1);
        Path file = SMITHY_PACK.resolve("data/feastcraft_smithy/smithy/conversions/" + namespace + "/" + name + ".json");
        assertTrue(Files.exists(file), file.toString());
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject read = JsonParser.parseReader(reader).getAsJsonObject();
            assertNotNull(read.get("tool"), item);
            assertNotNull(read.get("materials"), item);
            assertNotNull(read.get("modifiers"), item);
            return read;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
