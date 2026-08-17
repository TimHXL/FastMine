package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * OP 客户端提交的两项服务器全局蹲下触发规则。
 */
public record FastMineGlobalSettingsUpdatePayload(boolean veinMustSneak, boolean areaMustSneak)
        implements CustomPacketPayload {
    public static final Type<FastMineGlobalSettingsUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "global_settings_update")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineGlobalSettingsUpdatePayload> CODEC =
            new StreamCodec<>() {
                @Override
                public FastMineGlobalSettingsUpdatePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new FastMineGlobalSettingsUpdatePayload(buffer.readBoolean(), buffer.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, FastMineGlobalSettingsUpdatePayload payload) {
                    buffer.writeBoolean(payload.veinMustSneak());
                    buffer.writeBoolean(payload.areaMustSneak());
                }
            };

    @Override
    public Type<FastMineGlobalSettingsUpdatePayload> type() {
        return TYPE;
    }
}
