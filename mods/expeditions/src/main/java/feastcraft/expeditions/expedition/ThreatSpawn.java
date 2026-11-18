package feastcraft.expeditions.expedition;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hostiles spawn at the current threat level, taken from the oldest active raid. Stats snapshot at
 * spawn: a zombie that arrived at 3x stays a 3x zombie as the threat climbs.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class ThreatSpawn {
    private static final UUID HEALTH_ID = UUID.fromString("9a6f2e64-7a01-4b8e-9d31-111111111111");
    private static final UUID DAMAGE_ID = UUID.fromString("9a6f2e64-7a01-4b8e-9d31-222222222222");
    private static final UUID ARMOR_ID = UUID.fromString("9a6f2e64-7a01-4b8e-9d31-333333333333");
    private static final UUID SPEED_ID = UUID.fromString("9a6f2e64-7a01-4b8e-9d31-444444444444");

    /** Entity data key recording the multiplier this mob spawned under, for XP scaling. */
    private static final String XP_THREAT = Expeditions.MOD_ID + ":threat";

    private ThreatSpawn() {
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!ExpeditionsConfig.THREAT_ENABLED.get() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || level != level.getServer().overworld()) {
            return;
        }
        if (!(event.getEntity() instanceof Enemy enemy)) {
            return;
        }
        // A mob carrying the modifier is a re-add (chunk reload, portal travel), not a fresh spawn.
        AttributeInstance health = ((LivingEntity) enemy).getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH_ID) != null) {
            return;
        }

        LivingEntity living = (LivingEntity) enemy;
        double threat = ThreatCurve.threat(RaidLedger.get(level.getServer()).peakElapsedTicks(level.getServer()));
        living.getPersistentData().putDouble(XP_THREAT, threat);

        scale(living.getAttribute(Attributes.MAX_HEALTH), threat, HEALTH_ID);
        scale(living.getAttribute(Attributes.ATTACK_DAMAGE), threat, DAMAGE_ID);
        scale(living.getAttribute(Attributes.ARMOR), threat, ARMOR_ID);
        if (threat > ThreatCurve.HARD_THREAT_AT) {
            // Speed only past the hard anchor: up to 15% faster at the extreme.
            double speedFactor = 1 + 0.15 * (threat - ThreatCurve.HARD_THREAT_AT) / (ThreatCurve.EXTREME_THREAT_AT - ThreatCurve.HARD_THREAT_AT);
            scale(living.getAttribute(Attributes.MOVEMENT_SPEED), speedFactor, SPEED_ID);
        }
        living.setHealth(living.getMaxHealth());
    }

    private static void scale(AttributeInstance attribute, double factor, UUID id) {
        if (attribute == null || factor == 1) {
            return;
        }
        attribute.addPermanentModifier(new AttributeModifier(id, Expeditions.MOD_ID + ":threat", factor - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @SubscribeEvent
    static void onXpDrop(LivingExperienceDropEvent event) {
        if (!ExpeditionsConfig.THREAT_ENABLED.get() || event.getEntity().level().isClientSide()) {
            return;
        }
        double threat = event.getEntity().getPersistentData().getDouble(XP_THREAT);
        if (threat != 0) {
            event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * threat));
        }
    }
}
