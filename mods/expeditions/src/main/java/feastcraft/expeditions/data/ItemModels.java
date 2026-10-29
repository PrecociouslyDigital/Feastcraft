package feastcraft.expeditions.data;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

final class ItemModels extends ItemModelProvider {
    private static final int COMPASS_FRAMES = 32;

    ItemModels(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Expeditions.MOD_ID, existingFiles);
    }

    @Override
    protected void registerModels() {
        exfilCompass();
        emberCharcoal();
    }

    /** Borrows vanilla charcoal's shape, recolored to ember reds by the texture provider. */
    private void emberCharcoal() {
        withExistingParent(ExpeditionsRegistry.EMBER_CHARCOAL.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", Expeditions.id("item/ember_charcoal"));
    }

    /**
     * Borrows the vanilla compass's 32 needle frames. Overrides are not inherited from a parent model,
     * so the angle table from vanilla's compass.json is repeated here. Frame 16 is the needle pointing
     * straight up, and each frame covers the angles centered on its index over 32.
     */
    private void exfilCompass() {
        ItemModelBuilder compass = withExistingParent(ExpeditionsRegistry.EXFIL_COMPASS.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/compass_16"));
        for (int i = 0; i <= COMPASS_FRAMES; i++) {
            float angle = i == 0 ? 0.0F : (i - 0.5F) / COMPASS_FRAMES;
            compass.override()
                    .predicate(ResourceLocation.withDefaultNamespace("angle"), angle)
                    .model(new ModelFile.UncheckedModelFile(mcLoc(String.format("item/compass_%02d", (i + 16) % COMPASS_FRAMES))))
                    .end();
        }
    }
}
