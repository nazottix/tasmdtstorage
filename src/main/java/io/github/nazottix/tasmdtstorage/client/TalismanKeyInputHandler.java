package io.github.nazottix.tasmdtstorage.client;

import io.github.nazottix.tasmdtstorage.item.StorageTalismanItem;
import io.github.nazottix.tasmdtstorage.network.SetInventoryAutoInsertPayload;
import io.github.nazottix.tasmdtstorage.network.StorageBlockTransferPayload;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = tasmdtstorage.MODID, value = Dist.CLIENT)
public class TalismanKeyInputHandler {
    private static final int HOLD_TICKS = 12;

    private static TransferMode mode = TransferMode.NONE;
    private static boolean inventoryAutoInsertEnabled = true;
    private static boolean pressing;
    private static int pressTicks;
    private static boolean longPressHandled;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetPressState();
            return;
        }

        boolean hasTalisman = StorageTalismanItem.isHoldingTalisman(minecraft.player);
        boolean down = TalismanKeyMappings.TOGGLE_TALISMAN_PICKUP.isDown();
        if (down) {
            if (!pressing) {
                pressing = true;
                pressTicks = 0;
                longPressHandled = false;
            } else {
                pressTicks++;
                if (!longPressHandled && pressTicks >= HOLD_TICKS && hasTalisman) {
                    inventoryAutoInsertEnabled = !inventoryAutoInsertEnabled;
                    PacketDistributor.sendToServer(new SetInventoryAutoInsertPayload(inventoryAutoInsertEnabled));
                    longPressHandled = true;
                }
            }
        } else if (pressing) {
            if (!longPressHandled && hasTalisman) {
                mode = mode.next();
                minecraft.player.displayClientMessage(Component.translatable(mode.messageKey()).withStyle(ChatFormatting.AQUA), true);
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
        if (minecraft.player == null || minecraft.level == null || !StorageTalismanItem.isHoldingTalisman(minecraft.player)) {
            return;
        }

        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(blockHit.getBlockPos());
        if (!(blockEntity instanceof Container)) {
            return;
        }

        PacketDistributor.sendToServer(new StorageBlockTransferPayload(blockHit.getBlockPos(), mode.action));
        event.setCanceled(true);
    }

    private static void resetPressState() {
        pressing = false;
        pressTicks = 0;
        longPressHandled = false;
    }

    private enum TransferMode {
        NONE(null, "message.tasmdtstorage.mode.none"),
        STORAGE_TO_TALISMAN(StorageBlockTransferPayload.Action.STORE_TO_TALISMAN, "message.tasmdtstorage.mode.storage_to_talisman"),
        TALISMAN_TO_STORAGE(StorageBlockTransferPayload.Action.EXTRACT_TO_STORAGE, "message.tasmdtstorage.mode.talisman_to_storage");

        private final StorageBlockTransferPayload.Action action;
        private final String messageKey;

        TransferMode(StorageBlockTransferPayload.Action action, String messageKey) {
            this.action = action;
            this.messageKey = messageKey;
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
    }
}
