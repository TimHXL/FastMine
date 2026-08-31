package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Java 客户端请求当前准星目标的服务端权威挖掘预览。 */
public record FastMineMiningPreviewRequestPayload(BlockPos origin, Direction hitFace, boolean crouching, int requestId)
        implements CustomPacketPayload {
    public static final Type<FastMineMiningPreviewRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "mining_preview_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineMiningPreviewRequestPayload> CODEC = new StreamCodec<>() {
        @Override
        public FastMineMiningPreviewRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            BlockPos origin = BlockPos.of(buffer.readLong());
            int direction = buffer.readInt();
            if (direction < 0 || direction >= Direction.values().length) {
                throw new IllegalArgumentException("Invalid FastMine preview direction.");
            }
            boolean crouching = buffer.readBoolean();
            return new FastMineMiningPreviewRequestPayload(
                    origin, Direction.values()[direction], crouching, buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FastMineMiningPreviewRequestPayload payload) {
            buffer.writeLong(payload.origin().asLong());
            buffer.writeInt(payload.hitFace().ordinal());
            buffer.writeBoolean(payload.crouching());
            buffer.writeInt(payload.requestId());
        }
    };

    @Override
    public Type<FastMineMiningPreviewRequestPayload> type() {
        return TYPE;
    }
}
