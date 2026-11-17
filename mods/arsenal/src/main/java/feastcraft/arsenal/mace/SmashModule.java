package feastcraft.arsenal.mace;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** Falling onto a target with the mace adds damage for the height and throws back whatever stands around it. */
public enum SmashModule implements ModifierModule, MeleeDamageModifierHook, MeleeHitModifierHook {
    INSTANCE;

    public static final RecordLoadable<SmashModule> LOADER = new SingletonLoader<>(INSTANCE);
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<SmashModule>defaultHooks(ModifierHooks.MELEE_DAMAGE, ModifierHooks.MELEE_HIT);
    private static final double SHOCKWAVE_LIFT = 0.7;

    @Override
    public RecordLoadable<SmashModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    private static boolean smashing(ToolAttackContext context) {
        LivingEntity attacker = context.getAttacker();
        return !context.isExtraAttack()
                && !context.isProjectile()
                && !attacker.isFallFlying()
                && !attacker.hasEffect(MobEffects.SLOW_FALLING)
                && Smash.smashes(attacker.fallDistance);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        return smashing(context) ? damage + Smash.bonusDamage(context.getAttacker().fallDistance) : damage;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!smashing(context) || !(context.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity attacker = context.getAttacker();
        Entity target = context.getTarget();
        float fallDistance = attacker.fallDistance;
        shockwave(level, attacker, target, fallDistance);

        attacker.fallDistance = 0;
        attacker.setDeltaMovement(attacker.getDeltaMovement().multiply(1, 0, 1));
        if (attacker instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        BlockPos ground = target.blockPosition().below();
        BlockState groundState = level.getBlockState(ground);
        if (!groundState.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                    target.getX(), target.getY(), target.getZ(), Smash.heavy(fallDistance) ? 80 : 40, 1, 0.1, 1, 0.15);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                1, Smash.heavy(fallDistance) ? 0.5F : 0.8F);
    }

    private static void shockwave(ServerLevel level, LivingEntity attacker, Entity target, float fallDistance) {
        level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(Smash.SHOCKWAVE_RADIUS), nearby -> nearby != attacker
                        && nearby != target
                        && !nearby.isSpectator()
                        && !attacker.isAlliedTo(nearby)
                        && !(nearby instanceof OwnableEntity pet && pet.getOwner() == attacker)
                        && nearby.distanceTo(target) <= Smash.SHOCKWAVE_RADIUS)
                .forEach(nearby -> {
                    Vec3 away = nearby.position().subtract(target.position()).multiply(1, 0, 1).normalize();
                    double power = Smash.shockwavePower(nearby.distanceTo(target), fallDistance, nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                    if (power > 0) {
                        nearby.push(away.x * power, SHOCKWAVE_LIFT, away.z * power);
                        nearby.hurtMarked = true;
                    }
                });
    }
}
