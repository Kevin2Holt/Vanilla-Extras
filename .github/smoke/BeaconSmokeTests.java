package com.kevin2holt.vanillaextras.smoke;

import com.kevin2holt.vanillaextras.beacon.SaturationBeacon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// CI-only checks, run on a dedicated server by .github/workflows/smoke.yml. Builds beacon bases
// out of real blocks and checks which healing-slot effect and icon the mod picks for each.
public class BeaconSmokeTests implements ModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("vanilla-extras-smoke");
	private static final BlockPos BEACON_POS = new BlockPos(0, 300, 0);
	private static final int FULL_LAYERS = 4;

	private int failureCount = 0;


	@Override
	public void onInitialize() {

		ServerLifecycleEvents.SERVER_STARTED.register(this::runAllTests);
	}



	private void runAllTests(MinecraftServer server) {

		ServerLevel level = server.overworld();
		level.getChunk(0, 0);

		// Full diamond/netherite base
		buildBase(level, FULL_LAYERS, Blocks.DIAMOND_BLOCK, Blocks.NETHERITE_BLOCK);
		expectEffect(level, "full diamond base, Regeneration stored", MobEffects.REGENERATION, FULL_LAYERS, MobEffects.SATURATION);
		expectEffect(level, "full diamond base, Saturation stored", MobEffects.SATURATION, FULL_LAYERS, MobEffects.SATURATION);
		expectEffect(level, "full diamond base, Speed secondary untouched", MobEffects.SPEED, FULL_LAYERS, MobEffects.SPEED);
		expectIcon(level, "full diamond base shows Saturation", FULL_LAYERS, true);

		// One block swapped to iron, then swapped back
		level.setBlock(BEACON_POS.offset(4, -4, 4), Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		expectEffect(level, "one iron block, Saturation stored", MobEffects.SATURATION, FULL_LAYERS, MobEffects.REGENERATION);
		expectIcon(level, "one iron block shows Regeneration", FULL_LAYERS, false);
		level.setBlock(BEACON_POS.offset(4, -4, 4), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		expectEffect(level, "iron swapped back to diamond", MobEffects.SATURATION, FULL_LAYERS, MobEffects.SATURATION);

		// Whole base iron
		buildBase(level, FULL_LAYERS, Blocks.IRON_BLOCK, Blocks.IRON_BLOCK);
		expectEffect(level, "full iron base, Saturation stored", MobEffects.SATURATION, FULL_LAYERS, MobEffects.REGENERATION);
		expectIcon(level, "full iron base shows Regeneration", FULL_LAYERS, false);

		// Partial diamond bases: icon switches, effect never becomes Saturation
		for (int layerCount = 1; layerCount < FULL_LAYERS; layerCount++) {
			clearBase(level);
			buildBase(level, layerCount, Blocks.DIAMOND_BLOCK, Blocks.NETHERITE_BLOCK);
			expectIcon(level, layerCount + "-layer diamond base shows Saturation", layerCount, true);
			expectEffect(level, layerCount + "-layer diamond base, Saturation stored", MobEffects.SATURATION, layerCount, MobEffects.REGENERATION);
		}

		// No base at all
		clearBase(level);
		expectIcon(level, "no base shows Regeneration", 0, false);

		LOGGER.info(this.failureCount == 0 ? "SMOKE DONE all passed" : "SMOKE DONE with " + this.failureCount + " failure(s)");
		server.halt(false);
	}



	private static void buildBase(ServerLevel level, int layerCount, Block evenLayerBlock, Block oddLayerBlock) {

		for (int layer = 1; layer <= layerCount; layer++) {
			Block layerBlock = layer % 2 == 0 ? evenLayerBlock : oddLayerBlock;
			fillLayer(level, layer, layerBlock);
		}
	}


	private static void clearBase(ServerLevel level) {

		for (int layer = 1; layer <= FULL_LAYERS; layer++) {
			fillLayer(level, layer, Blocks.AIR);
		}
	}


	private static void fillLayer(ServerLevel level, int layer, Block block) {

		for (int x = -layer; x <= layer; x++) {
			for (int z = -layer; z <= layer; z++) {
				level.setBlock(BEACON_POS.offset(x, -layer, z), block.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}


	private void expectEffect(ServerLevel level, String caseName, Holder<MobEffect> storedSecondary, int levels, Holder<MobEffect> expectedEffect) {

		Holder<MobEffect> actualEffect = SaturationBeacon.resolveSecondaryEffect(storedSecondary, level, BEACON_POS, levels);
		report(caseName, expectedEffect.equals(actualEffect), expectedEffect.getRegisteredName(), actualEffect == null ? "none" : actualEffect.getRegisteredName());
	}


	private void expectIcon(ServerLevel level, String caseName, int levels, boolean isSaturationExpected) {

		boolean isSaturationShown = SaturationBeacon.isSaturationShown(level, BEACON_POS, levels);
		report(caseName, isSaturationShown == isSaturationExpected, String.valueOf(isSaturationExpected), String.valueOf(isSaturationShown));
	}


	private void report(String caseName, boolean isPassed, String expected, String actual) {

		if (isPassed) {
			LOGGER.info("SMOKE PASS {}", caseName);
			return;
		}

		this.failureCount++;
		LOGGER.error("SMOKE FAIL {} (expected {}, got {})", caseName, expected, actual);
	}
}
