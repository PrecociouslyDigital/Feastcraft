package feastcraft.foodadvancements.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
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
     * A real player who never joined, on an in-memory connection. Forge withholds advancements from
     * fake players, and Carrot's food list sync needs a live connection, which the helper's mock player lacks.
     */
    static ServerPlayer detached(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "food-test"));
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player);
        return player;
    }
}
