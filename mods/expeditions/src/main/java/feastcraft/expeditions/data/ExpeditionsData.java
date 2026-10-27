package feastcraft.expeditions.data;

import feastcraft.expeditions.Expeditions;
import java.util.List;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExpeditionsData {
    private ExpeditionsData() {
    }

    @SubscribeEvent
    static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new BlockModels(output, existingFiles));
        generator.addProvider(event.includeClient(), new ItemModels(output, existingFiles));
        generator.addProvider(event.includeClient(), new EnglishLang(output));
        generator.addProvider(event.includeServer(), new StructureTemplates(output,
                List.of(StructureTemplates.ARENA, StructureTemplates.EXFIL_BEACON)));
    }
}
