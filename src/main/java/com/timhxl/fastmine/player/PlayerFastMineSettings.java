package com.timhxl.fastmine.player;

import com.timhxl.fastmine.config.FastMineConfig;

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
        int areaDepth,
        boolean aggregateDropsAtFeet,
        boolean directExperience
) {
    /**
     * 根据服务器全局配置创建首次加入服务器时使用的默认设置。
     */
    public static PlayerFastMineSettings createDefault(FastMineConfig config) {
        return new PlayerFastMineSettings(
                config.defaultVeinEnabled,
                config.defaultAreaEnabled,
                config.defaultAreaWidth,
                config.defaultAreaHeight,
                config.defaultAreaDepth,
                false,
                false
        );
    }

    /**
     * 返回仅修改连锁采集开关后的新设置。
     */
    public PlayerFastMineSettings withVeinEnabled(boolean enabled) {
        return new PlayerFastMineSettings(enabled, areaEnabled, areaWidth, areaHeight, areaDepth,
                aggregateDropsAtFeet, directExperience);
    }

    /**
     * 返回仅修改范围挖矿开关后的新设置。
     */
    public PlayerFastMineSettings withAreaEnabled(boolean enabled) {
        return new PlayerFastMineSettings(veinEnabled, enabled, areaWidth, areaHeight, areaDepth,
                aggregateDropsAtFeet, directExperience);
    }

    /**
     * 返回仅修改范围挖矿尺寸后的新设置。
     */
    public PlayerFastMineSettings withAreaSize(int width, int height, int depth) {
        return new PlayerFastMineSettings(veinEnabled, areaEnabled, width, height, depth,
                aggregateDropsAtFeet, directExperience);
    }
}
