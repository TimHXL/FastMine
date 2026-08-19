package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * OP 客户端提交的服务器全局数值和开关规则。列表类配置使用独立的管理员编辑载荷。
 */
public record FastMineGlobalSettingsUpdatePayload(boolean veinMustSneak, boolean areaMustSneak, int maxChain,
                                                  int minAreaWidth, int minAreaHeight, int minAreaDepth,
                                                  int maxAreaWidth, int maxAreaHeight, int maxAreaDepth,
                                                  int defaultAreaWidth, int defaultAreaHeight, int defaultAreaDepth,
                                                  boolean verticalMiningEnabled, int verticalMiningDepth,
                                                  boolean structureProtectionEnabled)
        implements CustomPacketPayload {
    public static final Type<FastMineGlobalSettingsUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "global_settings_update")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineGlobalSettingsUpdatePayload> CODEC =
            new StreamCodec<>() {
                @Override
                public FastMineGlobalSettingsUpdatePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new FastMineGlobalSettingsUpdatePayload(
                            buffer.readBoolean(), buffer.readBoolean(), buffer.readInt(),
                            buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                            buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readInt(), buffer.readBoolean()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, FastMineGlobalSettingsUpdatePayload payload) {
                    buffer.writeBoolean(payload.veinMustSneak());
                    buffer.writeBoolean(payload.areaMustSneak());
                    buffer.writeInt(payload.maxChain());
                    buffer.writeInt(payload.minAreaWidth());
                    buffer.writeInt(payload.minAreaHeight());
                    buffer.writeInt(payload.minAreaDepth());
                    buffer.writeInt(payload.maxAreaWidth());
                    buffer.writeInt(payload.maxAreaHeight());
                    buffer.writeInt(payload.maxAreaDepth());
                    buffer.writeInt(payload.defaultAreaWidth());
                    buffer.writeInt(payload.defaultAreaHeight());
                    buffer.writeInt(payload.defaultAreaDepth());
                    buffer.writeBoolean(payload.verticalMiningEnabled());
                    buffer.writeInt(payload.verticalMiningDepth());
                    buffer.writeBoolean(payload.structureProtectionEnabled());
                }
            };

    @Override
    public Type<FastMineGlobalSettingsUpdatePayload> type() {
        return TYPE;
    }
}
