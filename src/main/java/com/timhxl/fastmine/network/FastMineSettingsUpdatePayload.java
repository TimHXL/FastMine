package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端提交的玩家个人设置。
 */
public record FastMineSettingsUpdatePayload(boolean veinEnabled, boolean areaEnabled, int areaWidth, int areaHeight,
                                            int areaDepth, boolean aggregateDropsAtFeet,
                                            boolean directExperience) implements CustomPacketPayload {
    public static final Type<FastMineSettingsUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "settings_update")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineSettingsUpdatePayload> CODEC =
            new StreamCodec<>() {
                @Override
                public FastMineSettingsUpdatePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new FastMineSettingsUpdatePayload(
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readBoolean(),
                            buffer.readBoolean()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, FastMineSettingsUpdatePayload payload) {
                    buffer.writeBoolean(payload.veinEnabled());
                    buffer.writeBoolean(payload.areaEnabled());
                    buffer.writeInt(payload.areaWidth());
                    buffer.writeInt(payload.areaHeight());
                    buffer.writeInt(payload.areaDepth());
                    buffer.writeBoolean(payload.aggregateDropsAtFeet());
                    buffer.writeBoolean(payload.directExperience());
                }
            };

    @Override
    public Type<FastMineSettingsUpdatePayload> type() {
        return TYPE;
    }
}
