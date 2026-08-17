package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineSettingsSyncPayload;

/**
 * 客户端缓存的服务端权威个人设置快照。
 */
public final class FastMineClientSettings {
    private static FastMineSettingsSyncPayload snapshot;
    private static long revision;

    private FastMineClientSettings() {
    }

    /**
     * 获取最近一次服务端同步的设置；尚未同步时返回 null。
     */
    public static FastMineSettingsSyncPayload getSnapshot() {
        return snapshot;
    }

    /**
     * 获取快照版本号，供已打开的界面检测异步更新。
     */
    public static long getRevision() {
        return revision;
    }

    /**
     * 保存服务端返回的最新快照。
     */
    public static void setSnapshot(FastMineSettingsSyncPayload newSnapshot) {
        snapshot = newSnapshot;
        revision++;
    }

    /**
     * 在断开连接后清除上一台服务器的缓存。
     */
    public static void clear() {
        snapshot = null;
        revision++;
    }
}
