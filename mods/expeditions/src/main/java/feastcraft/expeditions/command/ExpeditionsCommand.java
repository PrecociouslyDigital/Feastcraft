package feastcraft.expeditions.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.TickTime;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.base.HearthBlockEntity;
import feastcraft.expeditions.base.FuelTank;
import feastcraft.expeditions.exfil.ExfilBeacons;
import feastcraft.expeditions.expedition.Deployment;
import feastcraft.expeditions.expedition.Expedition;
import feastcraft.expeditions.expedition.RaidLedger;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator tools. Each subcommand goes through the same code path as its in-world counterpart. */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class ExpeditionsCommand {
    private static final int NEAREST_BEACON_SEARCH = 4096;

    private ExpeditionsCommand() {
    }

    @FunctionalInterface
    private interface PlayerAction {
        int run(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException;
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("expeditions")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(forPlayer("deploy", ExpeditionsCommand::deploy))
                .then(forPlayer("recall", ExpeditionsCommand::recall))
                .then(Commands.literal("raid").then(forPlayer("info", ExpeditionsCommand::raidInfo)))
                .then(Commands.literal("beacon")
                        .then(Commands.literal("reset")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ExpeditionsCommand::resetBeacon)))
                        .then(Commands.literal("nearest").executes(ExpeditionsCommand::nearestBeacon)))
                .then(Commands.literal("fuel").then(fuelCommands())));
    }

    /** {@code <name> [player]}, where the player defaults to whoever runs the command. */
    private static LiteralArgumentBuilder<CommandSourceStack> forPlayer(String name, PlayerAction action) {
        return Commands.literal(name)
                .executes(context -> action.run(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> action.run(context, EntityArgument.getPlayer(context, "player"))));
    }

    private static int deploy(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (!Deployment.start(player)) {
            context.getSource().sendFailure(Component.translatable("commands.feastcraft_expeditions.deploy.busy", player.getDisplayName()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.feastcraft_expeditions.deploy", player.getDisplayName()), true);
        return 1;
    }

    private static int recall(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Expedition.extract(player);
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.feastcraft_expeditions.recall", player.getDisplayName()), true);
        return 1;
    }

    /** {@code fuel <pos> <get|add|set>} targets the hearth's own tank at the given position. */
    private static ArgumentBuilder<CommandSourceStack, ?> fuelCommands() {
        return Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.literal("get").executes(context -> changeFuel(context, UnaryOperator.identity())))
                .then(Commands.literal("add").then(Commands.argument("ticks", LongArgumentType.longArg(0))
                        .executes(context -> changeFuel(context, tank -> tank.add(LongArgumentType.getLong(context, "ticks"))))))
                .then(Commands.literal("set").then(Commands.argument("ticks", LongArgumentType.longArg(0))
                        .executes(context -> changeFuel(context, tank -> new FuelTank(LongArgumentType.getLong(context, "ticks"))))));
    }

    private static int changeFuel(CommandContext<CommandSourceStack> context, UnaryOperator<FuelTank> change) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ServerLevel level = context.getSource().getLevel();
        if (!(level.getBlockEntity(pos) instanceof HearthBlockEntity hearth)) {
            context.getSource().sendFailure(Component.translatable("commands.feastcraft_expeditions.fuel.no_hearth"));
            return 0;
        }
        hearth.setFuel(change.apply(hearth.tank()));
        long ticks = hearth.tank().ticksRemaining();
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.feastcraft_expeditions.fuel", pos.toShortString(), TickTime.format(ticks), ticks), false);
        return (int) Math.min(Integer.MAX_VALUE, ticks);
    }

    private static int resetBeacon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ExfilBeacons.reset(context.getSource().getLevel(), pos);
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.feastcraft_expeditions.beacon.reset", pos.toShortString()), true);
        return 1;
    }

    private static int nearestBeacon(CommandContext<CommandSourceStack> context) {
        BlockPos from = BlockPos.containing(context.getSource().getPosition());
        Optional<BlockPos> beacon = ExfilBeacons.nearestUnspent(context.getSource().getLevel(), from, NEAREST_BEACON_SEARCH);
        context.getSource().sendSuccess(() -> beacon
                .map(pos -> Component.translatable("commands.feastcraft_expeditions.beacon.nearest",
                        pos.getX(), pos.getZ(), (int) ExfilBeacons.horizontalDistance(from, pos)))
                .orElseGet(() -> Component.translatable("commands.feastcraft_expeditions.beacon.none")), false);
        return beacon.isPresent() ? 1 : 0;
    }

    private static int raidInfo(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        long now = player.server.overworld().getGameTime();
        Component message = RaidLedger.get(player.server).raidOf(player.getUUID())
                .map(raid -> Component.translatable("commands.feastcraft_expeditions.raid.info",
                        player.getDisplayName(), raid.drop().toShortString(), (now - raid.startedAt()) / 20))
                .orElseGet(() -> Component.translatable(
                        "commands.feastcraft_expeditions.raid.none", player.getDisplayName()));
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }
}
