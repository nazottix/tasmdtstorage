package io.github.nazottix.tasmdtstorage;

import io.github.nazottix.tasmdtstorage.item.StorageTalismanItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = tasmdtstorage.MODID)
public class StorageTalismanEvents {
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (!crafted.is(tasmdtstorage.STORAGE_TALISMAN_ITEM.get())) {
            return;
        }

        int ingredientCount = 0;
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            if (!event.getInventory().getItem(i).isEmpty()) {
                ingredientCount++;
            }
        }

        if (ingredientCount != 1) {
            return;
        }

        Item stored = StorageTalismanItem.getStoredItem(crafted);
        int extracted = StorageTalismanItem.extractOneStack(crafted);
        if (stored == null || extracted <= 0) {
            return;
        }

        ItemStack extractedStack = new ItemStack(stored, extracted);
        Player player = event.getEntity();
        if (!player.getInventory().add(extractedStack)) {
            player.drop(extractedStack, false);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(tasmdtstorage.STORAGE_TALISMAN_ITEM.get())) {
            return;
        }

        Item stored = StorageTalismanItem.getStoredItem(stack);
        if (stored == null) {
            event.getToolTip().add(Component.translatable("item.tasmdtstorage.storage_talisman.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }

        int storedCount = StorageTalismanItem.getStoredCount(stack);
        int capacity = StorageTalismanItem.getCapacity(stack, stored);
        event.getToolTip().add(Component.translatable("item.tasmdtstorage.storage_talisman.bound", stored.getDescription()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("item.tasmdtstorage.storage_talisman.stored", storedCount, capacity).withStyle(ChatFormatting.GRAY));
    }
}
