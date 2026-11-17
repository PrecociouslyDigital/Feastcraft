package feastcraft.thaumcraftaspects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import feastcraft.thaumcraftaspects.AspectEntry.Kind;
import feastcraft.thaumcraftaspects.AspectEntry.Parent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Loads entries from datapacks and, when TC Modern Lib is present, registers them with it. Bad entries
 * are logged and skipped rather than failing the reload.
 */
@Mod.EventBusSubscriber(modid = ThaumcraftAspects.MOD_ID)
public final class AspectEntries extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static List<AspectEntry> entries = List.of();

    private AspectEntries() {
        super(new Gson(), ThaumcraftAspects.MOD_ID);
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AspectEntries());
    }

    /** The entries loaded by the last reload. */
    public static List<AspectEntry> entries() {
        return entries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        var loaded = new ArrayList<AspectEntry>();
        files.forEach((file, json) -> {
            try {
                GsonHelper.convertToJsonArray(json, "entries").forEach(element -> {
                    try {
                        AspectEntry entry = AspectFiles.entry(GsonHelper.convertToJsonObject(element, "entry"));
                        missing(entry).ifPresentOrElse(
                                id -> LOGGER.info("Skipping aspects for {} in {}: the game has no {}", entry.id(), file, id),
                                () -> loaded.add(entry));
                    } catch (RuntimeException e) {
                        LOGGER.error("Skipping an aspect entry in {}: {}", file, e.getMessage());
                    }
                });
            } catch (RuntimeException e) {
                LOGGER.error("Skipping aspect file {}: {}", file, e.getMessage());
            }
        });
        entries = List.copyOf(loaded);
        if (ModList.get().isLoaded("tcmodernlib")) {
            ThaumcraftScans.define(entries);
        }
    }

    private static Optional<ResourceLocation> missing(AspectEntry entry) {
        if (!registered(entry.kind(), entry.id())) {
            return Optional.of(entry.id());
        }
        if (entry.source() instanceof Parent parent && !registered(entry.kind(), parent.id())) {
            return Optional.of(parent.id());
        }
        return Optional.empty();
    }

    private static boolean registered(Kind kind, ResourceLocation id) {
        return switch (kind) {
            case ITEM -> ForgeRegistries.ITEMS.containsKey(id);
            case BLOCK -> ForgeRegistries.BLOCKS.containsKey(id);
            case ENTITY -> ForgeRegistries.ENTITY_TYPES.containsKey(id);
        };
    }
}
