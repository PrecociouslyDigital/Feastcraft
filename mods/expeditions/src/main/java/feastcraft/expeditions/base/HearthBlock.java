package feastcraft.expeditions.base;

import com.google.common.math.LongMath;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.TickTime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Feeds the base's fuel tank. Every hearth in the base feeds the same tank. Right-click with fuel
 * to burn one item, sneak to burn the whole stack, or use an empty hand to see what is left.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class HearthBlock extends Block {
    public HearthBlock() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 3_600_000.0F)
                .noLootTable()
                .lightLevel(state -> 13)
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.BLOCK));
    }

    // Vanilla skips a block's use() when the player sneaks with an item in hand, but sneaking is how
    // a whole stack gets burned.
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof HearthBlock) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!BaseDimension.isBase(server)) {
            player.displayClientMessage(Component.translatable("feastcraft_expeditions.hearth.not_in_base"), true);
            return InteractionResult.CONSUME;
        }

        BaseFuel fuel = BaseFuel.get(server);
        ItemStack stack = player.getItemInHand(hand);
        long ticksPerItem = FuelValues.ticksPerItem(stack);
        if (ticksPerItem > 0) {
            int burned = player.isShiftKeyDown() ? stack.getCount() : 1;
            ItemStack remainder = stack.getCraftingRemainingItem();
            fuel.update(server, tank -> tank.add(LongMath.saturatedMultiply(ticksPerItem, burned)));
            if (!player.getAbilities().instabuild) {
                stack.shrink(burned);
                for (int i = 0; i < burned && !remainder.isEmpty(); i++) {
                    player.getInventory().placeItemBackInInventory(remainder.copy());
                }
            }
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
        } else if (!stack.isEmpty()) {
            return InteractionResult.PASS;
        }

        player.displayClientMessage(fuel.tank().running()
                ? Component.translatable("feastcraft_expeditions.hearth.remaining", TickTime.format(fuel.tank().ticksRemaining()))
                : Component.translatable("feastcraft_expeditions.hearth.empty"), true);
        return InteractionResult.CONSUME;
    }
}
