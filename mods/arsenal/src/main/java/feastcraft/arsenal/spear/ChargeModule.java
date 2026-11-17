package feastcraft.arsenal.spear;

import feastcraft.arsenal.motion.Motion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/** Hold right-click to lower the spear; whatever it meets takes damage from how fast the two of you are closing. */
public enum ChargeModule implements ModifierModule, GeneralInteractionModifierHook {
    INSTANCE;

    public static final RecordLoadable<ChargeModule> LOADER = new SingletonLoader<>(INSTANCE);
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ChargeModule>defaultHooks(ModifierHooks.GENERAL_INTERACT);
    private static final int REENGAGE_TICKS = 10;
    private static final int TICKS_PER_SECOND = 20;
    /** When each charger last had each target in reach, by entity id. */
    private static final Map<LivingEntity, Map<Integer, Long>> ENGAGED = new WeakHashMap<>();

    @Override
    public RecordLoadable<ChargeModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    private static ChargeTier tier(IToolStackView tool) {
        return ChargeTier.forHarvestLevel(tool.getStats().get(ToolStats.HARVEST_TIER).getLevel());
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return tier(tool).totalTicks();
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.isBroken() || source != InteractionSource.RIGHT_CLICK) {
            return InteractionResult.PASS;
        }
        ENGAGED.remove(player);
        GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity charger, int timeLeft) {
        if (charger.level().isClientSide || tool.isBroken()) {
            return;
        }
        ChargeTier tier = tier(tool);
        Charge.Stage stage = Charge.stage(tier, getUseDuration(tool, modifier) - timeLeft);
        if (stage == Charge.Stage.LOWERING) {
            return;
        }
        long now = charger.level().getGameTime();
        Map<Integer, Long> engaged = ENGAGED.computeIfAbsent(charger, c -> new HashMap<>());
        for (LivingEntity target : targetsInReach(charger)) {
            Long last = engaged.get(target.getId());
            if (last != null && now - last < REENGAGE_TICKS) {
                continue;
            }
            engaged.put(target.getId(), now);
            Vec3 towardTarget = target.getBoundingBox().getCenter().subtract(charger.getEyePosition()).normalize();
            double closingSpeed = Motion.velocity(charger).subtract(Motion.velocity(target)).dot(towardTarget) * TICKS_PER_SECOND;
            Charge.impact(tier, stage, closingSpeed).ifPresent(impact -> {
                if (strike(charger, target, impact, towardTarget, closingSpeed)) {
                    ToolDamageUtil.damageAnimated(tool, 1, charger, charger.getUsedItemHand());
                }
            });
        }
    }

    private static List<LivingEntity> targetsInReach(LivingEntity charger) {
        Vec3 eye = charger.getEyePosition();
        Vec3 reach = charger.getViewVector(1).scale(charger instanceof Player player ? player.getEntityReach() : 3);
        Vec3 end = eye.add(reach);
        AABB swept = charger.getBoundingBox().expandTowards(reach).inflate(1);
        return charger.level().getEntitiesOfClass(LivingEntity.class, swept, target -> target != charger
                && target.isAlive()
                && target.isPickable()
                && !charger.isPassengerOfSameVehicle(target)
                && target.getBoundingBox().inflate(target.getPickRadius()).clip(eye, end).isPresent());
    }

    private static boolean strike(LivingEntity charger, LivingEntity target, Charge.Impact impact, Vec3 towardTarget, double closingSpeed) {
        DamageSource source = charger instanceof Player player
                ? charger.damageSources().playerAttack(player)
                : charger.damageSources().mobAttack(charger);
        if (!target.hurt(source, impact.damage())) {
            return false;
        }
        if (impact.knockback()) {
            target.knockback(Charge.knockbackStrength(closingSpeed), -towardTarget.x, -towardTarget.z);
        }
        if (impact.dismount()) {
            target.stopRiding();
        }
        charger.setLastHurtMob(target);
        charger.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1, 1);
        return true;
    }
}
