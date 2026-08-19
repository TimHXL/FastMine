package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineAdminConfigSyncPayload;

/** 客户端缓存的 OP 管理配置只读快照。 */
public final class FastMineClientAdminConfig {
    private static FastMineAdminConfigSyncPayload snapshot;
    private static long revision;

    private FastMineClientAdminConfig() {
    }

    public static FastMineAdminConfigSyncPayload getSnapshot() {
        return snapshot;
    }

    public static long getRevision() {
        return revision;
    }

    public static void setSnapshot(FastMineAdminConfigSyncPayload newSnapshot) {
        snapshot = newSnapshot;
        revision++;
    }

    public static void clear() {
        snapshot = null;
        revision++;
    }
}
