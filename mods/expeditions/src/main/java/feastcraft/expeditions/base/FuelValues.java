package feastcraft.expeditions.base;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import feastcraft.expeditions.Expeditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * What the hearth burns, loaded from {@code data/<namespace>/feastcraft_expeditions/fuel/*.json},
 * each shaped like {@code {"ingredient": <ingredient>, "ticks": <ticks per item>}}.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class FuelValues extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private record Fuel(Ingredient ingredient, long ticks) {
    }

    private static List<Fuel> fuels = List.of();

    private FuelValues() {
        super(new Gson(), Expeditions.MOD_ID + "/fuel");
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new FuelValues());
    }

    /** Ticks of fuel one item of this stack is worth, or 0 if it is not fuel. */
    public static long ticksPerItem(ItemStack stack) {
        return fuels.stream().filter(fuel -> fuel.ingredient().test(stack)).mapToLong(Fuel::ticks).findFirst().orElse(0);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        var loaded = new ArrayList<Fuel>();
        files.forEach((id, json) -> {
            try {
                var object = GsonHelper.convertToJsonObject(json, "fuel");
                long ticks = GsonHelper.getAsLong(object, "ticks");
                if (ticks <= 0) {
                    throw new JsonParseException("ticks must be positive");
                }
                loaded.add(new Fuel(Ingredient.fromJson(object.get("ingredient")), ticks));
            } catch (RuntimeException e) {
                LOGGER.error("Skipping fuel {}: {}", id, e.getMessage());
            }
        });
        fuels = List.copyOf(loaded);
    }
}
