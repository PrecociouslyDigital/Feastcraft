package feastcraft.arsenal.client;

import feastcraft.arsenal.ArsenalRegistry;
import feastcraft.arsenal.spear.LungeSwing;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.tconstruct.library.client.model.TinkerItemProperties;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;

public final class ArsenalClient {
    private ArsenalClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            TinkerItemProperties.registerToolProperties(ArsenalRegistry.SPEAR.get());
            TinkerItemProperties.registerToolProperties(ArsenalRegistry.MACE.get());
        }));
        modBus.addListener((RegisterColorHandlersEvent.Item event) -> {
            ToolModel.registerItemColors(event.getItemColors(), ArsenalRegistry.SPEAR);
            ToolModel.registerItemColors(event.getItemColors(), ArsenalRegistry.MACE);
        });
        MinecraftForge.EVENT_BUS.addListener(LungeSwing::onLeftClickEmpty);
    }
}
