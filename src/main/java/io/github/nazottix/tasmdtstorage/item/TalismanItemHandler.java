package io.github.nazottix.tasmdtstorage.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class TalismanItemHandler implements IItemHandlerModifiable {
    private final ItemStack talisman;

    public TalismanItemHandler(ItemStack talisman) {
        this.talisman = talisman;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        Item stored = StorageTalismanItem.getStoredItem(talisman);
        int count = StorageTalismanItem.getStoredCount(talisman);
        if (stored == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(stored, Math.min(count, stored.getDefaultMaxStackSize()));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty()) {
            return stack;
        }

        ItemStack one = stack.copy();
        int inserted = StorageTalismanItem.insert(talisman, one);
        if (inserted <= 0) {
            return stack;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(inserted);
        if (simulate) {
            StorageTalismanItem.extractAmount(talisman, inserted);
        }
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        Item stored = StorageTalismanItem.getStoredItem(talisman);
        int storedCount = StorageTalismanItem.getStoredCount(talisman);
        if (stored == null || storedCount <= 0) {
            return ItemStack.EMPTY;
        }

        int extracted = Math.min(amount, storedCount);
        if (!simulate) {
            StorageTalismanItem.extractAmount(talisman, extracted);
        }
        return new ItemStack(stored, extracted);
    }

    @Override
    public int getSlotLimit(int slot) {
        Item stored = StorageTalismanItem.getStoredItem(talisman);
        return stored == null ? 64 : stored.getDefaultMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot != 0 || stack.isEmpty()) {
            return false;
        }
        Item stored = StorageTalismanItem.getStoredItem(talisman);
        return stored == null || stored == stack.getItem();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        StorageTalismanItem.clearStored(talisman);
        if (!stack.isEmpty()) {
            StorageTalismanItem.insert(talisman, stack.copy());
        }
    }
}

