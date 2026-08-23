package com.evoker.fishingbow.client;

import com.evoker.fishingbow.entity.FishingBowArrow;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the Fishing Bow arrow using the vanilla arrow model/texture, plus a fishing line back to the
 * owner's hand - the same idea as {@code FishingHookRenderer}'s bobber string, drawn as a simple straight
 * segment via {@link SubmitNodeCollector#submitCustomGeometry} rather than replicating that class's
 * sag/curve math.
 */
public class FishingBowArrowRenderer extends ArrowRenderer<FishingBowArrow, FishingBowArrowRenderState> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/projectiles/arrow.png");

    public FishingBowArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
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
            Vec3 handPos = owner.getPosition(partialTick).add(0, owner.getEyeHeight() * 0.6, 0);
            state.ownerHandOffset = handPos.subtract(arrowPos);
        }
    }

    @Override
    public void submit(FishingBowArrowRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        Vec3 end = state.ownerHandOffset;
        if (end == null) {
            return;
        }

        collector.submitCustomGeometry(poseStack, RenderTypes.leash(), (pose, buffer) -> {
            buffer.addVertex(pose, 0.0F, 0.0F, 0.0F)
                    .setColor(210, 210, 210, 255)
                    .setLight(state.lightCoords)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
            buffer.addVertex(pose, (float) end.x, (float) end.y, (float) end.z)
                    .setColor(210, 210, 210, 255)
                    .setLight(state.lightCoords)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        });
    }

    @Override
    protected Identifier getTextureLocation(FishingBowArrowRenderState state) {
        return TEXTURE;
    }
}
