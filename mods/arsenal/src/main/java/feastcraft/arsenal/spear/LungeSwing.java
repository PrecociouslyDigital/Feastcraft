package feastcraft.arsenal.spear;

import feastcraft.arsenal.Arsenal;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Tells the server a Lunge spear swung at nothing. */
public record LungeSwing() {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Arsenal.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(LungeSwing.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((swing, buffer) -> {
                })
                .decoder((FriendlyByteBuf buffer) -> new LungeSwing())
                .consumerMainThread(LungeSwing::handle)
                .add();
    }

    private static void handle(LungeSwing swing, Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null) {
            Lunge.lunge(player);
        }
        context.get().setPacketHandled(true);
    }

    /** Client side: a swing that hit no block or entity. */
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (Lunge.level(event.getItemStack()) > 0) {
            CHANNEL.send(PacketDistributor.SERVER.noArg(), new LungeSwing());
        }
    }
}
