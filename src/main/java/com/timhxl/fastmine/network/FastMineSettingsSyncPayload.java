package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.config.FastMineConfig;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端返回的玩家真实设置及可选范围上限。
 */
public record FastMineSettingsSyncPayload(boolean veinEnabled, boolean areaEnabled, int areaWidth, int areaHeight,
                                          int areaDepth, int minAreaWidth, int minAreaHeight, int minAreaDepth,
                                          int maxAreaWidth, int maxAreaHeight, int maxAreaDepth,
                                          boolean canManageServerSettings, boolean veinMustSneak, boolean areaMustSneak,
                                          int maxChain, boolean aggregateDropsAtFeet, boolean directExperience)
        implements CustomPacketPayload {
    public static final Type<FastMineSettingsSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "settings_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineSettingsSyncPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public FastMineSettingsSyncPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new FastMineSettingsSyncPayload(
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readInt(),
                            buffer.readBoolean(),
                            buffer.readBoolean()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, FastMineSettingsSyncPayload payload) {
                    buffer.writeBoolean(payload.veinEnabled());
                    buffer.writeBoolean(payload.areaEnabled());
                    buffer.writeInt(payload.areaWidth());
                    buffer.writeInt(payload.areaHeight());
                    buffer.writeInt(payload.areaDepth());
                    buffer.writeInt(payload.minAreaWidth());
                    buffer.writeInt(payload.minAreaHeight());
                    buffer.writeInt(payload.minAreaDepth());
                    buffer.writeInt(payload.maxAreaWidth());
                    buffer.writeInt(payload.maxAreaHeight());
                    buffer.writeInt(payload.maxAreaDepth());
                    buffer.writeBoolean(payload.canManageServerSettings());
                    buffer.writeBoolean(payload.veinMustSneak());
                    buffer.writeBoolean(payload.areaMustSneak());
                    buffer.writeInt(payload.maxChain());
                    buffer.writeBoolean(payload.aggregateDropsAtFeet());
                    buffer.writeBoolean(payload.directExperience());
                }
            };

    public static FastMineSettingsSyncPayload from(PlayerFastMineSettings settings, FastMineConfig config,
                                                   boolean canManageServerSettings, boolean veinMustSneak, int maxChain) {
        return new FastMineSettingsSyncPayload(
                settings.veinEnabled(),
                settings.areaEnabled(),
                settings.areaWidth(),
                settings.areaHeight(),
                settings.areaDepth(),
                config.minAreaWidth,
                config.minAreaHeight,
                config.minAreaDepth,
                config.maxAreaWidth,
                config.maxAreaHeight,
                config.maxAreaDepth,
                canManageServerSettings,
                veinMustSneak,
                config.areaMustSneak,
                maxChain,
                settings.aggregateDropsAtFeet(),
                settings.directExperience()
        );
    }

    @Override
    public Type<FastMineSettingsSyncPayload> type() {
        return TYPE;
    }
}
