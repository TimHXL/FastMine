package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端请求当前玩家 FastMine 设置的空载荷。
 */
public record FastMineSettingsRequestPayload() implements CustomPacketPayload {
    public static final Type<FastMineSettingsRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "settings_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineSettingsRequestPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public FastMineSettingsRequestPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new FastMineSettingsRequestPayload();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, FastMineSettingsRequestPayload payload) {
                }
            };

    @Override
    public Type<FastMineSettingsRequestPayload> type() {
        return TYPE;
    }
}
