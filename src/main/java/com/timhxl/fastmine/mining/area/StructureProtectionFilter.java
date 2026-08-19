package com.timhxl.fastmine.mining.area;

import com.timhxl.fastmine.config.FastMineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * 基于 Minecraft 已保存的 Structure 范围，保护 Area Mining 的候选坐标。
 *
 * <p>本类只判断坐标是否落在结构范围的实际 Piece 内；它不尝试、也不能证明单个方块
 * 是否由结构生成器放置。配置错误或无法解析的 Structure ID 会被安全忽略。</p>
 */
public final class StructureProtectionFilter {
    private StructureProtectionFilter() {
    }

    /** 当坐标位于任一受保护 Structure 的已保存范围内时返回 true。 */
    public static boolean isProtected(ServerLevel level, BlockPos position, FastMineConfig config) {
        if (!config.structureProtectionEnabled || config.protectedStructures.isEmpty()) return false;

        for (String structureId : config.protectedStructures) {
            Identifier identifier = Identifier.tryParse(structureId);
            if (identifier == null) continue;

            Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(identifier);
            if (structure == null) continue;
            if (level.structureManager().getStructureAt(position, structure).isValid()) return true;
        }
        return false;
    }
}
