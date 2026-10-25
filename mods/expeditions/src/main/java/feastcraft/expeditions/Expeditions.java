package feastcraft.expeditions;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Expeditions.MOD_ID)
public final class Expeditions {
    public static final String MOD_ID = "feastcraft_expeditions";

    public Expeditions(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, ExpeditionsConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
