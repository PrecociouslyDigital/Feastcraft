package feastcraft.expeditions.data;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

final class EnglishLang extends LanguageProvider {
    EnglishLang(PackOutput output) {
        super(output, Expeditions.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.feastcraft_expeditions", "Feastcraft Expeditions");
        add(ExpeditionsRegistry.HEARTH.get(), "Hearth");

        add("feastcraft_expeditions.base.running", "The hearth roars to life. The base is running again.");
        add("feastcraft_expeditions.base.frozen", "The hearth has gone out. Plants and machines in the base have stopped.");
        add("feastcraft_expeditions.hearth.not_in_base", "A hearth only burns in the base.");
        add("feastcraft_expeditions.hearth.remaining", "Fuel remaining: %s");
        add("feastcraft_expeditions.hearth.empty", "The hearth is cold. Feed it fuel to run the base.");

        add("commands.feastcraft_expeditions.recall", "Recalled %s to the base");
        add("commands.feastcraft_expeditions.raid.info", "%s is raiding: dropped at %s, %s seconds ago");
        add("commands.feastcraft_expeditions.raid.none", "%s is at the base");
        add("commands.feastcraft_expeditions.fuel", "Base fuel: %s (%s ticks)");
    }
}
