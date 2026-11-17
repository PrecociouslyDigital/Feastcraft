package feastcraft.expeditions.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    // Vanilla reads a sleeping percentage above 100 as "the night cannot be skipped". Beds still
    // work and reset the phantom timer.
    private static final int NIGHT_CANNOT_BE_SKIPPED = 101;

    @Redirect(method = {"tick", "canSleepThroughNights"}, require = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getInt(Lnet/minecraft/world/level/GameRules$Key;)I"))
    private int feastcraft$stopSleepSkippingTheNight(GameRules rules, GameRules.Key<GameRules.IntegerValue> rule) {
        return rule == GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE ? NIGHT_CANNOT_BE_SKIPPED : rules.getInt(rule);
    }
}
