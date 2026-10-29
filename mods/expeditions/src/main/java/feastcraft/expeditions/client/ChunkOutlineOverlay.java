package feastcraft.expeditions.client;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.base.HearthBlock;
import feastcraft.expeditions.base.HearthBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * While the crosshair rests on a hearth, outlines the chunk that hearth powers: green while it is
 * fueled, red while it is frozen. Purely visual; drawn like the debug chunk borders.
 */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID, value = Dist.CLIENT)
public final class ChunkOutlineOverlay {
    private static final float[] FUELED = {0.2F, 1.0F, 0.3F};
    private static final float[] FROZEN = {1.0F, 0.25F, 0.2F};

    private ChunkOutlineOverlay() {
    }

    @SubscribeEvent
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !ExpeditionsConfig.EMBER_ENABLED.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || level.dimension() != BaseDimension.BASE
                || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof HearthBlock)
                || !(level.getBlockEntity(pos) instanceof HearthBlockEntity hearth)) {
            return;
        }

        ChunkPos chunk = new ChunkPos(pos);
        AABB bounds = new AABB(
                chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMinBlockX() + 16, level.getMaxBuildHeight(), chunk.getMinBlockZ() + 16);
        float[] color = hearth.fueled() ? FUELED : FROZEN;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lines, bounds, color[0], color[1], color[2], 0.6F);
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        poseStack.popPose();
    }
}
