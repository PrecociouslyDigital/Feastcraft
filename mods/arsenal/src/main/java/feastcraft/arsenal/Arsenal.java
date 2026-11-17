package feastcraft.arsenal;

import feastcraft.arsenal.client.ArsenalClient;
import feastcraft.arsenal.motion.Motion;
import feastcraft.arsenal.spear.Lunge;
import feastcraft.arsenal.spear.LungeSwing;
import feastcraft.arsenal.weapon.WeaponClassDamage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Arsenal.MOD_ID)
public final class Arsenal {
    public static final String MOD_ID = "feastcraft_arsenal";

    public Arsenal(FMLJavaModLoadingContext context) {
        ArsenalRegistry.register(context.getModEventBus());
        LungeSwing.register();
        MinecraftForge.EVENT_BUS.addListener(WeaponClassDamage::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(Motion::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(Lunge::onAttackEntity);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ArsenalClient.register(context.getModEventBus()));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
