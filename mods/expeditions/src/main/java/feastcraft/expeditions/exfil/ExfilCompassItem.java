package feastcraft.expeditions.exfil;

import feastcraft.expeditions.expedition.Expedition;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Points at the nearest unspent exfil beacon. It is a view of its holder's raid: it re-targets as
 * beacons are spent and disappears once the raid is over.
 */
public final class ExfilCompassItem extends Item {
    private static final String TARGET = "Target";
    private static final int RETARGET_INTERVAL = 100;
    private static final int SEARCH_DISTANCE = 2048;

    public ExfilCompassItem() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack pointingFrom(ItemStack compass, ServerLevel level, BlockPos from) {
        retarget(compass, level, from);
        return compass;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!(holder instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!Expedition.hasRaid(player)) {
            stack.setCount(0);
        } else if ((serverLevel.getGameTime() + player.getId()) % RETARGET_INTERVAL == 0) {
            retarget(stack, serverLevel, player.blockPosition());
        }
    }

    private static void retarget(ItemStack stack, ServerLevel level, BlockPos from) {
        ExfilBeacons.nearestUnspent(level, from, SEARCH_DISTANCE).ifPresentOrElse(
                beacon -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, GlobalPos.of(level.dimension(), beacon))
                        .result()
                        .ifPresent(tag -> stack.getOrCreateTag().put(TARGET, tag)),
                () -> stack.removeTagKey(TARGET));
    }

    @Nullable
    public static GlobalPos target(ItemStack stack) {
        if (stack.getTag() == null || !stack.getTag().contains(TARGET)) {
            return null;
        }
        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, stack.getTag().get(TARGET)).result().orElse(null);
    }
}
