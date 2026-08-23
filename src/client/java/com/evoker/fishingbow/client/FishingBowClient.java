package com.evoker.fishingbow.client;

import com.evoker.fishingbow.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class FishingBowClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.FISHING_BOW_ARROW, FishingBowArrowRenderer::new);
	}
}