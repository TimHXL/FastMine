package com.timhxl.fastmine.player;

/**
 * 玩家个人快速挖矿设置。
 *
 * <p>该对象仅描述玩家设置本身；按 UUID 查找和持久化由后续服务负责。</p>
 */
public record PlayerFastMineSettings(
        boolean veinEnabled,
        boolean areaEnabled,
        int areaWidth,
        int areaHeight,
        int areaDepth
) {
    public static final int DEFAULT_AREA_WIDTH = 3;
    public static final int DEFAULT_AREA_HEIGHT = 3;
    public static final int DEFAULT_AREA_DEPTH = 3;

    /**
     * 创建首次加入服务器时使用的默认设置。
     */
    public static PlayerFastMineSettings createDefault() {
        return new PlayerFastMineSettings(false, false, DEFAULT_AREA_WIDTH, DEFAULT_AREA_HEIGHT, DEFAULT_AREA_DEPTH);
    }

    /**
     * 返回仅修改连锁采集开关后的新设置。
     */
    public PlayerFastMineSettings withVeinEnabled(boolean enabled) {
        return new PlayerFastMineSettings(enabled, areaEnabled, areaWidth, areaHeight, areaDepth);
    }

    /**
     * 返回仅修改范围挖矿开关后的新设置。
     */
    public PlayerFastMineSettings withAreaEnabled(boolean enabled) {
        return new PlayerFastMineSettings(veinEnabled, enabled, areaWidth, areaHeight, areaDepth);
    }
}