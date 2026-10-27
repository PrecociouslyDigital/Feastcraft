package feastcraft.advancementpoints;

import feastcraft.advancementpoints.gametest.GameTestPack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AdvancementPoints.MOD_ID)
public final class AdvancementPoints {
    public static final String MOD_ID = "feastcraft_advancement_points";

    public AdvancementPoints(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(GameTestPack::addWhenTesting);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
