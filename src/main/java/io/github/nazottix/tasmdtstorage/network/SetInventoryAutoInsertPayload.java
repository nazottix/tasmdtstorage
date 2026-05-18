package io.github.nazottix.tasmdtstorage.network;

import io.github.nazottix.tasmdtstorage.item.StorageBallItem;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetInventoryAutoInsertPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetInventoryAutoInsertPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(tasmdtstorage.MODID, "set_inventory_auto_insert"));
    public static final StreamCodec<ByteBuf, SetInventoryAutoInsertPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SetInventoryAutoInsertPayload::enabled,
            SetInventoryAutoInsertPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetInventoryAutoInsertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !StorageBallItem.isHoldingStorageBall(player)) {
                return;
            }
            StorageBallItem.setInventoryAutoInsertEnabled(player, payload.enabled());
            Component message = Component.translatable(
                    payload.enabled()
                            ? "message.tasmdtstorage.inventory_auto_insert.enabled"
                            : "message.tasmdtstorage.inventory_auto_insert.disabled"
            ).withStyle(payload.enabled() ? ChatFormatting.GREEN : ChatFormatting.RED);
            player.displayClientMessage(message, true);
        });
    }
}


