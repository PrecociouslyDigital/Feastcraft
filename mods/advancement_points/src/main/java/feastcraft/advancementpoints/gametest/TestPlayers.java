package feastcraft.advancementpoints.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

final class TestPlayers {
    private TestPlayers() {
    }

    /**
     * A real player who never joined. Forge withholds advancements from fake players, and the
     * helper's mock player cannot join on Forge, whose registry sync needs a live connection.
     */
    static ServerPlayer detached(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "points-test"));
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player);
        return player;
    }
}
