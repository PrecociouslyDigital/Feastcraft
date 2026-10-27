package feastcraft.advancementpoints;

import feastcraft.advancementpoints.rules.Earned;
import feastcraft.advancementpoints.rules.Frame;
import feastcraft.advancementpoints.rules.PointTotals;
import java.util.List;
import net.minecraft.advancements.FrameType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * Keeps each player's points from advancements equal to what their completed advancements are worth.
 * Points are recomputed rather than accumulated, so revoking an advancement or changing a rule
 * leaves the player with exactly what the current rules say.
 */
@Mod.EventBusSubscriber(modid = AdvancementPoints.MOD_ID)
public final class Payout {
    /** Keeps these points apart from Pufferfish's own and from operators' commands. */
    public static final ResourceLocation SOURCE = AdvancementPoints.id("advancements");

    private Payout() {
    }

    /** Recipe unlocks are advancements too, but have no display, so they never pay. */
    public static void settle(ServerPlayer player, boolean announce) {
        List<Earned> earned = player.server.getAdvancements().getAllAdvancements().stream()
                .filter(advancement -> advancement.getDisplay() != null && player.getAdvancements().getOrStartProgress(advancement).isDone())
                .map(advancement -> new Earned(advancement.getId().toString(), frame(advancement.getDisplay().getFrame())))
                .toList();
        PointTotals.of(PointRules.rules(), earned).forEach((name, points) -> {
            ResourceLocation id = ResourceLocation.parse(name);
            SkillsAPI.getCategory(id).ifPresentOrElse(category -> {
                if (category.getPoints(player, SOURCE) == points) {
                    return;
                }
                if (announce) {
                    category.setPoints(player, SOURCE, points);
                } else {
                    category.setPointsSilently(player, SOURCE, points);
                }
            }, () -> PointRules.reportMissing(id));
        });
    }

    private static Frame frame(FrameType frame) {
        return switch (frame) {
            case TASK -> Frame.TASK;
            case GOAL -> Frame.GOAL;
            case CHALLENGE -> Frame.CHALLENGE;
        };
    }

    @SubscribeEvent
    static void onAdvancementProgress(AdvancementEvent.AdvancementProgressEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAdvancement().getDisplay() != null) {
            settle(player, true);
        }
    }

    /** After Pufferfish's Skills has loaded the player's skills. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            settle(player, false);
        }
    }

    /** A skill tree can reset its points on death. */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            settle(player, false);
        }
    }

    /**
     * A reload can change any rule or tree. Runs after Pufferfish's Skills reloads its trees. A single
     * player here is a join, which {@link #onLoggedIn} handles.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            event.getPlayerList().getPlayers().forEach(player -> settle(player, false));
        }
    }
}
