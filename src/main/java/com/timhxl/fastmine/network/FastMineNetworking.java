package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.config.FastMineConfig;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

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
        PayloadTypeRegistry.serverboundPlay().register(FastMineAdminConfigRequestPayload.TYPE,
                FastMineAdminConfigRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FastMineAdminConfigUpdatePayload.TYPE,
                FastMineAdminConfigUpdatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FastMineSettingsSyncPayload.TYPE,
                FastMineSettingsSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FastMineAdminConfigSyncPayload.TYPE,
                FastMineAdminConfigSyncPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(FastMineSettingsRequestPayload.TYPE,
                (payload, context) -> sendSettings(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(FastMineSettingsUpdatePayload.TYPE,
                (payload, context) -> updateSettings(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(FastMineGlobalSettingsUpdatePayload.TYPE,
                (payload, context) -> updateGlobalSettings(context.player(), context.server(), payload));
        ServerPlayNetworking.registerGlobalReceiver(FastMineAdminConfigRequestPayload.TYPE,
                (payload, context) -> sendAdminConfig(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(FastMineAdminConfigUpdatePayload.TYPE,
                (payload, context) -> updateAdminConfig(context.player(), context.server(), payload));
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
                FastMineMod.getVeinMiningConfigManager().getSettings().mustSneak,
                FastMineMod.getVeinMiningConfigManager().getSettings().maxChain
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
    private static void updateGlobalSettings(ServerPlayer player, net.minecraft.server.MinecraftServer server,
                                             FastMineGlobalSettingsUpdatePayload payload) {
        if (!canManageServerSettings(player)) {
            FastMineMod.LOGGER.warn("Rejected FastMine global settings update from non-OP player {}", player.getUUID());
            sendSettings(player);
            return;
        }

        FastMineMod.getVeinMiningConfigManager().getSettings().mustSneak = payload.veinMustSneak();
        FastMineMod.getVeinMiningConfigManager().getSettings().maxChain = Math.clamp(payload.maxChain(), 1, 10000);
        FastMineMod.getVeinMiningConfigManager().save();
        FastMineConfig config = FastMineMod.getConfigManager().getConfig();
        config.areaMustSneak = payload.areaMustSneak();
        config.minAreaWidth = payload.minAreaWidth();
        config.minAreaHeight = payload.minAreaHeight();
        config.minAreaDepth = payload.minAreaDepth();
        config.maxAreaWidth = payload.maxAreaWidth();
        config.maxAreaHeight = payload.maxAreaHeight();
        config.maxAreaDepth = payload.maxAreaDepth();
        config.defaultAreaWidth = payload.defaultAreaWidth();
        config.defaultAreaHeight = payload.defaultAreaHeight();
        config.defaultAreaDepth = payload.defaultAreaDepth();
        config.verticalMiningEnabled = payload.verticalMiningEnabled();
        config.verticalMiningDepth = payload.verticalMiningDepth();
        config.structureProtectionEnabled = payload.structureProtectionEnabled();
        config.normalize();
        FastMineMod.getConfigManager().save();
        FastMineMod.getVeinMiningRuleRegistry().reload(server);
        sendSettings(player);
        sendAdminConfig(player);
    }

    /**
     * 使用与管理员命令完全一致的二级权限判定。
     */
    private static boolean canManageServerSettings(ServerPlayer player) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    /** 仅向 OP 发送 groups.json 的管理界面快照。 */
    private static void sendAdminConfig(ServerPlayer player) {
        if (!canManageServerSettings(player)) {
            return;
        }

        List<FastMineAdminGroupSnapshot> groups = FastMineMod.getVeinMiningConfigManager().getGroups().stream()
                .map(FastMineAdminGroupSnapshot::from)
                .toList();
        ServerPlayNetworking.send(player, new FastMineAdminConfigSyncPayload(groups,
                FastMineAdminGlobalConfigSnapshot.from(FastMineMod.getConfigManager().getConfig())));
    }

    /** 校验 OP 的单次编辑操作、持久化 groups.json 并立即重载规则。 */
    private static void updateAdminConfig(ServerPlayer player, net.minecraft.server.MinecraftServer server,
                                          FastMineAdminConfigUpdatePayload payload) {
        if (!canManageServerSettings(player)) {
            FastMineMod.LOGGER.warn("Rejected FastMine group edit from non-OP player {}", player.getUUID());
            return;
        }

        boolean changed = switch (payload.operation()) {
            case CREATE_GROUP -> FastMineMod.getVeinMiningConfigManager().createGroup(payload.value());
            case DELETE_GROUP -> FastMineMod.getVeinMiningConfigManager().deleteGroup(payload.groupIndex());
            case ADD_BLOCK -> isKnownBlock(payload.value())
                    && FastMineMod.getVeinMiningConfigManager().addBlock(payload.groupIndex(), payload.value());
            case ADD_TOOL -> isKnownItem(payload.value())
                    && FastMineMod.getVeinMiningConfigManager().addTool(payload.groupIndex(), payload.value());
            case REMOVE_BLOCK -> FastMineMod.getVeinMiningConfigManager().removeBlock(payload.groupIndex(), payload.value());
            case REMOVE_TOOL -> FastMineMod.getVeinMiningConfigManager().removeTool(payload.groupIndex(), payload.value());
            case ADD_NATURAL_STONE -> addNaturalStone(payload.value());
            case REMOVE_NATURAL_STONE -> FastMineMod.getConfigManager().getConfig().naturalStoneBlocks.remove(payload.value());
            case ADD_PROTECTED_STRUCTURE -> addProtectedStructure(server, payload.value());
            case REMOVE_PROTECTED_STRUCTURE -> FastMineMod.getConfigManager().getConfig().protectedStructures.remove(payload.value());
        };
        if (!changed) {
            FastMineMod.LOGGER.warn("Rejected or unchanged FastMine group edit from {}: {}", player.getUUID(), payload.operation());
            sendAdminConfig(player);
            return;
        }

        FastMineMod.getVeinMiningConfigManager().save();
        FastMineMod.getConfigManager().getConfig().normalize();
        FastMineMod.getConfigManager().save();
        FastMineMod.getVeinMiningRuleRegistry().reload(server);
        sendAdminConfig(player);
    }

    private static boolean isKnownBlock(String value) {
        Identifier identifier = Identifier.tryParse(value);
        return identifier != null && BuiltInRegistries.BLOCK.containsKey(identifier);
    }

    private static boolean isKnownItem(String value) {
        Identifier identifier = Identifier.tryParse(value);
        return identifier != null && BuiltInRegistries.ITEM.containsKey(identifier);
    }

    private static boolean addNaturalStone(String value) {
        if (!isKnownBlock(value)) return false;
        List<String> blocks = FastMineMod.getConfigManager().getConfig().naturalStoneBlocks;
        if (blocks.contains(value) || blocks.size() >= 512) return false;
        blocks.add(value);
        return true;
    }

    private static boolean addProtectedStructure(net.minecraft.server.MinecraftServer server, String value) {
        Identifier identifier = Identifier.tryParse(value);
        if (identifier == null || server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(identifier) == null) return false;
        return FastMineMod.getConfigManager().getConfig().protectedStructures.add(value);
    }

}
