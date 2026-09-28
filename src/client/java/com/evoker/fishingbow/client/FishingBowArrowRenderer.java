package com.evoker.fishingbow.client;

import com.evoker.fishingbow.entity.FishingBowArrow;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the Fishing Bow arrow using the vanilla arrow model/texture, plus a fishing line back to the
 * owner's hand. The hook marker uses the same line geometry while no projectile exists.
 */
public class FishingBowArrowRenderer extends ArrowRenderer<FishingBowArrow, FishingBowArrowRenderState> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/projectiles/arrow.png");

    public FishingBowArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean affectedByCulling(FishingBowArrow entity) {
        return false;
    }

    @Override
    public FishingBowArrowRenderState createRenderState() {
        return new FishingBowArrowRenderState();
    }

    @Override
    public void extractRenderState(FishingBowArrow entity, FishingBowArrowRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.ownerHandOffset = null;
        if (entity.getOwner() instanceof Player owner) {
            Vec3 arrowPos = entity.getPosition(partialTick);
            Vec3 handPos = FishingLineRenderer.handPosition(owner, partialTick);
            state.ownerHandOffset = handPos.subtract(arrowPos);
        }
    }

    @Override
    public void submit(FishingBowArrowRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        Vec3 end = state.ownerHandOffset;
        if (end != null) {
            FishingLineRenderer.submit(collector, poseStack, end);
        }
    }

    @Override
    protected Identifier getTextureLocation(FishingBowArrowRenderState state) {
        return TEXTURE;
    }
}
