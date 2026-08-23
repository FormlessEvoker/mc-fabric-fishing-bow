package com.evoker.fishingbow.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Adds the data needed to draw a fishing line back to the owner's hand, on top of what vanilla's
 * {@link ArrowRenderState} already captures for drawing the arrow itself.
 */
public class FishingBowArrowRenderState extends ArrowRenderState {
    /**
     * The owner's hand position, relative to this arrow's own interpolated position - i.e. the far endpoint
     * of the line to draw, in the same coordinate space {@link com.mojang.blaze3d.vertex.PoseStack} uses
     * inside {@code submit()}. Null if there's no living owner to draw a line to.
     */
    @Nullable
    public Vec3 ownerHandOffset;
}
