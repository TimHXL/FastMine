package com.timhxl.fastmine.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * FastMine 服务器全局配置。
 *
 * <p>字段使用公开形式，以便 Gson 直接读写 config.json。</p>
 */
public final class FastMineConfig {
    public static final int CURRENT_SCHEMA_VERSION = 3;
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
     * 范围挖矿是否必须在玩家蹲下时才触发。
     *
     * <p>这是服务器全局规则；默认 false 以保持已有服务器的行为不变。</p>
     */
    public boolean areaMustSneak = false;

    /** 普通玩家可设置的最小范围宽度。宽度必须为奇数，确保锚点位于横向中心。 */
    public int minAreaWidth = 1;

    /** 普通玩家可设置的最小范围高度。高度必须为奇数，确保锚点位于纵向中心。 */
    public int minAreaHeight = 1;

    /** 普通玩家可设置的最小范围深度。深度从锚点单向延伸，因此允许偶数。 */
    public int minAreaDepth = 1;

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

    /** 管理员 GUI 维护的额外天然石材列表；它与数据包 Tag 共同构成允许集合。 */
    public List<String> naturalStoneBlocks = new ArrayList<>(List.of(
            "minecraft:stone", "minecraft:deepslate", "minecraft:granite", "minecraft:diorite",
            "minecraft:andesite", "minecraft:tuff", "minecraft:calcite"
    ));

    /** 是否额外跳过受保护结构已保存范围内的范围挖矿候选坐标。 */
    public boolean structureProtectionEnabled = true;

    /**
     * FastMine 破坏的额外方块掉落物是否直接转入执行挖掘的玩家背包。
     *
     * <p>仅影响范围挖掘和连锁采集额外破坏的方块；玩家手动挖掉的首个方块保持原版掉落行为。</p>
     */
    public boolean transferExtraDropsToPlayer = false;

    /** FastMine 额外破坏方块产生的经验是否允许直接获取；玩家仍需开启个人选项。 */
    public boolean directExperience = false;

    /** 连锁采集额外破坏的方块是否消耗工具耐久。 */
    public boolean veinMiningConsumesDurability = true;

    /** 连锁采集额外破坏的方块是否消耗饥饿值。 */
    public boolean veinMiningConsumesHunger = true;

    /** 范围挖掘额外破坏的方块是否消耗工具耐久。 */
    public boolean areaMiningConsumesDurability = true;

    /** 范围挖掘额外破坏的方块是否消耗饥饿值。 */
    public boolean areaMiningConsumesHunger = true;

    /** 需要保护的 Structure 注册表 ID。 */
    public Set<String> protectedStructures = new LinkedHashSet<>(Set.of(
            "minecraft:stronghold", "minecraft:ancient_city", "minecraft:mineshaft"
    ));

    /**
     * 将配置文件中的非法值修正为安全范围。
     */
    public void normalize() {
        migrateLegacyLimits();

        maxAreaWidth = clampOdd(maxAreaWidth, HARD_MAX_AREA_WIDTH);
        maxAreaHeight = clampOdd(maxAreaHeight, HARD_MAX_AREA_HEIGHT);
        maxAreaDepth = clamp(maxAreaDepth, 1, HARD_MAX_AREA_DEPTH);
        minAreaWidth = clampOdd(minAreaWidth, maxAreaWidth);
        minAreaHeight = clampOdd(minAreaHeight, maxAreaHeight);
        minAreaDepth = clamp(minAreaDepth, 1, maxAreaDepth);

        defaultAreaWidth = clampOdd(defaultAreaWidth, maxAreaWidth);
        defaultAreaHeight = clampOdd(defaultAreaHeight, maxAreaHeight);
        defaultAreaDepth = clamp(defaultAreaDepth, minAreaDepth, maxAreaDepth);
        defaultAreaWidth = Math.max(minAreaWidth, defaultAreaWidth);
        defaultAreaHeight = Math.max(minAreaHeight, defaultAreaHeight);
        verticalMiningDepth = clamp(verticalMiningDepth, 1, maxAreaDepth);

        if ((naturalStoneTag == null) || naturalStoneTag.isBlank()) {
            naturalStoneTag = "fastmine:natural_stones";
        }
        naturalStoneBlocks = normalizeIdentifiers(naturalStoneBlocks);
        protectedStructures = new LinkedHashSet<>(normalizeIdentifiers(protectedStructures));
    }

    private void migrateLegacyLimits() {
        if (schemaVersion < 2) {
            if (maxAreaWidth == 5 && maxAreaHeight == 5 && maxAreaDepth == 5) {
                maxAreaWidth = HARD_MAX_AREA_WIDTH;
                maxAreaHeight = HARD_MAX_AREA_HEIGHT;
                maxAreaDepth = HARD_MAX_AREA_DEPTH;
            }
        }
        schemaVersion = CURRENT_SCHEMA_VERSION;
    }

    private static int clampOdd(int value, int maximum) {
        int clamped = clamp(value, 1, maximum);
        return clamped % 2 == 0 ? clamped - 1 : clamped;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static List<String> normalizeIdentifiers(Iterable<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) result.add(value.strip());
            }
        }
        return new ArrayList<>(result);
    }
}
