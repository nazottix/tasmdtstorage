package io.github.nazottix.tasmdtstorage.recipe;

import io.github.nazottix.tasmdtstorage.item.StorageTalismanItem;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class StorageTalismanRecipe extends CustomRecipe {
    public StorageTalismanRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return getMode(input) != Mode.INVALID;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack talisman = findTalisman(input);
        if (talisman.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = talisman.copy();
        result.setCount(1);

        Mode mode = getMode(input);
        if (mode == Mode.INSERT) {
            ItemStack other = findFirstNonTalisman(input);
            StorageTalismanItem.insert(result, other);
        } else if (mode == Mode.UPGRADE) {
            StorageTalismanItem.upgrade(result);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return tasmdtstorage.STORAGE_TALISMAN_RECIPE_SERIALIZER.get();
    }

    private static Mode getMode(CraftingInput input) {
        int talismanCount = 0;
        int nonEmpty = 0;
        ItemStack other = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            nonEmpty++;
            if (stack.is(tasmdtstorage.STORAGE_TALISMAN_ITEM.get())) {
                talismanCount++;
            } else {
                if (!other.isEmpty()) {
                    return Mode.INVALID;
                }
                other = stack;
            }
        }

        if (talismanCount != 1) {
            return Mode.INVALID;
        }

        if (nonEmpty == 1) {
            return Mode.EXTRACT;
        }

        if (nonEmpty != 2 || other.isEmpty()) {
            return Mode.INVALID;
        }

        if (other.is(Items.NETHER_STAR)) {
            return Mode.UPGRADE;
        }

        if (other.is(tasmdtstorage.STORAGE_TALISMAN_ITEM.get())) {
            return Mode.INVALID;
        }

        ItemStack talisman = findTalisman(input);
        if (talisman.isEmpty()) {
            return Mode.INVALID;
        }

        Item stored = StorageTalismanItem.getStoredItem(talisman);
        if (stored == null) {
            return Mode.INSERT;
        }

        return stored == other.getItem() ? Mode.INSERT : Mode.INVALID;
    }

    private static ItemStack findTalisman(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(tasmdtstorage.STORAGE_TALISMAN_ITEM.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findFirstNonTalisman(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && !stack.is(tasmdtstorage.STORAGE_TALISMAN_ITEM.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private enum Mode {
        INSERT,
        EXTRACT,
        UPGRADE,
        INVALID
    }
}
