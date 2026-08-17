package com.timhxl.fastmine.config;

/**
 * FastMine 服务器全局配置。
 *
 * <p>字段使用公开形式，以便 Gson 直接读写 config.json。</p>
 */
public final class FastMineConfig {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int HARD_MAX_AREA_WIDTH = 9;
    public static final int HARD_MAX_AREA_HEIGHT = 9;
    public static final int HARD_MAX_AREA_DEPTH = 10;

    /**
     * 配置文件结构版本。
     */
    public int schemaVersion = CURRENT_SCHEMA_VERSION;

    /**
     * 新玩家默认是否开启连锁采集。
     */
    public boolean defaultVeinEnabled = false;

    /**
     * 新玩家默认是否开启范围挖矿。
     */
    public boolean defaultAreaEnabled = false;

    /**
     * 新玩家默认范围宽度。
     */
    public int defaultAreaWidth = 3;

    /**
     * 新玩家默认范围高度。
     */
    public int defaultAreaHeight = 3;

    /**
     * 新玩家默认范围深度。
     */
    public int defaultAreaDepth = 3;

    /**
     * 普通玩家允许设置的最大范围宽度。
     */
    public int maxAreaWidth = HARD_MAX_AREA_WIDTH;

    /**
     * 普通玩家允许设置的最大范围高度。
     */
    public int maxAreaHeight = HARD_MAX_AREA_HEIGHT;

    /**
     * 普通玩家允许设置的最大范围深度。
     */
    public int maxAreaDepth = HARD_MAX_AREA_DEPTH;

    /**
     * 向上或向下范围挖矿时，是否启用独立的服务器全局深度。
     */
    public boolean verticalMiningEnabled = true;

    /**
     * 向上或向下范围挖矿时强制使用的深度。
     */
    public int verticalMiningDepth = 2;

    /**
     * 范围挖矿允许破坏的天然石材 Tag。
     */
    public String naturalStoneTag = "fastmine:natural_stones";

    /**
     * 将配置文件中的非法值修正为安全范围。
     */
    public void normalize() {
        migrateLegacyLimits();

        maxAreaWidth = clampOdd(maxAreaWidth, HARD_MAX_AREA_WIDTH);
        maxAreaHeight = clampOdd(maxAreaHeight, HARD_MAX_AREA_HEIGHT);
        maxAreaDepth = clamp(maxAreaDepth, 1, HARD_MAX_AREA_DEPTH);

        defaultAreaWidth = clampOdd(defaultAreaWidth, maxAreaWidth);
        defaultAreaHeight = clampOdd(defaultAreaHeight, maxAreaHeight);
        defaultAreaDepth = clamp(defaultAreaDepth, 1, maxAreaDepth);
        verticalMiningDepth = clamp(verticalMiningDepth, 1, maxAreaDepth);

        if ((naturalStoneTag == null) || naturalStoneTag.isBlank()) {
            naturalStoneTag = "fastmine:natural_stones";
        }
    }

    private void migrateLegacyLimits() {
        if (schemaVersion < 2) {
            if (maxAreaWidth == 5 && maxAreaHeight == 5 && maxAreaDepth == 5) {
                maxAreaWidth = HARD_MAX_AREA_WIDTH;
                maxAreaHeight = HARD_MAX_AREA_HEIGHT;
                maxAreaDepth = HARD_MAX_AREA_DEPTH;
            }

            schemaVersion = CURRENT_SCHEMA_VERSION;
        }
    }

    private static int clampOdd(int value, int maximum) {
        int clamped = clamp(value, 1, maximum);
        return clamped % 2 == 0 ? clamped - 1 : clamped;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
