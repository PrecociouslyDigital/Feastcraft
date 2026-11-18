package feastcraft.expeditions.expedition;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.MinecraftServer;

/**
 * Keeps the overworld on hard so starvation is possible and the hunger ramp matters. Re-asserts at
 * startup and periodically, in case someone runs /difficulty.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class DifficultyGuard {
    private static final int REASSERT_TICKS = 100;

    private DifficultyGuard() {
    }

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        enforce(event.getServer());
    }

    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % REASSERT_TICKS == 0) {
            enforce(event.getServer());
        }
    }

    private static void enforce(MinecraftServer server) {
        if (ExpeditionsConfig.THREAT_ENABLED.get() && server.overworld().getDifficulty() != Difficulty.HARD) {
            server.setDifficulty(Difficulty.HARD, true);
        }
    }
}
