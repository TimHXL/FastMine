package com.timhxl.fastmine.vein.mining;

import com.timhxl.fastmine.vein.config.VeinMiningSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

/**
 * 一次玩家手动破坏所生成的连锁采集计划。
 *
 * <p>计划不包含锚点方块；锚点已由原版破坏流程处理。执行器将在下一阶段消费该对象。</p>
 */
public record VeinMiningPlan(
        String groupName,
        Set<Block> targetBlocks,
        Set<Item> allowedTools,
        VeinMiningSettings settings,
        List<BlockPos> candidates
) {
}
