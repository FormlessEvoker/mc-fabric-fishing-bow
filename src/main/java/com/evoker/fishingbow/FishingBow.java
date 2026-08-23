package com.evoker.fishingbow;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FishingBow implements ModInitializer {
	public static final String MOD_ID = "fishing-bow";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.initialize();
		ModItems.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/**
	 * Always-on debug logging for the fishing-bow mechanic, kept in place (not stripped after each fix) so
	 * behavior can be traced across test sessions. Grep {@code run/logs/latest.log} for "[FBDEBUG]".
	 */
	public static void debug(String message, Object... args) {
		LOGGER.info("[FBDEBUG] " + message, args);
	}
}
