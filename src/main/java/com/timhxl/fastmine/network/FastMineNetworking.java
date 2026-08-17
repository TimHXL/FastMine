package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * FastMine 玩家设置的服务端网络入口。
 */
public final class FastMineNetworking {
    private FastMineNetworking() {
    }

    /**
     * 注册双向载荷类型及服务端接收器。
     */
    public static void registerServer() {
        PayloadTypeRegistry.serverboundPlay().register(FastMineSettingsRequestPayload.TYPE,
                FastMineSettingsRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FastMineSettingsUpdatePayload.TYPE,
                FastMineSettingsUpdatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FastMineGlobalSettingsUpdatePayload.TYPE,
                FastMineGlobalSettingsUpdatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FastMineSettingsSyncPayload.TYPE,
                FastMineSettingsSyncPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(FastMineSettingsRequestPayload.TYPE,
                (payload, context) -> sendSettings(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(FastMineSettingsUpdatePayload.TYPE,
                (payload, context) -> updateSettings(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(FastMineGlobalSettingsUpdatePayload.TYPE,
                (payload, context) -> updateGlobalSettings(context.player(), payload));
    }

    /**
     * 将服务器保存的真实个人设置发送给指定玩家。
     */
    public static void sendSettings(ServerPlayer player) {
        PlayerFastMineSettings settings = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        ServerPlayNetworking.send(player, FastMineSettingsSyncPayload.from(
                settings,
                FastMineMod.getConfigManager().getConfig(),
                canManageServerSettings(player),
                FastMineMod.getVeinMiningConfigManager().getSettings().mustSneak
        ));
    }

    /**
     * 校验并保存客户端提交的个人设置；非法载荷不会写入配置。
     */
    private static void updateSettings(ServerPlayer player, FastMineSettingsUpdatePayload payload) {
        try {
            FastMineMod.getPlayerSettingsService().updateFromClient(
                    player.getUUID(),
                    payload.veinEnabled(),
                    payload.areaEnabled(),
                    payload.areaWidth(),
                    payload.areaHeight(),
                    payload.areaDepth()
            );
        } catch (IllegalArgumentException exception) {
            FastMineMod.LOGGER.warn("Rejected invalid FastMine settings: {}", exception.getMessage());
        }

        sendSettings(player);
    }

    /**
     * 仅允许 OP 修改服务器全局蹲下触发规则。
     */
    private static void updateGlobalSettings(ServerPlayer player, FastMineGlobalSettingsUpdatePayload payload) {
        if (!canManageServerSettings(player)) {
            FastMineMod.LOGGER.warn("Rejected FastMine global settings update from non-OP player {}", player.getUUID());
            sendSettings(player);
            return;
        }

        FastMineMod.getVeinMiningConfigManager().getSettings().mustSneak = payload.veinMustSneak();
        FastMineMod.getVeinMiningConfigManager().save();
        FastMineMod.getConfigManager().getConfig().areaMustSneak = payload.areaMustSneak();
        FastMineMod.getConfigManager().save();
        sendSettings(player);
    }

    /**
     * 使用与管理员命令完全一致的二级权限判定。
     */
    private static boolean canManageServerSettings(ServerPlayer player) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }
}
