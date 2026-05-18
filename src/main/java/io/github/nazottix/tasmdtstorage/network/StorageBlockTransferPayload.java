package io.github.nazottix.tasmdtstorage.network;

import io.github.nazottix.tasmdtstorage.item.StorageBallItem;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StorageBlockTransferPayload(BlockPos pos, Direction side, Action action) implements CustomPacketPayload {
    public static final Type<StorageBlockTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(tasmdtstorage.MODID, "storage_block_transfer"));
    public static final StreamCodec<ByteBuf, StorageBlockTransferPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            StorageBlockTransferPayload::pos,
            ByteBufCodecs.idMapper(Direction::from3DDataValue, Direction::get3DDataValue),
            StorageBlockTransferPayload::side,
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
            if (!(context.player() instanceof ServerPlayer player) || !StorageBallItem.isHoldingStorageBall(player)) {
                return;
            }

            if (!player.level().isLoaded(payload.pos()) || player.blockPosition().distManhattan(payload.pos()) > 8) {
                return;
            }

            StorageTarget target = resolveTarget(player.level(), payload.pos(), payload.side());
            if (target == null) {
                return;
            }

            int moved = payload.action() == Action.STORE_TO_TALISMAN
                    ? moveStorageToStorageBall(player, target)
                    : extractStorageBallToStorage(player, target);

            if (moved > 0) {
                Component message = Component.translatable(
                        payload.action() == Action.STORE_TO_TALISMAN
                                ? "message.tasmdtstorage.storage_to_storage_ball"
                                : "message.tasmdtstorage.storage_ball_to_storage",
                        moved
                ).withStyle(ChatFormatting.GRAY);
                player.displayClientMessage(message, true);
            }
        });
    }

    private static StorageTarget resolveTarget(Level level, BlockPos pos, Direction clickedSide) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, clickedSide);
        if (handler == null) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, clickedSide.getOpposite());
        }
        if (handler == null) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        }
        if (handler == null) {
            for (Direction dir : Direction.values()) {
                handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
                if (handler != null) {
                    break;
                }
            }
        }
        if (handler != null) {
            return new StorageTarget(null, handler);
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            Container chestContainer = ChestBlock.getContainer(chestBlock, state, level, pos, true);
            if (chestContainer != null) {
                return new StorageTarget(chestContainer, null);
            }
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container container ? new StorageTarget(container, null) : null;
    }

    private static int moveStorageToStorageBall(ServerPlayer player, StorageTarget target) {
        if (target.handler != null) {
            int movedTotal = 0;
            for (int slot = 0; slot < target.handler.getSlots(); slot++) {
                while (true) {
                    ItemStack simulated = target.handler.extractItem(slot, 64, true);
                    if (simulated.isEmpty()) {
                        break;
                    }

                    ItemStack extracted = target.handler.extractItem(slot, simulated.getCount(), false);
                    if (extracted.isEmpty()) {
                        break;
                    }

                    ItemStack working = extracted.copy();
                    int moved = StorageBallItem.insertIntoAnyStorageBall(player, working);
                    movedTotal += moved;

                    if (!working.isEmpty()) {
                        ItemStack remainder = working;
                        for (int i = 0; i < target.handler.getSlots() && !remainder.isEmpty(); i++) {
                            remainder = target.handler.insertItem(i, remainder, false);
                        }
                    }

                    if (moved <= 0) {
                        break;
                    }
                }
            }
            return movedTotal;
        }

        if (target.container == null) {
            return 0;
        }

        int movedTotal = 0;
        for (int i = 0; i < target.container.getContainerSize(); i++) {
            ItemStack stackInSlot = target.container.getItem(i);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            int moved = StorageBallItem.insertIntoAnyStorageBall(player, stackInSlot);
            if (moved > 0) {
                movedTotal += moved;
                target.container.setChanged();
            }
        }
        return movedTotal;
    }

    private static int extractStorageBallToStorage(ServerPlayer player, StorageTarget target) {
        if (target.handler != null) {
            return StorageBallItem.extractFromAnyStorageBallToItemHandler(player, target.handler);
        }
        if (target.container != null) {
            return StorageBallItem.extractFromAnyStorageBallToContainer(player, target.container);
        }
        return 0;
    }

    private record StorageTarget(Container container, IItemHandler handler) {}

    public enum Action {
        STORE_TO_TALISMAN,
        EXTRACT_TO_STORAGE;

        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Action::ordinal);
    }
}
