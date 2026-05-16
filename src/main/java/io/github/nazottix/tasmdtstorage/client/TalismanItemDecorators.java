package io.github.nazottix.tasmdtstorage.client;

import io.github.nazottix.tasmdtstorage.item.StorageTalismanItem;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

@EventBusSubscriber(modid = tasmdtstorage.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class TalismanItemDecorators {
    @SubscribeEvent
    public static void registerDecorators(RegisterItemDecorationsEvent event) {
        event.register(tasmdtstorage.STORAGE_TALISMAN_ITEM.get(), TalismanItemDecorators::renderStoredItem);
    }

    private static boolean renderStoredItem(GuiGraphics guiGraphics, Font font, ItemStack talisman, int x, int y) {
        Item stored = StorageTalismanItem.getStoredItem(talisman);
        if (stored == null) {
            return false;
        }

        ItemStack iconStack = new ItemStack(stored);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        guiGraphics.pose().translate(x + 9.0F, y + 9.0F, 0.0F);
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        guiGraphics.renderItem(iconStack, 0, 0);
        guiGraphics.pose().popPose();
        return false;
    }
}
