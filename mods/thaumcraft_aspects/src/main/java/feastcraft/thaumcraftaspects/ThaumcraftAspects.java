package feastcraft.thaumcraftaspects;

import feastcraft.thaumcraftaspects.gametest.GameTestPack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ThaumcraftAspects.MOD_ID)
public final class ThaumcraftAspects {
    public static final String MOD_ID = "feastcraft_thaumcraft_aspects";

    public ThaumcraftAspects(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(GameTestPack::addWhenTesting);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
