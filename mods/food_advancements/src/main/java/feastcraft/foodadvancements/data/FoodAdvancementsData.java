package feastcraft.foodadvancements.data;

import feastcraft.foodadvancements.FoodAdvancements;
import java.util.List;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FoodAdvancements.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FoodAdvancementsData {
    private FoodAdvancementsData() {
    }

    @SubscribeEvent
    static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new EnglishLang(output));
        generator.addProvider(event.includeServer(), new Menus(output, event.getLookupProvider(), existingFiles));
        generator.addProvider(event.includeServer(), new ForgeAdvancementProvider(output, event.getLookupProvider(), existingFiles,
                List.of(new EatingAdvancements())));
    }
}
