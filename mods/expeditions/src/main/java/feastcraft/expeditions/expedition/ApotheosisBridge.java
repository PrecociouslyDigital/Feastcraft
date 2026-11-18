package feastcraft.expeditions.expedition;

import dev.shadowsoffire.apotheosis.adventure.AdventureConfig;
import feastcraft.expeditions.ExpeditionsConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Scales Apotheosis's natural-spawn affix chance by the threat multiplier, so affixed mobs get
 * likelier as raids go on. Only registered when Apotheosis is loaded, so this class (and its
 * Apotheosis imports) never loads otherwise.
 */
public final class ApotheosisBridge {
    private static final int TICKS = 100;
    public static final String APOTHEOSIS = "apotheosis";

    private static boolean capturedDefault;
    private static float defaultChance;

    private ApotheosisBridge() {
    }

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        captureDefault();
    }

    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % TICKS != 0) {
            return;
        }
        if (!ExpeditionsConfig.THREAT_ENABLED.get()) {
            return;
        }
        captureDefault();
        MinecraftServer server = event.getServer();
        double threat = ThreatCurve.threat(RaidLedger.get(server).peakElapsedTicks(server));
        AdventureConfig.randomAffixItem = (float) Math.min(1, defaultChance * threat);
    }

    private static void captureDefault() {
        if (!capturedDefault) {
            capturedDefault = true;
            defaultChance = AdventureConfig.randomAffixItem;
        }
    }
}
