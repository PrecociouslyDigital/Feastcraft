package feastcraft.foodadvancements.advancement;

import feastcraft.foodadvancements.FoodAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When a player's food advancements are checked: after Carrot records a food on the eating event,
 * at login, and on reload. The login and reload checks award an advancement added later to players
 * who already qualify.
 */
@Mod.EventBusSubscriber(modid = FoodAdvancements.MOD_ID)
public final class AdvancementChecks {
    private AdvancementChecks() {
    }

    /** Lowest priority, so Carrot has recorded the food first. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FoodsEatenTrigger.INSTANCE.trigger(player);
        }
    }

    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FoodsEatenTrigger.INSTANCE.trigger(player);
        }
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            event.getPlayerList().getPlayers().forEach(FoodsEatenTrigger.INSTANCE::trigger);
        }
    }
}
