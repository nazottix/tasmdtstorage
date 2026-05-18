package io.github.nazottix.tasmdtstorage.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class StorageBallItemHandler implements IItemHandlerModifiable {
    private final ItemStack storage_ball;

    public StorageBallItemHandler(ItemStack storage_ball) {
        this.storage_ball = storage_ball;
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
        Item stored = StorageBallItem.getStoredItem(storage_ball);
        int count = StorageBallItem.getStoredCount(storage_ball);
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
        int inserted = StorageBallItem.insert(storage_ball, one);
        if (inserted <= 0) {
            return stack;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(inserted);
        if (simulate) {
            StorageBallItem.extractAmount(storage_ball, inserted);
        }
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        Item stored = StorageBallItem.getStoredItem(storage_ball);
        int storedCount = StorageBallItem.getStoredCount(storage_ball);
        if (stored == null || storedCount <= 0) {
            return ItemStack.EMPTY;
        }

        int extracted = Math.min(amount, storedCount);
        if (!simulate) {
            StorageBallItem.extractAmount(storage_ball, extracted);
        }
        return new ItemStack(stored, extracted);
    }

    @Override
    public int getSlotLimit(int slot) {
        Item stored = StorageBallItem.getStoredItem(storage_ball);
        return stored == null ? 64 : stored.getDefaultMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot != 0 || stack.isEmpty()) {
            return false;
        }
        Item stored = StorageBallItem.getStoredItem(storage_ball);
        return stored == null || stored == stack.getItem();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        StorageBallItem.clearStored(storage_ball);
        if (!stack.isEmpty()) {
            StorageBallItem.insert(storage_ball, stack.copy());
        }
    }
}




