package io.github.nazottix.tasmdtstorage.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class StorageTalismanItem extends Item {
    private static final String KEY_STORED_ITEM = "stored_item";
    private static final String KEY_STORED_COUNT = "stored_count";
    private static final String KEY_MAX_STACKS = "max_stacks";
    private static final String KEY_AUTO_INSERT_ENABLED = "tasmdtstorage_inventory_auto_insert_enabled";

    public static final int DEFAULT_MAX_STACKS = 32;
    public static final int UPGRADE_STACKS = 16;

    public StorageTalismanItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || !isInventoryAutoInsertEnabled(player)) {
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
    public Component getName(ItemStack stack) {
        Item stored = getStoredItem(stack);
        if (stored == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.tasmdtstorage.storage_talisman.named", stored.getDescription());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack talisman = context.getItemInHand();
        Item storedItem = getStoredItem(talisman);
        Player player = context.getPlayer();
        if (storedItem == null || getStoredCount(talisman) <= 0 || player == null) {
            return InteractionResult.PASS;
        }

        ItemStack proxyStack = new ItemStack(storedItem, 1);
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        UseOnContext proxyContext = new UseOnContext(context.getLevel(), player, context.getHand(), proxyStack, hit);
        InteractionResult result = storedItem.useOn(proxyContext);
        int consumed = 1 - proxyStack.getCount();
        if (result.consumesAction() && consumed > 0) {
            setStoredCount(talisman, getStoredCount(talisman) - consumed);
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack talisman = player.getItemInHand(usedHand);
        Item storedItem = getStoredItem(talisman);
        if (storedItem == null || getStoredCount(talisman) <= 0) {
            return InteractionResultHolder.pass(talisman);
        }

        ItemStack proxyStack = new ItemStack(storedItem, 1);
        if (proxyStack.getFoodProperties(player) != null) {
            if (!player.canEat(proxyStack.getFoodProperties(player).canAlwaysEat())) {
                return InteractionResultHolder.fail(talisman);
            }
            return ItemUtils.startUsingInstantly(level, player, usedHand);
        }

        InteractionResultHolder<ItemStack> result = storedItem.use(level, player, usedHand);
        int consumed = 1 - result.getObject().getCount();
        if (!level.isClientSide && result.getResult().consumesAction() && consumed > 0) {
            setStoredCount(talisman, getStoredCount(talisman) - consumed);
            if (!result.getObject().isEmpty() && result.getObject().getItem() != storedItem && !player.getInventory().add(result.getObject())) {
                player.drop(result.getObject(), false);
            }
        }

        return result.getResult().consumesAction()
                ? new InteractionResultHolder<>(result.getResult(), talisman)
                : InteractionResultHolder.pass(talisman);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        Item stored = getStoredItem(stack);
        if (stored != null) {
            ItemStack proxy = new ItemStack(stored, 1);
            if (proxy.getFoodProperties(entity) != null) {
                return proxy.getUseDuration(entity);
            }
        }
        return super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        Item stored = getStoredItem(stack);
        if (stored != null) {
            ItemStack proxy = new ItemStack(stored, 1);
            if (proxy.getFoodProperties(null) != null) {
                return proxy.getUseAnimation();
            }
        }
        return super.getUseAnimation(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Item stored = getStoredItem(stack);
        if (stored == null || getStoredCount(stack) <= 0) {
            return stack;
        }

        ItemStack proxy = new ItemStack(stored, 1);
        if (proxy.getFoodProperties(livingEntity) == null) {
            return stack;
        }

        ItemStack remainder = proxy.finishUsingItem(level, livingEntity);
        if (!level.isClientSide) {
            extractAmount(stack, 1);
            if (!remainder.isEmpty() && remainder.getItem() != stored && livingEntity instanceof Player player && !player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }
        return stack;
    }

    public static boolean hasAnyTalisman(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof StorageTalismanItem) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHoldingTalisman(Player player) {
        return player.getMainHandItem().getItem() instanceof StorageTalismanItem
                || player.getOffhandItem().getItem() instanceof StorageTalismanItem;
    }

    public static boolean isInventoryAutoInsertEnabled(Player player) {
        if (!player.getPersistentData().contains(KEY_AUTO_INSERT_ENABLED)) {
            return true;
        }
        return player.getPersistentData().getBoolean(KEY_AUTO_INSERT_ENABLED);
    }

    public static void setInventoryAutoInsertEnabled(Player player, boolean enabled) {
        player.getPersistentData().putBoolean(KEY_AUTO_INSERT_ENABLED, enabled);
    }

    public static int insertIntoAnyTalisman(Player player, ItemStack source) {
        if (source.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (ItemStack candidate : player.getInventory().items) {
            if (!candidate.isEmpty() && candidate.getItem() instanceof StorageTalismanItem) {
                int inserted = insert(candidate, source);
                if (inserted > 0) {
                    source.shrink(inserted);
                    total += inserted;
                    if (source.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return total;
    }

    public static int extractFromAnyTalismanToContainer(Player player, Container container) {
        int movedTotal = 0;
        for (ItemStack talisman : player.getInventory().items) {
            if (talisman.isEmpty() || !(talisman.getItem() instanceof StorageTalismanItem)) {
                continue;
            }

            Item stored = getStoredItem(talisman);
            int storedCount = getStoredCount(talisman);
            if (stored == null || storedCount <= 0) {
                continue;
            }

            int moved = moveItemToContainer(container, stored, storedCount);
            if (moved > 0) {
                setStoredCount(talisman, storedCount - moved);
                movedTotal += moved;
            }
        }
        return movedTotal;
    }

    public static int extractAmount(ItemStack talisman, int amount) {
        int extracted = Math.min(Math.max(0, amount), getStoredCount(talisman));
        if (extracted > 0) {
            setStoredCount(talisman, getStoredCount(talisman) - extracted);
        }
        return extracted;
    }

    public static void clearStored(ItemStack talisman) {
        CompoundTag tag = getOrCreateTag(talisman);
        tag.remove(KEY_STORED_ITEM);
        tag.putInt(KEY_STORED_COUNT, 0);
        setTag(talisman, tag);
    }

    private static int moveItemToContainer(Container container, Item item, int amount) {
        int remaining = amount;

        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.isEmpty() || slotStack.getItem() != item) {
                continue;
            }

            int slotLimit = Math.min(container.getMaxStackSize(), slotStack.getMaxStackSize());
            int canMove = Math.min(remaining, slotLimit - slotStack.getCount());
            if (canMove > 0) {
                slotStack.grow(canMove);
                remaining -= canMove;
            }
        }

        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack slotStack = container.getItem(i);
            if (!slotStack.isEmpty()) {
                continue;
            }
            if (!container.canPlaceItem(i, new ItemStack(item))) {
                continue;
            }

            int move = Math.min(remaining, Math.min(container.getMaxStackSize(), item.getDefaultMaxStackSize()));
            container.setItem(i, new ItemStack(item, move));
            remaining -= move;
        }

        if (remaining < amount) {
            container.setChanged();
        }
        return amount - remaining;
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

    private static void setMaxStacks(ItemStack talisman, int value) {
        CompoundTag tag = getOrCreateTag(talisman);
        tag.putInt(KEY_MAX_STACKS, Math.max(1, value));
        setTag(talisman, tag);
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
