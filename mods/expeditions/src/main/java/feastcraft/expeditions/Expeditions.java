package feastcraft.expeditions;

import feastcraft.expeditions.base.HearthIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Expeditions.MOD_ID)
public final class Expeditions {
    public static final String MOD_ID = "feastcraft_expeditions";

    public Expeditions(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, ExpeditionsConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ExpeditionsConfig.COMMON_SPEC);
        ExpeditionsRegistry.register(context.getModEventBus());
        context.getModEventBus().addListener((FMLCommonSetupEvent event) -> HearthIndex.registerTicketValidation());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
