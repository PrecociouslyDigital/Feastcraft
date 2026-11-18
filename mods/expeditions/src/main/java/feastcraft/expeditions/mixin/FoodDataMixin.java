package feastcraft.expeditions.mixin;

import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.expedition.Raid;
import feastcraft.expeditions.expedition.RaidLedger;
import feastcraft.expeditions.expedition.ThreatCurve;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * A raiding player's hunger drains faster as their raid goes on, by shrinking the 4-point
 * exhaustion threshold in FoodData#tick. No-op at base or with no raid.
 */
@Mixin(FoodData.class)
class FoodDataMixin {
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 4.0F))
    private float feastcraft$drainFaster(float constant, Player player) {
        if (!ExpeditionsConfig.THREAT_ENABLED.get() || !(player instanceof ServerPlayer serverPlayer)) {
            return constant;
        }
        double multiplier = multiplierOf(serverPlayer);
        return multiplier <= 1 ? constant : constant / (float) multiplier;
    }

    private static double multiplierOf(ServerPlayer player) {
        return RaidLedger.get(player.server).raidOf(player.getUUID())
                .<Double>map(raid -> ThreatCurve.hunger(player.server.overworld().getGameTime() - raid.startedAt()))
                .orElse(1.0);
    }
}
