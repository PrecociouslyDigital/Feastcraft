package feastcraft.expeditions.exfil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-click to call extraction. {@link #SPENT} only drives the look (a spent beacon goes dark);
 * whether a beacon can be used is decided by {@link ExfilBeacons}.
 */
public final class ExfilBeaconBlock extends BaseEntityBlock {
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");

    public ExfilBeaconBlock() {
        super(Properties.of()
                .mapColor(MapColor.DIAMOND)
                .strength(-1.0F, 3_600_000.0F)
                .noLootTable()
                .lightLevel(state -> state.getValue(SPENT) ? 0 : 15)
                .sound(SoundType.LODESTONE)
                .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(SPENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPENT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExfilBeaconBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer caller)) {
            return InteractionResult.SUCCESS;
        }
        String message = switch (ExfilBeacons.call(serverLevel, pos, caller)) {
            case STARTED -> "feastcraft_expeditions.exfil.called";
            case NOT_RAIDING -> "feastcraft_expeditions.exfil.not_raiding";
            case SPENT -> "feastcraft_expeditions.exfil.spent";
            case REFUSED -> "feastcraft_expeditions.exfil.refused";
        };
        caller.displayClientMessage(Component.translatable(message), true);
        return InteractionResult.CONSUME;
    }
}
