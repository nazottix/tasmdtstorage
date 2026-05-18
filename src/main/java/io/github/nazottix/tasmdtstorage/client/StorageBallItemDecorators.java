package io.github.nazottix.tasmdtstorage.client;

import io.github.nazottix.tasmdtstorage.item.StorageBallItem;
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
public class StorageBallItemDecorators {
    @SubscribeEvent
    public static void registerDecorators(RegisterItemDecorationsEvent event) {
        event.register(tasmdtstorage.STORAGE_BALL_ITEM.get(), StorageBallItemDecorators::renderStoredItem);
    }

    private static boolean renderStoredItem(GuiGraphics guiGraphics, Font font, ItemStack storage_ball, int x, int y) {
        renderGlintBoost(guiGraphics, storage_ball, x, y);
        renderModeBadge(guiGraphics, x, y);

        Item stored = StorageBallItem.getStoredItem(storage_ball);
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

    private static void renderGlintBoost(GuiGraphics guiGraphics, ItemStack storage_ball, int x, int y) {
        if (!StorageBallItem.isAutoInsertGlint(storage_ball)) {
            return;
        }
        guiGraphics.fill(x, y, x + 16, y + 16, 0x22A78BFA);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, 0x1198E4FF);
    }

    private static void renderModeBadge(GuiGraphics guiGraphics, int x, int y) {
        int border = 0xFF111827;
        int color = StorageBallKeyInputHandler.getModeColor();

        guiGraphics.fill(x, y, x + 7, y + 7, border);
        guiGraphics.fill(x + 1, y + 1, x + 6, y + 6, color);

        if (StorageBallKeyInputHandler.isModeActive()) {
            guiGraphics.fill(x + 2, y + 2, x + 5, y + 5, 0xAAFFFFFF);
        }
    }
}



