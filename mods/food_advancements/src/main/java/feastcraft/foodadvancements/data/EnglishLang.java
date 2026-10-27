package feastcraft.foodadvancements.data;

import feastcraft.foodadvancements.FoodAdvancements;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

final class EnglishLang extends LanguageProvider {
    EnglishLang(PackOutput output) {
        super(output, FoodAdvancements.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        EatingAdvancements.ENTRIES.forEach(entry -> {
            add(entry.key("title"), entry.title());
            add(entry.key("description"), entry.description());
        });
    }
}
