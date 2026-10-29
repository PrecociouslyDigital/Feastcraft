package feastcraft.expeditions.base;

import com.google.common.math.LongMath;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.ExpeditionsRegistry;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Feeds its own fuel tank and keeps exactly the chunk it occupies running. Right-click with fuel to
 * burn one item, sneak to burn the whole stack, or use an empty hand to see what is left. Multiple
 * hearths can share a chunk; the chunk runs while any of them is fueled.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class HearthBlock extends Block implements EntityBlock {
    public HearthBlock() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 3_600_000.0F)
                .noLootTable()
                .lightLevel(state -> 13)
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.BLOCK));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HearthBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ExpeditionsRegistry.HEARTH_ENTITY.get()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> ticker = (BlockEntityTicker<T>) (BlockEntityTicker<HearthBlockEntity>) HearthBlockEntity::serverTick;
        return ticker;
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
        if (!ExpeditionsConfig.EMBER_ENABLED.get()) {
            player.displayClientMessage(Component.translatable("feastcraft_expeditions.hearth.disabled"), true);
            return InteractionResult.CONSUME;
        }
        if (!(server.getBlockEntity(pos) instanceof HearthBlockEntity hearth)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        long ticksPerItem = FuelValues.ticksPerItem(stack);
        if (ticksPerItem > 0) {
            int burned = player.isShiftKeyDown() ? stack.getCount() : 1;
            ItemStack remainder = stack.getCraftingRemainingItem();
            hearth.addFuel(LongMath.saturatedMultiply(ticksPerItem, burned));
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

        player.displayClientMessage(hearth.fueled()
                ? Component.translatable("feastcraft_expeditions.hearth.remaining", TickTime.format(hearth.tank().ticksRemaining()))
                : Component.translatable("feastcraft_expeditions.hearth.empty"), true);
        return InteractionResult.CONSUME;
    }
}
