package com.timhxl.fastmine.vein.mining;

import com.timhxl.fastmine.vein.config.VeinMiningSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Veinminer 普通模式的三维广度优先连锁候选搜索。
 *
 * <p>本类只计算与锚点相连的目标方块，不执行破坏、不处理掉落，也不触发事件。
 * maxChain 与 Veinminer 的语义一致：锚点本身计入最大数量，因此返回列表最多为 maxChain - 1 个额外方块。</p>
 */
public final class VeinMiningPlanner {
    private VeinMiningPlanner() {
    }

    /**
     * 从锚点开始搜索属于目标集合的三维连通方块。
     *
     * @param level 当前服务端世界
     * @param origin 玩家手动破坏的锚点
     * @param targetBlocks 当前组允许连锁的实际方块集合
     * @param settings 当前组覆盖后的有效设置
     * @return 不含锚点、按距离由近到远排序的额外候选方块
     */
    public static List<BlockPos> plan(Level level, BlockPos origin, Set<Block> targetBlocks, VeinMiningSettings settings) {
        if (targetBlocks.isEmpty() || settings.maxChain <= 1) {
            return List.of();
        }

        int searchRadius = Math.clamp(settings.searchRadius, 1, 5);
        int maxChain = Math.max(1, settings.maxChain);
        Set<BlockPos> visited = new LinkedHashSet<>();
        Set<BlockPos> queued = new LinkedHashSet<>();
        Deque<SearchNode> queue = new ArrayDeque<>();
        List<BlockPos> candidates = new ArrayList<>();
        queue.addLast(new SearchNode(origin.immutable(), 0));
        queued.add(origin.immutable());

        while (!queue.isEmpty() && visited.size() < maxChain) {
            SearchNode current = queue.removeFirst();
            if (!visited.add(current.position())) {
                continue;
            }

            if (!current.position().equals(origin)) {
                candidates.add(current.position());
            }

            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int y = -searchRadius; y <= searchRadius; y++) {
                    for (int z = -searchRadius; z <= searchRadius; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }

                        BlockPos next = current.position().offset(x, y, z).immutable();
                        if (!queued.add(next) || !targetBlocks.contains(level.getBlockState(next).getBlock())) {
                            continue;
                        }
                        queue.addLast(new SearchNode(next, current.distance() + 1));
                    }
                }
            }
        }

        return List.copyOf(candidates);
    }

    /**
     * 保留距离字段，供后续按 Veinminer delay 规则分层调度破坏时使用。
     */
    private record SearchNode(BlockPos position, int distance) {
    }
}
