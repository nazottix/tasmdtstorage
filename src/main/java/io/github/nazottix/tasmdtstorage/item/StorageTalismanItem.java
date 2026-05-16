package io.github.nazottix.tasmdtstorage.item;

import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class StorageTalismanItem extends Item {
    private static final String KEY_STORED_ITEM = "stored_item";
    private static final String KEY_STORED_COUNT = "stored_count";
    private static final String KEY_MAX_STACKS = "max_stacks";

    public static final int DEFAULT_MAX_STACKS = 32;
    public static final int UPGRADE_STACKS = 16;

    public StorageTalismanItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        Item storedItem = getStoredItem(stack);
        if (storedItem == null || isFull(stack, storedItem)) {
            return;
        }

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (i == slotId) {
                continue;
            }

            ItemStack inventoryStack = player.getInventory().items.get(i);
            if (inventoryStack.isEmpty() || inventoryStack.getItem() != storedItem) {
                continue;
            }

            int inserted = insert(stack, inventoryStack);
            if (inserted > 0) {
                inventoryStack.shrink(inserted);
                if (isFull(stack, storedItem)) {
                    break;
                }
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack talisman = context.getItemInHand();
        Item storedItem = getStoredItem(talisman);
        if (!(storedItem instanceof BlockItem blockItem) || getStoredCount(talisman) <= 0) {
            return InteractionResult.PASS;
        }

        ItemStack placeStack = new ItemStack(storedItem);
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        InteractionResult result = blockItem.place(placeContext);
        if (result.consumesAction()) {
            setStoredCount(talisman, getStoredCount(talisman) - 1);
            return result;
        }
        return InteractionResult.PASS;
    }

    public static boolean isBound(ItemStack talisman) {
        return getStoredItem(talisman) != null;
    }

    public static Item getStoredItem(ItemStack talisman) {
        CompoundTag tag = getTag(talisman);
        if (!tag.contains(KEY_STORED_ITEM)) {
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY_STORED_ITEM));
        if (id == null) {
            return null;
        }

        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    public static int getStoredCount(ItemStack talisman) {
        return getTag(talisman).getInt(KEY_STORED_COUNT);
    }

    public static int getMaxStacks(ItemStack talisman) {
        CompoundTag tag = getTag(talisman);
        return Math.max(1, tag.contains(KEY_MAX_STACKS) ? tag.getInt(KEY_MAX_STACKS) : DEFAULT_MAX_STACKS);
    }

    public static void upgrade(ItemStack talisman) {
        setMaxStacks(talisman, getMaxStacks(talisman) + UPGRADE_STACKS);
    }

    public static void setMaxStacks(ItemStack talisman, int value) {
        CompoundTag tag = getOrCreateTag(talisman);
        tag.putInt(KEY_MAX_STACKS, Math.max(1, value));
        setTag(talisman, tag);
    }

    public static int extractOneStack(ItemStack talisman) {
        Item stored = getStoredItem(talisman);
        if (stored == null) {
            return 0;
        }

        int maxPerStack = stored.getDefaultMaxStackSize();
        int extracted = Math.min(maxPerStack, getStoredCount(talisman));
        setStoredCount(talisman, getStoredCount(talisman) - extracted);
        return extracted;
    }

    public static int insert(ItemStack talisman, ItemStack source) {
        if (source.isEmpty()) {
            return 0;
        }

        Item sourceItem = source.getItem();
        Item storedItem = getStoredItem(talisman);

        if (storedItem == null) {
            bindItem(talisman, sourceItem);
            storedItem = sourceItem;
        }

        if (storedItem != sourceItem) {
            return 0;
        }

        int free = getCapacity(talisman, storedItem) - getStoredCount(talisman);
        if (free <= 0) {
            return 0;
        }

        int inserted = Math.min(free, source.getCount());
        setStoredCount(talisman, getStoredCount(talisman) + inserted);
        return inserted;
    }

    public static boolean isFull(ItemStack talisman, Item storedItem) {
        return getStoredCount(talisman) >= getCapacity(talisman, storedItem);
    }

    public static int getCapacity(ItemStack talisman, Item storedItem) {
        return getMaxStacks(talisman) * storedItem.getDefaultMaxStackSize();
    }

    private static void bindItem(ItemStack talisman, Item item) {
        CompoundTag tag = getOrCreateTag(talisman);
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        tag.putString(KEY_STORED_ITEM, key.toString());
        if (!tag.contains(KEY_MAX_STACKS)) {
            tag.putInt(KEY_MAX_STACKS, DEFAULT_MAX_STACKS);
        }
        setTag(talisman, tag);
    }

    private static void setStoredCount(ItemStack talisman, int newCount) {
        CompoundTag tag = getOrCreateTag(talisman);
        tag.putInt(KEY_STORED_COUNT, Math.max(0, newCount));
        setTag(talisman, tag);
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        return getTag(stack);
    }

    private static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
