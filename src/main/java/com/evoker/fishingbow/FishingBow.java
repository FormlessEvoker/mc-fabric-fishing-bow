package com.evoker.fishingbow;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FishingBow implements ModInitializer {
	public static final String MOD_ID = "fishing-bow";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		FishingBowConfig.initialize();
		ModEntities.initialize();
		ModItems.initialize();
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(player -> {
			ActiveFishingShot shot = player.getAttached(ModAttachments.ACTIVE_FISHING_SHOT);
			if (shot != null) shot.tick();
		}));
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
			ActiveFishingShot shot = listener.getPlayer().getAttached(ModAttachments.ACTIVE_FISHING_SHOT);
			if (shot != null) shot.cancel();
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Sends diagnostic details to the mod's conventional DEBUG logger category. */
	public static void debug(String message, Object... args) {
		LOGGER.debug(message, args);
	}
}
