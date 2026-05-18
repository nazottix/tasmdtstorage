package io.github.nazottix.tasmdtstorage.recipe;

import io.github.nazottix.tasmdtstorage.item.StorageBallItem;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class StorageBallRecipe extends CustomRecipe {
    public static final String KEY_EXTRACT_ITEM = "extract_item";
    public static final String KEY_EXTRACT_COUNT = "extract_count";

    public StorageBallRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return getMode(input) != Mode.INVALID;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack storage_ball = findStorageBall(input);
        if (storage_ball.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = storage_ball.copy();
        result.setCount(1);

        Mode mode = getMode(input);
        if (mode == Mode.INSERT) {
            ItemStack other = findFirstNonStorageBall(input);
            StorageBallItem.insert(result, other.copyWithCount(1));
        } else if (mode == Mode.EXTRACT) {
            Item stored = StorageBallItem.getStoredItem(result);
            int extracted = StorageBallItem.extractOneStack(result);
            if (stored != null && extracted > 0) {
                CompoundTag tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stored);
                tag.putString(KEY_EXTRACT_ITEM, id.toString());
                tag.putInt(KEY_EXTRACT_COUNT, extracted);
                result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        } else if (mode == Mode.UPGRADE) {
            StorageBallItem.upgrade(result);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return tasmdtstorage.STORAGE_BALL_RECIPE_SERIALIZER.get();
    }

    private static Mode getMode(CraftingInput input) {
        int storage_ballCount = 0;
        int nonEmpty = 0;
        ItemStack other = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            nonEmpty++;
            if (stack.is(tasmdtstorage.STORAGE_BALL_ITEM.get())) {
                storage_ballCount++;
            } else {
                if (!other.isEmpty()) {
                    return Mode.INVALID;
                }
                other = stack;
            }
        }

        if (storage_ballCount != 1) {
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

        if (other.is(tasmdtstorage.STORAGE_BALL_ITEM.get())) {
            return Mode.INVALID;
        }

        ItemStack storage_ball = findStorageBall(input);
        if (storage_ball.isEmpty()) {
            return Mode.INVALID;
        }

        Item stored = StorageBallItem.getStoredItem(storage_ball);
        if (stored == null) {
            return Mode.INSERT;
        }

        return stored == other.getItem() ? Mode.INSERT : Mode.INVALID;
    }

    private static ItemStack findStorageBall(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(tasmdtstorage.STORAGE_BALL_ITEM.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findFirstNonStorageBall(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && !stack.is(tasmdtstorage.STORAGE_BALL_ITEM.get())) {
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



