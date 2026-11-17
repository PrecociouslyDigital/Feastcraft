package feastcraft.arsenal.spear;

import feastcraft.arsenal.Arsenal;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * A jab with a Lunge spear throws its wielder forward. A jab is any attack swing, so a miss counts:
 * the client reports those with {@link LungeSwing}, and hits arrive as {@link AttackEntityEvent}.
 */
public final class Lunge {
    public static final ModifierId ID = new ModifierId(Arsenal.id("lunge"));
    private static final double IMPULSE_PER_LEVEL = 0.458;
    private static final float EXHAUSTION_PER_LEVEL = 4;
    private static final int MIN_FOOD = 7;
    /** Game time of each player's last lunge, so one swing never lunges twice. */
    private static final Map<Player, Long> LAST_LUNGE = new WeakHashMap<>();

    private Lunge() {
    }

    /** The held tool's Lunge level, 0 if it has none. */
    public static int level(ItemStack held) {
        return ModifierUtil.getModifierLevel(held, ID);
    }

    /** A hit; misses arrive through {@link LungeSwing}. */
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide) {
            lunge(event.getEntity());
        }
    }

    static void lunge(Player player) {
        ItemStack held = player.getMainHandItem();
        int level = level(held);
        if (level == 0 || !canLunge(player)) {
            return;
        }
        ToolStack tool = ToolStack.from(held);
        if (tool.isBroken()) {
            return;
        }
        LAST_LUNGE.put(player, player.level().getGameTime());
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize().scale(IMPULSE_PER_LEVEL * level);
        player.push(forward.x, forward.y, forward.z);
        player.hurtMarked = true;
        player.causeFoodExhaustion(EXHAUSTION_PER_LEVEL * level);
        ToolDamageUtil.damageAnimated(tool, 1, player);
    }

    private static boolean canLunge(Player player) {
        Long last = LAST_LUNGE.get(player);
        boolean recovered = last == null || player.level().getGameTime() - last >= player.getCurrentItemAttackStrengthDelay();
        boolean fed = player.getAbilities().instabuild || player.getFoodData().getFoodLevel() >= MIN_FOOD;
        return recovered && fed && !player.isPassenger() && !player.isFallFlying() && !player.isInWater();
    }
}
