package feastcraft.expeditions.data;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

/** Every block is dressed in vanilla textures, so the mod ships no art of its own. */
final class BlockModels extends BlockStateProvider {
    BlockModels(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Expeditions.MOD_ID, existingFiles);
    }

    @Override
    protected void registerStatesAndModels() {
        withItem(ExpeditionsRegistry.HEARTH.get(), models().cubeBottomTop("hearth",
                vanilla("blast_furnace_front_on"), vanilla("smooth_stone"), vanilla("blast_furnace_top")));
    }

    private void withItem(Block block, ModelFile model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    static ResourceLocation vanilla(String blockTexture) {
        return ResourceLocation.withDefaultNamespace("block/" + blockTexture);
    }
}
