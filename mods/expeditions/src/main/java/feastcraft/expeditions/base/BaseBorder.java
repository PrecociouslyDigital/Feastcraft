package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.mixin.DelegateBorderChangeListenerAccessor;
import feastcraft.expeditions.mixin.WorldBorderAccessor;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gives the base its own world border, fixed to {@link BaseBounds}.
 *
 * <p>Vanilla mirrors the overworld's border into every dimension and only ever sends players the
 * overworld's border, so both directions are cut here.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class BaseBorder {
    private BaseBorder() {
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel base = BaseDimension.level(server);
        WorldBorder overworldBorder = server.overworld().getWorldBorder();

        for (BorderChangeListener listener : ((WorldBorderAccessor) overworldBorder).feastcraft$listeners()) {
            if (listener instanceof BorderChangeListener.DelegateBorderChangeListener delegate
                    && ((DelegateBorderChangeListenerAccessor) delegate).feastcraft$target() == base.getWorldBorder()) {
                overworldBorder.removeListener(listener);
            }
        }

        BaseBounds bounds = BaseDimension.bounds();
        base.getWorldBorder().setCenter(bounds.borderCenterX(), bounds.borderCenterZ());
        base.getWorldBorder().setSize(bounds.borderSize());

        // Vanilla broadcasts every overworld border change to all players; this listener runs after
        // that broadcast and puts the base's border back for the players standing in it.
        overworldBorder.addListener(new AnyChange(() -> base.players().forEach(BaseBorder::send)));
    }

    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sendIfInBase(event.getEntity());
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sendIfInBase(event.getEntity());
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendIfInBase(event.getEntity());
    }

    private static void sendIfInBase(Player player) {
        if (player instanceof ServerPlayer serverPlayer && BaseDimension.isBase(serverPlayer.level())) {
            send(serverPlayer);
        }
    }

    private static void send(ServerPlayer player) {
        player.connection.send(new ClientboundInitializeBorderPacket(player.level().getWorldBorder()));
    }

    private record AnyChange(Runnable onChange) implements BorderChangeListener {
        @Override
        public void onBorderSizeSet(WorldBorder border, double size) {
            onChange.run();
        }

        @Override
        public void onBorderSizeLerping(WorldBorder border, double oldSize, double newSize, long time) {
            onChange.run();
        }

        @Override
        public void onBorderCenterSet(WorldBorder border, double x, double z) {
            onChange.run();
        }

        @Override
        public void onBorderSetWarningTime(WorldBorder border, int warningTime) {
            onChange.run();
        }

        @Override
        public void onBorderSetWarningBlocks(WorldBorder border, int warningBlocks) {
            onChange.run();
        }

        @Override
        public void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock) {
            onChange.run();
        }

        @Override
        public void onBorderSetDamageSafeZOne(WorldBorder border, double safeZone) {
            onChange.run();
        }
    }
}
