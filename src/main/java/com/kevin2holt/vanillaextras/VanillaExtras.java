package com.kevin2holt.vanillaextras;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaExtras implements ModInitializer {
	public static final String MOD_ID = "vanilla-extras";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Vanilla Extras initialized");
	}
}
