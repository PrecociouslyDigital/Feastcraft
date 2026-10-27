package feastcraft.advancementpoints;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import feastcraft.advancementpoints.rules.AdvancementPattern;
import feastcraft.advancementpoints.rules.FramePoints;
import feastcraft.advancementpoints.rules.PointRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * The rules datapacks supply in {@code data/<namespace>/feastcraft_advancement_points/rules/*.json}.
 * A file that fails to parse is logged and skipped, so one bad rule never disables the others.
 */
@Mod.EventBusSubscriber(modid = AdvancementPoints.MOD_ID)
public final class PointRules extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static List<PointRule> rules = List.of();
    private static final Set<ResourceLocation> reportedMissing = new HashSet<>();

    private PointRules() {
        super(new Gson(), AdvancementPoints.MOD_ID + "/rules");
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PointRules());
    }

    public static List<PointRule> rules() {
        return rules;
    }

    /** Warns once per reload about a rule paying a category no skill tree defines. */
    static void reportMissing(ResourceLocation category) {
        if (reportedMissing.add(category)) {
            LOGGER.warn("A point rule pays category {}, which no skill tree defines", category);
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        var loaded = new ArrayList<PointRule>();
        files.forEach((id, json) -> {
            try {
                loaded.add(parse(GsonHelper.convertToJsonObject(json, "rule")));
            } catch (RuntimeException e) {
                LOGGER.error("Skipping point rule {}: {}", id, e.getMessage());
            }
        });
        rules = List.copyOf(loaded);
        reportedMissing.clear();
    }

    private static PointRule parse(JsonObject json) {
        ResourceLocation category = ResourceLocation.tryParse(GsonHelper.getAsString(json, "category"));
        if (category == null) {
            throw new JsonParseException("category is not a valid id");
        }
        var patterns = new ArrayList<AdvancementPattern>();
        GsonHelper.getAsJsonArray(json, "advancements").forEach(pattern ->
                patterns.add(new AdvancementPattern(GsonHelper.convertToString(pattern, "advancement pattern"))));
        return new PointRule(category.toString(), patterns, points(json.get("points")));
    }

    private static FramePoints points(JsonElement json) {
        if (json == null) {
            throw new JsonParseException("missing points");
        }
        if (json.isJsonPrimitive()) {
            return FramePoints.uniform(GsonHelper.convertToInt(json, "points"));
        }
        JsonObject byFrame = GsonHelper.convertToJsonObject(json, "points");
        return new FramePoints(
                GsonHelper.getAsInt(byFrame, "task", 0),
                GsonHelper.getAsInt(byFrame, "goal", 0),
                GsonHelper.getAsInt(byFrame, "challenge", 0));
    }
}
