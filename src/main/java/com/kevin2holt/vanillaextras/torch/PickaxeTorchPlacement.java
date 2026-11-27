package com.kevin2holt.vanillaextras.torch;

import com.kevin2holt.vanillaextras.VanillaExtras;
import com.kevin2holt.vanillaextras.inventory.CarriedItemSearch;
import com.kevin2holt.vanillaextras.inventory.ItemSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Right-clicking a block with a pickaxe places a torch from the player's inventory, exactly as
// if the torch had been held. With the fromContainers setting on, a player with no loose torches
// can also place one from a shulker box, bundle, or ender chest (see CarriedItemSearch). Called only after vanilla found nothing else to do with the click
// (no block interaction such as opening a chest, no item use-on-block), on both the server and a
// modded client (where it is the usual client-side placement prediction).
public final class PickaxeTorchPlacement {
	// Which items count as torches; a datapack can change this tag without touching the mod
	public static final TagKey<Item> PLACEABLE_TORCHES =
		TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(VanillaExtras.MOD_ID, "pickaxe_placeable_torches"));

	// Same volume and pitch scaling BlockItem.place uses for its placement sound
	private static final float PLACE_SOUND_VOLUME_OFFSET = 1.0F;
	private static final float PLACE_SOUND_VOLUME_DIVISOR = 2.0F;
	private static final float PLACE_SOUND_PITCH_SCALE = 0.8F;

	// Cached per item class: whether the class replaces Item's right-click methods
	private static final ClassValue<Boolean> OVERRIDES_RIGHT_CLICK = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> itemClass) {

			return overridesItemMethod(itemClass, "use", Level.class, Player.class, InteractionHand.class)
				|| overridesItemMethod(itemClass, "useOn", UseOnContext.class);
		}
	};


	private PickaxeTorchPlacement() {
	}



	// Places a torch and returns the placement result, or PASS when this click is not a pickaxe torch placement.
	// isFromContainersAllowed: when no loose torch is carried, also look in shulker boxes, bundles, and the
	// ender chest (server only; a client cannot see the ender chest).
	public static InteractionResult tryPlaceTorch(Player player, InteractionHand hand, BlockHitResult hitResult, boolean isFromContainersAllowed) {

		if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) {
			return InteractionResult.PASS;
		}

		if (!isPlainPickaxe(player.getMainHandItem())) {
			return InteractionResult.PASS;
		}

		ItemStack torchStack = findLooseTorchStack(player);
		if (!torchStack.isEmpty()) {
			return placeTorch(player, hand, hitResult, torchStack);
		}

		if (!isFromContainersAllowed) {
			return InteractionResult.PASS;
		}

		return placeTorchFromContainers(player, hand, hitResult);
	}


	public static boolean hasLooseTorch(Player player) {

		return !findLooseTorchStack(player).isEmpty();
	}


	// Client-side guess at whether the server could place a torch from a container: a torch shows in a
	// carried shulker box or bundle, or an ender chest is carried (its contents are only known to the server)
	public static boolean mayHaveContainerTorch(Player player) {

		if (!isPlainPickaxe(player.getMainHandItem())) {
			return false;
		}

		if (CarriedItemSearch.isCarryingEnderChest(player)) {
			return true;
		}

		return CarriedItemSearch.getSources(player, CarriedItemSearch.NO_SKIPPED_SLOT, true).stream()
			.filter(source -> !source.isLooseSlot())
			.anyMatch(source -> !source.peek(PickaxeTorchPlacement::isPlaceableTorch).isEmpty());
	}


	// BlockItem.place plays its sound for everyone except the placing player, because that
	// player's client normally plays it during prediction. A vanilla client predicted nothing,
	// so it gets the sound sent directly.
	public static void sendPlaceSoundToPlayer(ServerPlayer player, BlockPos placedPos) {

		BlockState placedState = player.level().getBlockState(placedPos);
		SoundType soundType = placedState.getSoundType();

		player.connection.send(new ClientboundSoundPacket(
			BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundType.getPlaceSound()),
			SoundSource.BLOCKS,
			placedPos.getX() + 0.5,
			placedPos.getY() + 0.5,
			placedPos.getZ() + 0.5,
			(soundType.getVolume() + PLACE_SOUND_VOLUME_OFFSET) / PLACE_SOUND_VOLUME_DIVISOR,
			soundType.getPitch() * PLACE_SOUND_PITCH_SCALE,
			player.getRandom().nextLong()
		));
	}


	// Where a torch would go for this click: the clicked block if it is replaceable (like grass), else the face's neighbor
	public static BlockPos getPlacementPos(Player player, InteractionHand hand, BlockHitResult hitResult) {

		return new BlockPlaceContext(player, hand, ItemStack.EMPTY, hitResult).getClickedPos();
	}



	// A pickaxe (by the item tag, so modded pickaxes count) that has no right-click behavior of its own
	private static boolean isPlainPickaxe(ItemStack stack) {

		if (!stack.is(ItemTags.PICKAXES)) {
			return false;
		}

		// Component-driven right-click uses (eating, blocking) that base Item.use handles
		if (stack.has(DataComponents.CONSUMABLE) || stack.has(DataComponents.BLOCKS_ATTACKS)) {
			return false;
		}

		return !OVERRIDES_RIGHT_CLICK.get(stack.getItem().getClass());
	}


	// ItemStack.useOn applies adventure-mode rules, then BlockItem picks wall or floor torch,
	// consumes one (skipped in creative), plays the sound, and fires the placement triggers
	private static InteractionResult placeTorch(Player player, InteractionHand hand, BlockHitResult hitResult, ItemStack torchStack) {

		return torchStack.useOn(new BlockPlaceContext(player, hand, torchStack, hitResult));
	}


	// Places a one-torch copy, then takes the real torch out of its container only if the copy was used
	// up. A failed placement (or creative mode, which uses nothing) leaves the container untouched.
	private static InteractionResult placeTorchFromContainers(Player player, InteractionHand hand, BlockHitResult hitResult) {

		for (ItemSource source : CarriedItemSearch.getSources(player, CarriedItemSearch.NO_SKIPPED_SLOT, true)) {
			ItemStack storedTorch = source.peek(PickaxeTorchPlacement::isPlaceableTorch);
			if (storedTorch.isEmpty()) {
				continue;
			}

			ItemStack placedTorch = storedTorch.copyWithCount(1);
			InteractionResult result = placeTorch(player, hand, hitResult, placedTorch);
			if (placedTorch.isEmpty()) {
				source.take(stack -> ItemStack.isSameItemSameComponents(storedTorch, stack), 1);
			}
			return result;
		}

		return InteractionResult.PASS;
	}


	// Off-hand first (so a held torch is the one used), then hotbar, then the rest of the inventory
	private static ItemStack findLooseTorchStack(Player player) {

		ItemStack offhandStack = player.getOffhandItem();
		if (isPlaceableTorch(offhandStack)) {
			return offhandStack;
		}

		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (isPlaceableTorch(stack)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}


	private static boolean isPlaceableTorch(ItemStack stack) {

		return !stack.isEmpty() && stack.is(PLACEABLE_TORCHES) && stack.getItem() instanceof BlockItem;
	}


	private static boolean overridesItemMethod(Class<?> itemClass, String methodName, Class<?>... parameterTypes) {

		try {
			return itemClass.getMethod(methodName, parameterTypes).getDeclaringClass() != Item.class;
		} catch (NoSuchMethodException exception) {
			// Only possible if Item's own signature changed; treat the item as plain rather than disabling the feature
			VanillaExtras.LOGGER.warn("Could not find Item.{} while checking {}", methodName, itemClass.getName());
			return false;
		}
	}
}
