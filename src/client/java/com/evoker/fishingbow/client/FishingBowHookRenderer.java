package com.evoker.fishingbow.client;

import com.evoker.fishingbow.entity.FishingBowHook;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Draws an arrow and its line at the target's interpolated position, without a projectile entity. */
public final class FishingBowHookRenderer extends EntityRenderer<FishingBowHook, FishingBowHookRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/entity/projectiles/arrow.png");
    private final ArrowModel arrowModel;

    public FishingBowHookRenderer(EntityRendererProvider.Context context) {
        super(context);
        arrowModel = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
    }

    @Override protected boolean affectedByCulling(FishingBowHook entity) { return false; }

    @Override public FishingBowHookRenderState createRenderState() { return new FishingBowHookRenderState(); }

    @Override public void extractRenderState(FishingBowHook hook, FishingBowHookRenderState state, float partialTick) {
        super.extractRenderState(hook, state, partialTick);
        Vec3 marker = hook.getPosition(partialTick);
        Vec3 anchor = marker;
        Entity target = hook.level().getEntity(hook.targetId());
        LivingEntity livingTarget = target instanceof LivingEntity living ? living : null;
        float targetBodyYaw = livingTarget == null ? Float.NaN
                : livingTarget.getPreciseBodyRotation(partialTick);
        float targetModelYaw = livingTarget == null ? Float.NaN : FishingBowHook.modelYaw(targetBodyYaw);
        if (target instanceof LivingEntity living) {
            anchor = living.getPosition(partialTick).add(hook.localOffset()
                    .yRot((float) Math.toRadians(targetModelYaw)));
        }
        state.anchorOffset = anchor.subtract(marker);
        state.ownerHandOffset = null;
        Entity owner = hook.level().getEntity(hook.ownerId());
        if (owner instanceof Player player) {
            state.ownerHandOffset = FishingLineRenderer.handPosition(player, partialTick).subtract(anchor);
        }
        state.arrowYaw = livingTarget != null ? hook.getYRot() + targetModelYaw : hook.getYRot();
        state.arrowPitch = hook.getXRot();
    }

    @Override public void submit(FishingBowHookRenderState state, PoseStack poseStack,
                                  SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(state.anchorOffset);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.arrowYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.arrowPitch));
        collector.submitModel(arrowModel, new ArrowRenderState(), poseStack, TEXTURE,
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        if (state.ownerHandOffset != null) {
            FishingLineRenderer.submit(collector, poseStack, state.ownerHandOffset);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
