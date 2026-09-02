package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Java 客户端请求一次服务端权威挖掘预览。准星目标和蹲下状态仅由服务端读取。 */
public record FastMineMiningPreviewRequestPayload(int requestId)
        implements CustomPacketPayload {
    public static final Type<FastMineMiningPreviewRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "mining_preview_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineMiningPreviewRequestPayload> CODEC = new StreamCodec<>() {
        @Override
        public FastMineMiningPreviewRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return new FastMineMiningPreviewRequestPayload(buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FastMineMiningPreviewRequestPayload payload) {
            buffer.writeInt(payload.requestId());
        }
    };

    @Override
    public Type<FastMineMiningPreviewRequestPayload> type() {
        return TYPE;
    }
}
