package feastcraft.expeditions.data;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import feastcraft.expeditions.exfil.ExfilBeaconBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
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
        withItem(ExpeditionsRegistry.DEPARTURE_GATE.get(), models().cubeBottomTop("departure_gate",
                vanilla("crying_obsidian"), vanilla("respawn_anchor_bottom"), vanilla("respawn_anchor_top")));

        ModelFile lit = models().cubeBottomTop("exfil_beacon",
                vanilla("lodestone_side"), vanilla("lodestone_top"), vanilla("beacon"));
        ModelFile spent = models().cubeBottomTop("exfil_beacon_spent",
                vanilla("lodestone_side"), vanilla("lodestone_top"), vanilla("obsidian"));
        getVariantBuilder(ExpeditionsRegistry.EXFIL_BEACON.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(state.getValue(ExfilBeaconBlock.SPENT) ? spent : lit).build());
        simpleBlockItem(ExpeditionsRegistry.EXFIL_BEACON.get(), lit);
    }

    private void withItem(Block block, ModelFile model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    static ResourceLocation vanilla(String blockTexture) {
        return ResourceLocation.withDefaultNamespace("block/" + blockTexture);
    }
}
