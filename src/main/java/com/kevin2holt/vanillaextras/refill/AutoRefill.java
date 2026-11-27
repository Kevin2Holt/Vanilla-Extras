package com.kevin2holt.vanillaextras.refill;

import com.kevin2holt.vanillaextras.feature.ModFeatures;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ContainerComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

// Server half of auto-refill. A modded client notices its held stack running low and sends a
// RefillRequestPayload; this moves matching items into that slot from the player's own items.
// Items are only moved, never created, so a forged request can at worst tidy the sender's inventory.
//
// Search order: hotbar, main inventory, shulker boxes, bundles, ender chest. Containers are only
// opened one layer deep: a bundle inside a shulker box, or anything inside the ender chest's
// shulker boxes, is never touched.
public final class AutoRefill {
	private static final int HOTBAR_FIRST_SLOT = 0;
	private static final int HOTBAR_END_SLOT = 9;// exclusive
	private static final int MAIN_INVENTORY_END_SLOT = 36;// exclusive


	private AutoRefill() {
	}


	public static void register() {

		PayloadTypeRegistry.serverboundPlay().register(RefillRequestPayload.TYPE, RefillRequestPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RefillRequestPayload.TYPE, (payload, context) -> {
			handleRefillRequest(context.player(), payload);
		});
	}



	private static void handleRefillRequest(ServerPlayer player, RefillRequestPayload request) {

		if (!ModFeatures.AUTO_REFILL.isEnabled() || player.isCreative() || player.isSpectator()) {
			return;
		}

		int targetSlot = request.inventorySlot();
		if (!isRefillableSlot(targetSlot) || request.template().isEmpty()) {
			return;
		}

		Inventory inventory = player.getInventory();
		ItemStack template = request.template().copyWithCount(1);
		ItemStack targetStack = inventory.getItem(targetSlot);

		// The slot changed since the client asked (another mod refilled it, or the player swapped items)
		if (!targetStack.isEmpty() && !RefillRules.isRefillMatch(template, targetStack)) {
			return;
		}

		int currentCount = targetStack.getCount();
		int maxStackSize = template.getMaxStackSize();
		int triggerCount = RefillRules.clampTrigger(request.triggerCount());
		int refillAmount = RefillRules.clampAmount(request.refillAmount());

		// Rechecked against the server's copy: this is also where yielding to other refill mods pays off
		if (!RefillRules.shouldRefill(currentCount, maxStackSize, triggerCount)) {
			return;
		}

		int itemsWanted = RefillRules.getTargetCount(currentCount, maxStackSize, triggerCount, refillAmount) - currentCount;
		ItemStack pulledItems = pullItems(player, template, targetSlot, itemsWanted, request.isFromContainersAllowed());
		if (pulledItems.isEmpty()) {
			return;
		}

		if (targetStack.isEmpty()) {
			inventory.setItem(targetSlot, pulledItems);
		} else {
			targetStack.grow(pulledItems.getCount());
		}
		inventory.setChanged();
	}


	private static boolean isRefillableSlot(int inventorySlot) {

		return Inventory.isHotbarSlot(inventorySlot) || inventorySlot == Inventory.SLOT_OFFHAND;
	}


	// Returns everything gathered as one stack (empty if nothing matched)
	private static ItemStack pullItems(ServerPlayer player, ItemStack template, int targetSlot, int itemsWanted, boolean isFromContainersAllowed) {

		Inventory inventory = player.getInventory();
		ItemStack gathered = ItemStack.EMPTY;

		gathered = pullFromSlots(inventory, HOTBAR_FIRST_SLOT, HOTBAR_END_SLOT, targetSlot, template, itemsWanted, gathered);
		gathered = pullFromSlots(inventory, HOTBAR_END_SLOT, MAIN_INVENTORY_END_SLOT, targetSlot, template, itemsWanted, gathered);

		if (!isFromContainersAllowed) {
			return gathered;
		}

		List<ItemStack> carriedStacks = getCarriedStacks(inventory, targetSlot);

		for (ItemStack shulkerBox : carriedStacks) {
			if (isShulkerBox(shulkerBox)) {
				gathered = pullFromComponent(shulkerBox, DataComponents.CONTAINER, template, itemsWanted, gathered);
			}
		}

		for (ItemStack bundle : carriedStacks) {
			if (bundle.has(DataComponents.BUNDLE_CONTENTS)) {
				gathered = pullFromComponent(bundle, DataComponents.BUNDLE_CONTENTS, template, itemsWanted, gathered);
			}
		}

		boolean isCarryingEnderChest = carriedStacks.stream().anyMatch(stack -> stack.getItem() == Items.ENDER_CHEST);
		if (isCarryingEnderChest) {
			Container enderChest = player.getEnderChestInventory();
			gathered = pullFromSlots(enderChest, 0, enderChest.getContainerSize(), -1, template, itemsWanted, gathered);
		}

		return gathered;
	}


	// Hotbar, main inventory, and off hand: every slot a carried container could sit in
	private static List<ItemStack> getCarriedStacks(Inventory inventory, int targetSlot) {

		List<ItemStack> carriedStacks = new ArrayList<>();

		for (int slot = HOTBAR_FIRST_SLOT; slot < MAIN_INVENTORY_END_SLOT; slot++) {
			if (slot != targetSlot) {
				carriedStacks.add(inventory.getItem(slot));
			}
		}
		if (targetSlot != Inventory.SLOT_OFFHAND) {
			carriedStacks.add(inventory.getItem(Inventory.SLOT_OFFHAND));
		}

		return carriedStacks;
	}


	private static boolean isShulkerBox(ItemStack stack) {

		return Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
	}



	// Takes matching items from container slots [firstSlot, endSlot), skipping skippedSlot
	private static ItemStack pullFromSlots(Container container, int firstSlot, int endSlot, int skippedSlot, ItemStack template, int itemsWanted, ItemStack gathered) {

		for (int slot = firstSlot; slot < endSlot && gathered.getCount() < itemsWanted; slot++) {
			ItemStack sourceStack = container.getItem(slot);
			if (slot == skippedSlot || !RefillRules.isRefillMatch(template, sourceStack)) {
				continue;
			}

			gathered = takeFromStack(sourceStack, itemsWanted, gathered);
			container.setChanged();
		}

		return gathered;
	}


	// Takes matching items out of an item's stored contents (shulker box or bundle) and writes the
	// shrunken contents back onto that item. Only the top layer is looked at.
	private static <T extends ContainerComponent<T>> ItemStack pullFromComponent(ItemStack containerItem, DataComponentType<T> componentType, ItemStack template, int itemsWanted, ItemStack gathered) {

		T contents = containerItem.get(componentType);
		if (contents == null || gathered.getCount() >= itemsWanted) {
			return gathered;
		}

		List<ItemStack> storedStacks = new ArrayList<>(contents.itemCopies().toList());
		boolean isChanged = false;

		for (ItemStack storedStack : storedStacks) {
			if (gathered.getCount() >= itemsWanted) {
				break;
			}
			if (RefillRules.isRefillMatch(template, storedStack)) {
				gathered = takeFromStack(storedStack, itemsWanted, gathered);
				isChanged = true;
			}
		}

		if (isChanged) {
			// Shulker boxes keep emptied slots so the other items stay put; bundles have no slot positions
			boolean isSlotPositionKept = componentType == DataComponents.CONTAINER;
			containerItem.set(componentType, contents.copyWithContents(storedStacks.stream().filter(stack -> isSlotPositionKept || !stack.isEmpty())));
		}

		return gathered;
	}


	// Moves up to (itemsWanted - gathered) items out of sourceStack into the gathered stack.
	// The first source decides the exact components of the result (relevant for damaged tools).
	private static ItemStack takeFromStack(ItemStack sourceStack, int itemsWanted, ItemStack gathered) {

		if (!gathered.isEmpty() && !ItemStack.isSameItemSameComponents(gathered, sourceStack)) {
			return gathered;
		}

		int itemsToTake = Math.min(itemsWanted - gathered.getCount(), sourceStack.getCount());
		if (itemsToTake <= 0) {
			return gathered;
		}

		ItemStack taken = sourceStack.split(itemsToTake);
		if (gathered.isEmpty()) {
			return taken;
		}

		gathered.grow(taken.getCount());
		return gathered;
	}
}
