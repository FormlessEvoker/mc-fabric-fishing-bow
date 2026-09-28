package com.evoker.fishingbow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.evoker.fishingbow.ModItems;

/** Shared line geometry for physical arrows and arrowless hook anchors. */
final class FishingLineRenderer {
    private static final int SEGMENTS = 16;

    private FishingLineRenderer() { }

    static Vec3 handPosition(Player player, float partialTick) {
        HumanoidArm arm = player.getMainHandItem().is(ModItems.FISHING_BOW)
                ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == player && minecraft.options.getCameraType().isFirstPerson()) {
            float fov = minecraft.options.fov().get();
            Vec3 nearHand = minecraft.getEntityRenderDispatcher().camera.getNearPlane(fov)
                    .getPointOnPlane(side * 0.525F + FishingBowClientConfig.firstPersonLineHorizontalOffset,
                            -0.1F + FishingBowClientConfig.firstPersonLineVerticalOffset)
                    .scale(960.0 / fov);
            return player.getEyePosition(partialTick).add(nearHand);
        }

        double yaw = Math.toRadians(player.getPreciseBodyRotation(partialTick));
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double scale = player.getScale();
        double sideways = side * 0.35 * scale;
        double forward = 0.8 * scale;
        double crouch = player.isCrouching() ? -0.1875 : 0.0;
        return player.getEyePosition(partialTick).add(
                -cos * sideways - sin * forward,
                crouch - 0.45 * scale,
                -sin * sideways + cos * forward);
    }

    static void submit(SubmitNodeCollector collector, PoseStack poseStack, Vec3 end) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            float endX = (float) end.x;
            float endY = (float) end.y;
            float endZ = (float) end.z;
            for (int i = 0; i < SEGMENTS; i++) {
                float startT = (float) i / SEGMENTS;
                float endT = (float) (i + 1) / SEGMENTS;
                float startX = endX * startT;
                float startY = endY * startT - 4.0F * FishingBowClientConfig.lineSag * startT * (1.0F - startT);
                float startZ = endZ * startT;
                float finishX = endX * endT;
                float finishY = endY * endT - 4.0F * FishingBowClientConfig.lineSag * endT * (1.0F - endT);
                float finishZ = endZ * endT;

                float normalX = finishX - startX;
                float normalY = finishY - startY;
                float normalZ = finishZ - startZ;
                float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
                if (length > 0.0F) {
                    normalX /= length;
                    normalY /= length;
                    normalZ /= length;
                } else {
                    normalY = 1.0F;
                }

                lineVertex(buffer, pose, startX, startY, startZ, normalX, normalY, normalZ);
                lineVertex(buffer, pose, finishX, finishY, finishZ, normalX, normalY, normalZ);
            }
        });
    }

    private static void lineVertex(VertexConsumer buffer, PoseStack.Pose pose,
            float x, float y, float z, float normalX, float normalY, float normalZ) {
        buffer.addVertex(pose, x, y, z)
                .setColor(FishingBowClientConfig.lineColor)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(FishingBowClientConfig.lineWidth);
    }
}
