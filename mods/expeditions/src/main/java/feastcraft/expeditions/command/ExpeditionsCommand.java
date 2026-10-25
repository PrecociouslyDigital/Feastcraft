package feastcraft.expeditions.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.TickTime;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.base.BaseFuel;
import feastcraft.expeditions.base.FuelTank;
import feastcraft.expeditions.expedition.Deployment;
import feastcraft.expeditions.expedition.Expedition;
import feastcraft.expeditions.expedition.RaidLedger;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator tools. Each subcommand goes through the same code path as its in-world counterpart. */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class ExpeditionsCommand {
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
                .then(Commands.literal("fuel")
                        .then(Commands.literal("get").executes(context -> changeFuel(context, UnaryOperator.identity())))
                        .then(Commands.literal("add").then(Commands.argument("ticks", LongArgumentType.longArg(0))
                                .executes(context -> changeFuel(context, tank -> tank.add(LongArgumentType.getLong(context, "ticks"))))))
                        .then(Commands.literal("set").then(Commands.argument("ticks", LongArgumentType.longArg(0))
                                .executes(context -> changeFuel(context, tank -> new FuelTank(LongArgumentType.getLong(context, "ticks"))))))));
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

    private static int changeFuel(CommandContext<CommandSourceStack> context, UnaryOperator<FuelTank> change) {
        ServerLevel base = BaseDimension.level(context.getSource().getServer());
        BaseFuel fuel = BaseFuel.get(base);
        fuel.update(base, change);
        long ticks = fuel.tank().ticksRemaining();
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.feastcraft_expeditions.fuel", TickTime.format(ticks), ticks), false);
        return (int) Math.min(Integer.MAX_VALUE, ticks);
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
