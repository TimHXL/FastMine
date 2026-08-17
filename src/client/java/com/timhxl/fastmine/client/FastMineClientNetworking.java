package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineSettingsRequestPayload;
import com.timhxl.fastmine.network.FastMineSettingsSyncPayload;
import com.timhxl.fastmine.network.FastMineSettingsUpdatePayload;
import com.timhxl.fastmine.network.FastMineGlobalSettingsUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
                                      int areaDepth) {
        if (ClientPlayNetworking.canSend(FastMineSettingsUpdatePayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineSettingsUpdatePayload(
                    veinEnabled,
                    areaEnabled,
                    areaWidth,
                    areaHeight,
                    areaDepth
            ));
        }
    }

    /**
     * 提交 OP 管理的服务器全局蹲下触发规则。
     */
    public static void updateGlobalSettings(boolean veinMustSneak, boolean areaMustSneak) {
        if (ClientPlayNetworking.canSend(FastMineGlobalSettingsUpdatePayload.TYPE)) {
            ClientPlayNetworking.send(new FastMineGlobalSettingsUpdatePayload(veinMustSneak, areaMustSneak));
        }
    }
}
