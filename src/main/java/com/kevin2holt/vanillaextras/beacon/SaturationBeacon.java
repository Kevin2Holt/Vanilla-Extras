package com.kevin2holt.vanillaextras.beacon;

import com.kevin2holt.vanillaextras.feature.ModFeatures;
import com.kevin2holt.vanillaextras.network.BeaconOptionsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Saturation as a tier-4 secondary effect, offered only on beacons whose full 4-layer pyramid
// is built from diamond and/or netherite blocks. There it takes Regeneration's place in the
// modded beacon screen. Vanilla clients still see Regeneration, which the server keeps accepting.
public final class SaturationBeacon {
	private static final int PYRAMID_LAYERS_REQUIRED = 4;// a secondary effect needs the full pyramid


	private SaturationBeacon() {
	}


	public static void register() {

		PayloadTypeRegistry.clientboundPlay().register(BeaconOptionsPayload.TYPE, BeaconOptionsPayload.STREAM_CODEC);
	}


	public static boolean isSaturation(Holder<MobEffect> effect) {

		return MobEffects.SATURATION.equals(effect);
	}


	// Whether this beacon may give Saturation right now: feature on and a diamond/netherite pyramid
	public static boolean isSaturationOffered(Level level, BlockPos beaconPos) {

		return ModFeatures.SATURATION_BEACON.isEnabled() && isSaturationPyramid(level, beaconPos);
	}


	// Checks every block of all 4 layers (9x9, 7x7, 5x5 and 3x3 under the beacon)
	public static boolean isSaturationPyramid(Level level, BlockPos beaconPos) {

		for (int layer = 1; layer <= PYRAMID_LAYERS_REQUIRED; layer++) {
			int layerY = beaconPos.getY() - layer;
			if (layerY < level.getMinY()) {
				return false;
			}

			for (int x = beaconPos.getX() - layer; x <= beaconPos.getX() + layer; x++) {
				for (int z = beaconPos.getZ() - layer; z <= beaconPos.getZ() + layer; z++) {
					if (!isSaturationBaseBlock(level.getBlockState(new BlockPos(x, layerY, z)))) {
						return false;
					}
				}
			}
		}

		return true;
	}


	private static boolean isSaturationBaseBlock(BlockState state) {

		return state.is(Blocks.DIAMOND_BLOCK) || state.is(Blocks.NETHERITE_BLOCK);
	}


	// Tells a modded client which tier-4 option to show; sent just before the beacon screen opens
	public static void sendBeaconOptions(ServerPlayer player, Level level, BlockPos beaconPos) {

		// Vanilla clients have not registered the channel, so they are skipped here
		if (!ServerPlayNetworking.canSend(player, BeaconOptionsPayload.TYPE)) {
			return;
		}

		ServerPlayNetworking.send(player, new BeaconOptionsPayload(isSaturationOffered(level, beaconPos)));
	}
}
