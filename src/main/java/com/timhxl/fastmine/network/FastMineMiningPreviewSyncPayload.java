package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** 服务端返回的、已经过全部 FastMine 规则筛选的预览方块坐标。 */
public record FastMineMiningPreviewSyncPayload(int requestId, List<BlockPos> positions)
        implements CustomPacketPayload {
    private static final int MAX_POSITIONS = 10_000;
    public static final Type<FastMineMiningPreviewSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "mining_preview_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineMiningPreviewSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public FastMineMiningPreviewSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            int requestId = buffer.readInt();
            int count = buffer.readInt();
            if (count < 0 || count > MAX_POSITIONS) {
                throw new IllegalArgumentException("Invalid FastMine preview position count.");
            }
            List<BlockPos> positions = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                positions.add(BlockPos.of(buffer.readLong()));
            }
            return new FastMineMiningPreviewSyncPayload(requestId, List.copyOf(positions));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FastMineMiningPreviewSyncPayload payload) {
            if (payload.positions().size() > MAX_POSITIONS) {
                throw new IllegalArgumentException("Too many FastMine preview positions.");
            }
            buffer.writeInt(payload.requestId());
            buffer.writeInt(payload.positions().size());
            for (BlockPos position : payload.positions()) {
                buffer.writeLong(position.asLong());
            }
        }
    };

    @Override
    public Type<FastMineMiningPreviewSyncPayload> type() {
        return TYPE;
    }
}
