package feastcraft.expeditions.expedition;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.base.BaseDimension;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Holds the residency invariant at every point a player can move between dimensions. */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class ResidencyEvents {
    private ResidencyEvents() {
    }

    // Forge routes every cross-dimension teleport, including commands, through this event.
    @SubscribeEvent
    static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Expedition.isBound(player)) {
            boolean fromBase = BaseDimension.isBase(player.level());
            boolean toBase = event.getDimension() == BaseDimension.BASE;
            if (!Residency.mayTravel(Expedition.hasRaid(player), fromBase, toBase)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        player.setRespawnPosition(BaseDimension.BASE, BaseDimension.SPAWN, 0, true, false);
        if (Expedition.isBound(player)) {
            reconcile(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        // Leaving the End through its exit portal is also a respawn, but the player is still raiding.
        if (!event.isEndConquered()) {
            Expedition.end(player);
        }
        BaseDimension.ensureFloor(BaseDimension.level(player.server));
        if (Expedition.isBound(player)) {
            reconcile(player);
        }
    }

    @SubscribeEvent
    static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        // The new mode is applied after this event, so the player is not yet bound while being moved.
        if (event.getNewGameMode().isSurvival() && event.getEntity() instanceof ServerPlayer player) {
            reconcile(player);
        }
    }

    @SubscribeEvent
    static void onSetSpawn(PlayerSetSpawnEvent event) {
        boolean isBaseSpawn = event.getSpawnLevel() == BaseDimension.BASE
                && BaseDimension.SPAWN.equals(event.getNewSpawn());
        if (!isBaseSpawn && event.getEntity() instanceof ServerPlayer player && Expedition.isBound(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof Level level && BaseDimension.isBase(level)) {
            event.setCanceled(true);
        }
    }

    private static void reconcile(ServerPlayer player) {
        switch (Residency.reconcile(Expedition.hasRaid(player), BaseDimension.isBase(player.level()))) {
            case NONE -> { }
            case SEND_HOME -> BaseDimension.arrive(player);
            case SEND_TO_DROP -> Expedition.resume(player);
        }
    }
}
