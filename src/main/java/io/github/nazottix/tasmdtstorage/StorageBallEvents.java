package io.github.nazottix.tasmdtstorage;

import io.github.nazottix.tasmdtstorage.item.StorageBallItem;
import io.github.nazottix.tasmdtstorage.recipe.StorageBallRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = tasmdtstorage.MODID)
public class StorageBallEvents {
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (!crafted.is(tasmdtstorage.STORAGE_BALL_ITEM.get())) {
            return;
        }

        CompoundTag tag = crafted.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(StorageBallRecipe.KEY_EXTRACT_ITEM) || !tag.contains(StorageBallRecipe.KEY_EXTRACT_COUNT)) {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getString(StorageBallRecipe.KEY_EXTRACT_ITEM));
        if (id == null) {
            return;
        }
        Item stored = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        int extracted = tag.getInt(StorageBallRecipe.KEY_EXTRACT_COUNT);
        if (stored == null || extracted <= 0) {
            return;
        }

        tag.remove(StorageBallRecipe.KEY_EXTRACT_ITEM);
        tag.remove(StorageBallRecipe.KEY_EXTRACT_COUNT);
        crafted.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        ItemStack extractedStack = new ItemStack(stored, extracted);
        Player player = event.getEntity();
        if (!player.getInventory().add(extractedStack)) {
            player.drop(extractedStack, false);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(tasmdtstorage.STORAGE_BALL_ITEM.get())) {
            return;
        }

        Item stored = StorageBallItem.getStoredItem(stack);
        if (stored == null) {
            event.getToolTip().add(Component.translatable("item.tasmdtstorage.storage_ball.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }

        int storedCount = StorageBallItem.getStoredCount(stack);
        int capacity = StorageBallItem.getCapacity(stack, stored);
        event.getToolTip().add(Component.translatable("item.tasmdtstorage.storage_ball.bound", stored.getDescription()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("item.tasmdtstorage.storage_ball.stored", storedCount, capacity).withStyle(ChatFormatting.GRAY));
    }
}

