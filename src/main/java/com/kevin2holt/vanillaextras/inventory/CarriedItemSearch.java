package com.kevin2holt.vanillaextras.inventory;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ContainerComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// Every place a player's own items can be pulled from, in priority order. Shared by auto-refill
// and pickaxe torches so both search the same way:
//
//     1. Hotbar
//     2. Main inventory
//     3. Shulker boxes carried in the inventory
//     4. Bundles carried in the inventory
//     5. The ender chest, if the player carries an ender chest item
//     6. "Refill" shulker boxes inside that ender chest
//
// Containers are opened one layer deep. The only exception is step 6: a shulker box in the ender
// chest whose name starts with "Refill" or "Auto-Refill" (see isRefillName). Nothing inside those
// shulkers is opened, and no other container in the ender chest is.
public final class CarriedItemSearch {
	public static final int NO_SKIPPED_SLOT = -1;

	private static final int HOTBAR_FIRST_SLOT = 0;
	private static final int HOTBAR_END_SLOT = 9;// exclusive
	private static final int MAIN_INVENTORY_END_SLOT = 36;// exclusive

	private static final List<String> REFILL_NAME_PREFIXES = List.of("refill", "autorefill");
	private static final Pattern NAME_SEPARATORS = Pattern.compile("[\\s_\\-]+");


	private CarriedItemSearch() {
	}


	// skippedSlot: an inventory slot to leave out (the slot being refilled), or NO_SKIPPED_SLOT.
	// With isFromContainersAllowed false, only steps 1 and 2 are returned.
	public static List<ItemSource> getSources(Player player, int skippedSlot, boolean isFromContainersAllowed) {

		Inventory inventory = player.getInventory();
		List<ItemSource> sources = new ArrayList<>();

		addSlotSources(sources, inventory, HOTBAR_FIRST_SLOT, HOTBAR_END_SLOT, skippedSlot);
		addSlotSources(sources, inventory, HOTBAR_END_SLOT, MAIN_INVENTORY_END_SLOT, skippedSlot);

		if (!isFromContainersAllowed) {
			return sources;
		}

		List<ItemStack> carriedStacks = getCarriedStacks(inventory, skippedSlot);

		for (ItemStack stack : carriedStacks) {
			if (isShulkerBox(stack)) {
				sources.add(new ComponentSource<>(stack, DataComponents.CONTAINER));
			}
		}

		for (ItemStack stack : carriedStacks) {
			if (stack.has(DataComponents.BUNDLE_CONTENTS)) {
				sources.add(new ComponentSource<>(stack, DataComponents.BUNDLE_CONTENTS));
			}
		}

		if (isCarryingEnderChest(carriedStacks)) {
			Container enderChest = player.getEnderChestInventory();
			addSlotSources(sources, enderChest, 0, enderChest.getContainerSize(), NO_SKIPPED_SLOT);

			for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
				ItemStack stack = enderChest.getItem(slot);
				if (isShulkerBox(stack) && isRefillName(stack.get(DataComponents.CUSTOM_NAME))) {
					sources.add(new ComponentSource<>(stack, DataComponents.CONTAINER, enderChest));
				}
			}
		}

		return sources;
	}


	// True for names starting with "Refill" or "Auto-Refill", ignoring case and treating spaces,
	// underscores, and hyphens alike: "auto_refill", "AutoRefill", "Refill - Torches" all count.
	public static boolean isRefillName(Component customName) {

		if (customName == null) {
			return false;
		}

		String normalizedName = NAME_SEPARATORS.matcher(customName.getString().toLowerCase(Locale.ROOT)).replaceAll("");
		return REFILL_NAME_PREFIXES.stream().anyMatch(normalizedName::startsWith);
	}


	public static boolean isCarryingEnderChest(Player player) {

		return isCarryingEnderChest(getCarriedStacks(player.getInventory(), NO_SKIPPED_SLOT));
	}


	public static boolean isShulkerBox(ItemStack stack) {

		return Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
	}



	private static boolean isCarryingEnderChest(List<ItemStack> carriedStacks) {

		return carriedStacks.stream().anyMatch(stack -> stack.getItem() == Items.ENDER_CHEST);
	}


	// Hotbar, main inventory, and off hand: every slot a carried container could sit in
	private static List<ItemStack> getCarriedStacks(Inventory inventory, int skippedSlot) {

		List<ItemStack> carriedStacks = new ArrayList<>();

		for (int slot = HOTBAR_FIRST_SLOT; slot < MAIN_INVENTORY_END_SLOT; slot++) {
			if (slot != skippedSlot) {
				carriedStacks.add(inventory.getItem(slot));
			}
		}
		if (skippedSlot != Inventory.SLOT_OFFHAND) {
			carriedStacks.add(inventory.getItem(Inventory.SLOT_OFFHAND));
		}

		return carriedStacks;
	}


	private static void addSlotSources(List<ItemSource> sources, Container container, int firstSlot, int endSlot, int skippedSlot) {

		for (int slot = firstSlot; slot < endSlot; slot++) {
			if (slot != skippedSlot) {
				sources.add(new SlotSource(container, slot));
			}
		}
	}



	private record SlotSource(Container container, int slot) implements ItemSource {

		@Override
		public ItemStack peek(Predicate<ItemStack> filter) {

			ItemStack stack = this.container.getItem(this.slot);
			return !stack.isEmpty() && filter.test(stack) ? stack.copy() : ItemStack.EMPTY;
		}


		@Override
		public ItemStack take(Predicate<ItemStack> filter, int maxCount) {

			ItemStack stack = this.container.getItem(this.slot);
			if (stack.isEmpty() || maxCount <= 0 || !filter.test(stack)) {
				return ItemStack.EMPTY;
			}

			ItemStack taken = stack.split(maxCount);
			this.container.setChanged();
			return taken;
		}


		@Override
		public boolean isLooseSlot() {

			return true;
		}
	}



	// The stored contents of a shulker box or bundle. Contents are re-read on every call, so several
	// takes in a row stay correct, and written back onto the item after each take.
	//
	// holder: the container the item itself sits in (the ender chest), marked changed after a take; null for the inventory
	private record ComponentSource<T extends ContainerComponent<T>>(ItemStack containerItem, DataComponentType<T> componentType, Container holder) implements ItemSource {

		ComponentSource(ItemStack containerItem, DataComponentType<T> componentType) {

			this(containerItem, componentType, null);
		}


		@Override
		public ItemStack peek(Predicate<ItemStack> filter) {

			T contents = this.containerItem.get(this.componentType);
			if (contents == null) {
				return ItemStack.EMPTY;
			}

			return contents.itemCopies()
				.filter(stack -> !stack.isEmpty() && filter.test(stack))
				.findFirst()
				.orElse(ItemStack.EMPTY);
		}


		@Override
		public ItemStack take(Predicate<ItemStack> filter, int maxCount) {

			T contents = this.containerItem.get(this.componentType);
			if (contents == null || maxCount <= 0) {
				return ItemStack.EMPTY;
			}

			List<ItemStack> storedStacks = new ArrayList<>(contents.itemCopies().toList());
			for (ItemStack storedStack : storedStacks) {
				if (storedStack.isEmpty() || !filter.test(storedStack)) {
					continue;
				}

				ItemStack taken = storedStack.split(maxCount);
				this.writeBack(contents, storedStacks);
				return taken;
			}

			return ItemStack.EMPTY;
		}


		@Override
		public boolean isLooseSlot() {

			return false;
		}


		private void writeBack(T contents, List<ItemStack> storedStacks) {

			// Shulker boxes keep emptied slots so the other items stay put; bundles have no slot positions
			boolean isSlotPositionKept = this.componentType == DataComponents.CONTAINER;
			this.containerItem.set(this.componentType, contents.copyWithContents(storedStacks.stream().filter(stack -> isSlotPositionKept || !stack.isEmpty())));

			if (this.holder != null) {
				this.holder.setChanged();
			}
		}
	}
}
