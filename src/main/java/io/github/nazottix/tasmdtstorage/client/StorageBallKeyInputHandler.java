package io.github.nazottix.tasmdtstorage.client;

import io.github.nazottix.tasmdtstorage.item.StorageBallItem;
import io.github.nazottix.tasmdtstorage.network.SetInventoryAutoInsertPayload;
import io.github.nazottix.tasmdtstorage.network.StorageBlockTransferPayload;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = tasmdtstorage.MODID, value = Dist.CLIENT)
public class StorageBallKeyInputHandler {
    private static TransferMode mode = TransferMode.NONE;
    private static boolean inventoryAutoInsertEnabled = true;
    private static boolean pressing;
    private static boolean comboHandled;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetPressState();
            return;
        }

        boolean hasStorageBall = StorageBallItem.isHoldingStorageBall(minecraft.player);
        boolean down = StorageBallKeyMappings.TOGGLE_TALISMAN_PICKUP.isDown();
        boolean shiftDown = minecraft.player.isShiftKeyDown();
        if (down) {
            if (!pressing) {
                pressing = true;
                comboHandled = false;
            }
            if (!comboHandled && shiftDown && hasStorageBall) {
                inventoryAutoInsertEnabled = !inventoryAutoInsertEnabled;
                PacketDistributor.sendToServer(new SetInventoryAutoInsertPayload(inventoryAutoInsertEnabled));
                comboHandled = true;
            }
        } else if (pressing) {
            if (!comboHandled && hasStorageBall) {
                mode = mode.next();
                minecraft.player.displayClientMessage(Component.translatable(mode.messageKey()).withStyle(mode.chatColor()), true);
            }
            resetPressState();
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (mode == TransferMode.NONE || !event.isUseItem()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !StorageBallItem.isHoldingStorageBall(minecraft.player)) {
            return;
        }

        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            return;
        }

        PacketDistributor.sendToServer(new StorageBlockTransferPayload(blockHit.getBlockPos(), blockHit.getDirection(), mode.action));
        event.setCanceled(true);
    }

    private static void resetPressState() {
        pressing = false;
        comboHandled = false;
    }

    private enum TransferMode {
        NONE(null, "message.tasmdtstorage.mode.none", ChatFormatting.GRAY),
        STORAGE_TO_TALISMAN(StorageBlockTransferPayload.Action.STORE_TO_TALISMAN, "message.tasmdtstorage.mode.storage_to_storage_ball", ChatFormatting.GREEN),
        TALISMAN_TO_STORAGE(StorageBlockTransferPayload.Action.EXTRACT_TO_STORAGE, "message.tasmdtstorage.mode.storage_ball_to_storage", ChatFormatting.BLUE);

        private final StorageBlockTransferPayload.Action action;
        private final String messageKey;
        private final ChatFormatting chatColor;

        TransferMode(StorageBlockTransferPayload.Action action, String messageKey, ChatFormatting chatColor) {
            this.action = action;
            this.messageKey = messageKey;
            this.chatColor = chatColor;
        }

        private TransferMode next() {
            return switch (this) {
                case NONE -> STORAGE_TO_TALISMAN;
                case STORAGE_TO_TALISMAN -> TALISMAN_TO_STORAGE;
                case TALISMAN_TO_STORAGE -> NONE;
            };
        }

        private String messageKey() {
            return messageKey;
        }

        private ChatFormatting chatColor() {
            return chatColor;
        }
    }

    public static int getModeColor() {
        return switch (mode) {
            case NONE -> 0xFF6B7280;
            case STORAGE_TO_TALISMAN -> 0xFF22C55E;
            case TALISMAN_TO_STORAGE -> 0xFF3B82F6;
        };
    }

    public static boolean isModeActive() {
        return mode != TransferMode.NONE;
    }

    public static boolean isInventoryAutoInsertEnabled() {
        return inventoryAutoInsertEnabled;
    }
}




