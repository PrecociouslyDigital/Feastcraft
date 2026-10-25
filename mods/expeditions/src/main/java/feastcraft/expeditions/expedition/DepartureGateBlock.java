package feastcraft.expeditions.expedition;

import feastcraft.expeditions.base.BaseDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click to deploy to a random point in the overworld. Deploying never needs fuel. */
public final class DepartureGateBlock extends Block {
    public DepartureGateBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(-1.0F, 3_600_000.0F)
                .noLootTable()
                .lightLevel(state -> 10)
                .sound(SoundType.LODESTONE)
                .pushReaction(PushReaction.BLOCK));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!BaseDimension.isBase(level)) {
            serverPlayer.displayClientMessage(Component.translatable("feastcraft_expeditions.gate.not_in_base"), true);
        } else if (!Deployment.start(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("feastcraft_expeditions.deploy.busy"), true);
        }
        return InteractionResult.CONSUME;
    }
}
