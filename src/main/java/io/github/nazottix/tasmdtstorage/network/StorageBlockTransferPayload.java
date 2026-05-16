package io.github.nazottix.tasmdtstorage.network;

import io.github.nazottix.tasmdtstorage.item.StorageTalismanItem;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StorageBlockTransferPayload(BlockPos pos, Action action) implements CustomPacketPayload {
    public static final Type<StorageBlockTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(tasmdtstorage.MODID, "storage_block_transfer"));
    public static final StreamCodec<ByteBuf, StorageBlockTransferPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            StorageBlockTransferPayload::pos,
            Action.STREAM_CODEC,
            StorageBlockTransferPayload::action,
            StorageBlockTransferPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageBlockTransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !StorageTalismanItem.isHoldingTalisman(player)) {
                return;
            }

            if (!player.level().isLoaded(payload.pos()) || player.blockPosition().distManhattan(payload.pos()) > 8) {
                return;
            }

            Container container = resolveContainer(player, payload.pos());
            if (container == null) {
                return;
            }

            int moved = payload.action() == Action.STORE_TO_TALISMAN
                    ? moveStorageToTalisman(player, container)
                    : StorageTalismanItem.extractFromAnyTalismanToContainer(player, container);

            if (moved > 0) {
                Component message = Component.translatable(
                        payload.action() == Action.STORE_TO_TALISMAN
                                ? "message.tasmdtstorage.storage_to_talisman"
                                : "message.tasmdtstorage.talisman_to_storage",
                        moved
                ).withStyle(ChatFormatting.GRAY);
                player.displayClientMessage(message, true);
            }
        });
    }

    private static Container resolveContainer(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, state, player.level(), pos, true);
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    private static int moveStorageToTalisman(ServerPlayer player, Container container) {
        int movedTotal = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stackInSlot = container.getItem(i);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            int moved = StorageTalismanItem.insertIntoAnyTalisman(player, stackInSlot);
            if (moved > 0) {
                movedTotal += moved;
                container.setChanged();
            }
        }
        return movedTotal;
    }

    public enum Action {
        STORE_TO_TALISMAN,
        EXTRACT_TO_STORAGE;

        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Action::ordinal);
    }
}
