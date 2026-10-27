package feastcraft.foodadvancements;

import feastcraft.foodadvancements.advancement.FoodsEatenTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FoodAdvancements.MOD_ID)
public final class FoodAdvancements {
    public static final String MOD_ID = "feastcraft_food_advancements";

    public FoodAdvancements(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> CriteriaTriggers.register(FoodsEatenTrigger.INSTANCE)));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
