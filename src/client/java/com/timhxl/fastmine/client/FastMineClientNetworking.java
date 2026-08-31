package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineSettingsRequestPayload;
import com.timhxl.fastmine.network.FastMineSettingsSyncPayload;
import com.timhxl.fastmine.network.FastMineSettingsUpdatePayload;
import com.timhxl.fastmine.network.FastMineGlobalSettingsUpdatePayload;
import com.timhxl.fastmine.network.FastMineAdminConfigRequestPayload;
import com.timhxl.fastmine.network.FastMineAdminConfigSyncPayload;
import com.timhxl.fastmine.network.FastMineAdminConfigUpdatePayload;
import com.timhxl.fastmine.network.FastMineMiningPreviewRequestPayload;
import com.timhxl.fastmine.network.FastMineMiningPreviewSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * FastMine 客户端个人设置网络入口。
 */
public final class FastMineClientNetworking {
    private FastMineClientNetworking() {
    }

    /**
     * 注册服务端个人设置同步接收器。
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FastMineSettingsSyncPayload.TYPE,
                (payload, context) -> FastMineClientSettings.setSnapshot(payload));
        ClientPlayNetworking.registerGlobalReceiver(FastMineAdminConfigSyncPayload.TYPE,
                (payload, context) -> FastMineClientAdminConfig.setSnapshot(payload));
        ClientPlayNetworking.registerGlobalReceiver(FastMineMiningPreviewSyncPayload.TYPE,
                (payload, context) -> FastMinePreviewRenderer.acceptServerPreview(payload));
    }

    /**
     * 向当前服务器请求一次最新个人设置。
     */
    public static void requestSettings() {
        if (ClientPlayNetworking.canSend(FastMineSettingsRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineSettingsRequestPayload());
        }
    }

    /**
     * 向服务器提交玩家设置。服务端会校验、保存并返回权威快照。
     */
    public static void updateSettings(boolean veinEnabled, boolean areaEnabled, int areaWidth, int areaHeight,
                                      int areaDepth, boolean aggregateDropsAtFeet, boolean directExperience) {
        if (ClientPlayNetworking.canSend(FastMineSettingsUpdatePayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineSettingsUpdatePayload(
                    veinEnabled,
                    areaEnabled,
                    areaWidth,
                    areaHeight,
                    areaDepth,
                    aggregateDropsAtFeet,
                    directExperience
            ));
        }
    }

    /**
     * 提交 OP 管理的服务器全局蹲下触发规则。
     */
    public static void updateGlobalSettings(boolean veinMustSneak, boolean areaMustSneak, int maxChain,
                                            int minAreaWidth, int minAreaHeight, int minAreaDepth,
                                            int maxAreaWidth, int maxAreaHeight, int maxAreaDepth,
                                            int defaultAreaWidth, int defaultAreaHeight, int defaultAreaDepth,
                                            boolean verticalMiningEnabled, int verticalMiningDepth,
                                            boolean structureProtectionEnabled, boolean transferExtraDropsToPlayer) {
        if (ClientPlayNetworking.canSend(FastMineGlobalSettingsUpdatePayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineGlobalSettingsUpdatePayload(
                    veinMustSneak, areaMustSneak, maxChain, minAreaWidth, minAreaHeight, minAreaDepth,
                    maxAreaWidth, maxAreaHeight, maxAreaDepth, defaultAreaWidth, defaultAreaHeight, defaultAreaDepth,
                    verticalMiningEnabled, verticalMiningDepth, structureProtectionEnabled,
                    transferExtraDropsToPlayer
            ));
        }
    }

    /** 请求 OP 管理界面所需的服务器 groups.json 快照。 */
    public static void requestAdminConfig() {
        if (ClientPlayNetworking.canSend(FastMineAdminConfigRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineAdminConfigRequestPayload());
        }
    }

    /** 向服务器提交管理员界面中的单次组编辑操作。 */
    public static void updateAdminConfig(FastMineAdminConfigUpdatePayload.Operation operation, int groupIndex, String value) {
        if (ClientPlayNetworking.canSend(FastMineAdminConfigUpdatePayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineAdminConfigUpdatePayload(operation, groupIndex, value));
        }
    }

    /** 请求服务器按真实连锁/范围规则计算当前准星目标的候选方块。 */
    public static void requestMiningPreview(BlockPos origin, Direction hitFace, boolean crouching, int requestId) {
        if (ClientPlayNetworking.canSend(FastMineMiningPreviewRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineMiningPreviewRequestPayload(origin, hitFace, crouching, requestId));
        }
    }
}
