package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.config.FastMineConfig;
import com.timhxl.fastmine.mining.MiningContext;
import com.timhxl.fastmine.mining.area.AreaMiningPlanner;
import com.timhxl.fastmine.mining.area.NaturalStoneFilter;
import com.timhxl.fastmine.mining.area.StructureProtectionFilter;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import com.timhxl.fastmine.vein.mining.VeinMiningTrigger;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;

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
        PayloadTypeRegistry.serverboundPlay().register(FastMineMiningPreviewRequestPayload.TYPE,
                FastMineMiningPreviewRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FastMineSettingsSyncPayload.TYPE,
                FastMineSettingsSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FastMineAdminConfigSyncPayload.TYPE,
                FastMineAdminConfigSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FastMineMiningPreviewSyncPayload.TYPE,
                FastMineMiningPreviewSyncPayload.CODEC);

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
        ServerPlayNetworking.registerGlobalReceiver(FastMineMiningPreviewRequestPayload.TYPE,
                (payload, context) -> sendMiningPreview(context.player(), payload));
    }

    /**
     * 按服务端射线检测到的准星方块、玩家状态、连锁组、天然石材和结构保护规则计算预览。
     * 客户端只提交请求序号，不能指定墙后方块或伪造蹲下状态。
     */
    private static void sendMiningPreview(ServerPlayer player, FastMineMiningPreviewRequestPayload payload) {
        ServerLevel level = (ServerLevel) player.level();
        if (!player.isAlive()) {
            sendMiningPreview(player, payload.requestId(), List.of());
            return;
        }

        Vec3 eyePosition = player.getEyePosition();
        Vec3 endPosition = eyePosition.add(player.getLookAngle().scale(6.0D));
        BlockHitResult hitResult = level.clip(new ClipContext(eyePosition, endPosition,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            sendMiningPreview(player, payload.requestId(), List.of());
            return;
        }

        BlockPos origin = hitResult.getBlockPos();

        FastMineConfig config = FastMineMod.getConfigManager().getConfig();
        PlayerFastMineSettings settings = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        var state = level.getBlockState(origin);
        if (state.isAir() || (!settings.areaEnabled() && !settings.veinEnabled())) {
            sendMiningPreview(player, payload.requestId(), List.of());
            return;
        }

        MiningContext context = new MiningContext(level, player, origin.immutable(), state, settings, config);
        var veinPlan = VeinMiningTrigger.plan(context, player.isCrouching());
        if (veinPlan.isPresent()) {
            List<BlockPos> positions = new ArrayList<>(veinPlan.get().candidates().size() + 1);
            positions.add(origin.immutable());
            positions.addAll(veinPlan.get().candidates());
            sendMiningPreview(player, payload.requestId(), positions);
            return;
        }

        Direction miningDirection = hitResult.getDirection().getOpposite();
        if (!settings.areaEnabled() || (config.areaMustSneak && !player.isCrouching())
                || !player.getMainHandItem().is(ItemTags.PICKAXES)
                || !NaturalStoneFilter.isAllowed(state, config)) {
            sendMiningPreview(player, payload.requestId(), List.of());
            return;
        }

        List<BlockPos> allowed = new ArrayList<>();
        for (BlockPos position : NaturalStoneFilter.filterAllowedPositions(
                context, AreaMiningPlanner.calculateCandidates(context, miningDirection))) {
            if (position.equals(origin) || !StructureProtectionFilter.isProtected(level, position, config)) {
                allowed.add(position);
            }
        }
        sendMiningPreview(player, payload.requestId(), allowed);
    }

    private static void sendMiningPreview(ServerPlayer player, int requestId, List<BlockPos> positions) {
        int endIndex = Math.min(positions.size(), FastMineMiningPreviewSyncPayload.MAX_POSITIONS);
        ServerPlayNetworking.send(player, new FastMineMiningPreviewSyncPayload(
                requestId, List.copyOf(positions.subList(0, endIndex))));
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
                    payload.areaDepth(),
                    payload.aggregateDropsAtFeet(),
                    payload.directExperience()
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
        config.transferExtraDropsToPlayer = payload.transferExtraDropsToPlayer();
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
            case ADD_TARGET_BLOCK -> addTargetBlock(player, payload.groupIndex());
            case ADD_HELD_TOOL -> addHeldTool(player, payload.groupIndex());
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

    /**
     * 由服务器自身射线检测管理员当前准星方块，客户端不能指定任意方块 ID。
     */
    private static boolean addTargetBlock(ServerPlayer player, int groupIndex) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eyePosition = player.getEyePosition();
        Vec3 endPosition = eyePosition.add(player.getLookAngle().scale(6.0D));
        BlockHitResult hitResult = level.clip(new ClipContext(eyePosition, endPosition,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        Identifier identifier = BuiltInRegistries.BLOCK.getKey(level.getBlockState(hitResult.getBlockPos()).getBlock());
        return identifier != null && FastMineMod.getVeinMiningConfigManager().addBlock(groupIndex, identifier.toString());
    }

    /**
     * 由服务器自身读取管理员主手物品，客户端不能指定任意工具 ID。
     */
    private static boolean addHeldTool(ServerPlayer player, int groupIndex) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            return false;
        }

        Identifier identifier = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        return identifier != null && FastMineMod.getVeinMiningConfigManager().addTool(groupIndex, identifier.toString());
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
