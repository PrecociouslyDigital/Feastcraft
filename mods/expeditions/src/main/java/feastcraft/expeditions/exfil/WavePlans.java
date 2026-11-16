package feastcraft.expeditions.exfil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import feastcraft.expeditions.Expeditions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Every loaded {@link WavePlan}, and which one a beacon fights. */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
final class WavePlans extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Sorted by id, so a beacon's pick does not depend on the order files were read in. */
    private static List<WavePlan> plans = List.of();

    private WavePlans() {
        super(new Gson(), Expeditions.MOD_ID + "/exfil_waves");
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WavePlans());
    }

    /**
     * A weighted pick among the plans for this level's dimension, seeded by the beacon so that every
     * tick of its countdown, and every restart, picks the same plan.
     */
    static Optional<WavePlan> planFor(ServerLevel level, long beaconKey) {
        List<WavePlan> candidates = plans.stream().filter(plan -> plan.dimensions().contains(level.dimension())).toList();
        int total = candidates.stream().mapToInt(WavePlan::weight).sum();
        if (total == 0) {
            return Optional.empty();
        }
        int roll = RandomSource.create(beaconKey).nextInt(total);
        for (WavePlan plan : candidates) {
            roll -= plan.weight();
            if (roll < 0) {
                return Optional.of(plan);
            }
        }
        throw new IllegalStateException("weighted roll fell past the last plan");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        var loaded = new TreeMap<ResourceLocation, WavePlan>();
        files.forEach((id, json) -> {
            var parsed = WavePlan.CODEC.parse(JsonOps.INSTANCE, json);
            parsed.error().ifPresent(error -> LOGGER.error("Skipping exfil wave plan {}: {}", id, error.message()));
            parsed.result().ifPresent(plan -> loaded.put(id, plan));
        });
        plans = List.copyOf(loaded.values());
    }
}
