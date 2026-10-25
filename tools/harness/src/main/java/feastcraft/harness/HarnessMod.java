package feastcraft.harness;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Lets an agent play the client headlessly through an HTTP control API. It does nothing unless
 * the {@value #PORT_PROPERTY} system property names the port to listen on.
 */
@Mod(HarnessMod.MOD_ID)
public final class HarnessMod {
    public static final String MOD_ID = "feastcraft_harness";
    static final String PORT_PROPERTY = "feastcraft.harness.port";

    public HarnessMod() {
        Integer port = Integer.getInteger(PORT_PROPERTY);
        if (port == null || FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(AutoWorld.class);
        MinecraftForge.EVENT_BUS.register(ClientTicks.class);
        MinecraftForge.EVENT_BUS.register(Observations.class);
        ControlServer.start(port);
    }
}
