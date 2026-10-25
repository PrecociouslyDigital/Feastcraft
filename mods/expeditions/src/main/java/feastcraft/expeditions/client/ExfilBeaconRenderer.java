package feastcraft.expeditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import feastcraft.expeditions.exfil.ExfilBeaconBlock;
import feastcraft.expeditions.exfil.ExfilBeaconBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Draws a vanilla beacon beam over every unspent beacon, so beacons double as landmarks. */
final class ExfilBeaconRenderer implements BlockEntityRenderer<ExfilBeaconBlockEntity> {
    private static final float[] BEAM_COLOR = {0.35F, 0.85F, 1.0F};

    ExfilBeaconRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ExfilBeaconBlockEntity beacon, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (beacon.getLevel() == null || beacon.getBlockState().getValue(ExfilBeaconBlock.SPENT)) {
            return;
        }
        BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
                beacon.getLevel().getGameTime(), 1, BeaconRenderer.MAX_RENDER_Y, BEAM_COLOR, 0.2F, 0.25F);
    }

    @Override
    public boolean shouldRenderOffScreen(ExfilBeaconBlockEntity beacon) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
