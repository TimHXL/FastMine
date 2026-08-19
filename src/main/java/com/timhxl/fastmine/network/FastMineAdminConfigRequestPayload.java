package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** OP 客户端请求管理员配置快照的空载荷。 */
public record FastMineAdminConfigRequestPayload() implements CustomPacketPayload {
    public static final Type<FastMineAdminConfigRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "admin_config_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineAdminConfigRequestPayload> CODEC = new StreamCodec<>() {
        @Override
        public FastMineAdminConfigRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return new FastMineAdminConfigRequestPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FastMineAdminConfigRequestPayload payload) {
        }
    };

    @Override
    public Type<FastMineAdminConfigRequestPayload> type() {
        return TYPE;
    }
}
