package com.kevin2holt.vanillaextras.inventory;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

// One place items can be taken from: an inventory or ender chest slot, or the stored contents of
// a carried shulker box or bundle.
public interface ItemSource {

	// A copy of the first stack here that passes the filter, or EMPTY
	ItemStack peek(Predicate<ItemStack> filter);


	// Removes up to maxCount items from the first stack here that passes the filter and returns
	// them (EMPTY if none). Never mixes two stored stacks, so the result has one set of components.
	ItemStack take(Predicate<ItemStack> filter, int maxCount);


	// True for a plain slot, whose stack can be used in place instead of through take()
	boolean isLooseSlot();
}
