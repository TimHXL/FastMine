package com.timhxl.fastmine.network;

import com.timhxl.fastmine.config.FastMineConfig;

import java.util.List;

/** OP 管理界面使用的 FastMine 全局配置只读快照。 */
public record FastMineAdminGlobalConfigSnapshot(int minAreaWidth, int minAreaHeight, int minAreaDepth,
                                                int maxAreaWidth, int maxAreaHeight, int maxAreaDepth,
                                                int defaultAreaWidth, int defaultAreaHeight, int defaultAreaDepth,
                                                boolean verticalMiningEnabled, int verticalMiningDepth,
                                                boolean structureProtectionEnabled, boolean transferExtraDropsToPlayer,
                                                boolean directExperience,
                                                boolean veinMiningConsumesDurability, boolean veinMiningConsumesHunger,
                                                boolean areaMiningConsumesDurability, boolean areaMiningConsumesHunger,
                                                List<String> naturalStoneBlocks,
                                                List<String> protectedStructures) {
    public static FastMineAdminGlobalConfigSnapshot from(FastMineConfig config) {
        return new FastMineAdminGlobalConfigSnapshot(
                config.minAreaWidth, config.minAreaHeight, config.minAreaDepth,
                config.maxAreaWidth, config.maxAreaHeight, config.maxAreaDepth,
                config.defaultAreaWidth, config.defaultAreaHeight, config.defaultAreaDepth,
                config.verticalMiningEnabled, config.verticalMiningDepth, config.structureProtectionEnabled,
                config.transferExtraDropsToPlayer,
                config.directExperience,
                config.veinMiningConsumesDurability, config.veinMiningConsumesHunger,
                config.areaMiningConsumesDurability, config.areaMiningConsumesHunger,
                List.copyOf(config.naturalStoneBlocks), List.copyOf(config.protectedStructures)
        );
    }
}
