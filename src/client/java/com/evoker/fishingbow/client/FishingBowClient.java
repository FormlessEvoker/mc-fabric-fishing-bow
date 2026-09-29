package com.evoker.fishingbow.client;

import com.evoker.fishingbow.FishingBow;
import com.evoker.fishingbow.ModEntities;
import com.evoker.fishingbow.entity.FishingBowArrow;
import com.evoker.fishingbow.entity.FishingBowHook;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class FishingBowClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FishingBowClientConfig.initialize();
		EntityRendererRegistry.register(ModEntities.FISHING_BOW_ARROW, FishingBowArrowRenderer::new);
		EntityRendererRegistry.register(ModEntities.FISHING_BOW_HOOK, FishingBowHookRenderer::new);
		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> logEntityTracking("client_entity_load", entity));
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> logEntityTracking("client_entity_unload", entity));
	}

	private static void logEntityTracking(String event, Entity entity) {
		if (!(entity instanceof FishingBowHook) && !(entity instanceof FishingBowArrow)) return;
		var player = Minecraft.getInstance().player;
		FishingBow.debug("event={} entityId={} entityType={} playerToEntity={}",
				event, entity.getId(), entity.getType(),
				player == null ? -1.0 : player.distanceTo(entity));
	}
}
