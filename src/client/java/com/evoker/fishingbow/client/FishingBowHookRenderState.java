package com.evoker.fishingbow.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public final class FishingBowHookRenderState extends EntityRenderState {
    public Vec3 anchorOffset = Vec3.ZERO;
    public Vec3 ownerHandOffset;
    public float arrowYaw;
    public float arrowPitch;
}
