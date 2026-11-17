package feastcraft.arsenal;

import feastcraft.arsenal.weapon.WeaponClassDamage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Arsenal.MOD_ID)
public final class Arsenal {
    public static final String MOD_ID = "feastcraft_arsenal";

    public Arsenal(FMLJavaModLoadingContext context) {
        ArsenalRegistry.register(context.getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(WeaponClassDamage::onLivingHurt);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
