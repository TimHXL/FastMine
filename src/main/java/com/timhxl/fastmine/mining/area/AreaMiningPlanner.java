package com.timhxl.fastmine.mining.area;

import com.timhxl.fastmine.config.FastMineConfig;
import com.timhxl.fastmine.mining.MiningContext;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据范围挖矿规则计算候选方块位置。
 *
 * <p>该类只计算坐标，不读取方块状态，也不执行破坏操作。传入的方向必须是从
 * 玩家命中的方块面指向方块内部的实际挖掘方向。</p>
 */
public final class AreaMiningPlanner {
    private AreaMiningPlanner() {
    }

    /**
     * 计算本次范围挖矿的全部候选位置，包含原始方块所在位置。
     */
    public static List<BlockPos> calculateCandidates(MiningContext context, Direction miningDirection) {
        PlayerFastMineSettings settings = context.playerSettings();
        FastMineConfig config = context.config();
        int width = clamp(settings.areaWidth(), 1, config.maxAreaWidth);
        int height = clamp(settings.areaHeight(), 1, config.maxAreaHeight);
        int depth = getEffectiveDepth(settings, config, miningDirection);
        int widthStart = -(width / 2);
        int widthEnd = widthStart + width - 1;
        int heightStart = -(height / 2);
        int heightEnd = heightStart + height - 1;
        List<BlockPos> candidates = new ArrayList<>(width * height * depth);

        for (int depthOffset = 0; depthOffset < depth; depthOffset++) {
            BlockPos layerCenter = context.origin().relative(miningDirection, depthOffset);

            for (int firstOffset = widthStart; firstOffset <= widthEnd; firstOffset++) {
                for (int secondOffset = heightStart; secondOffset <= heightEnd; secondOffset++) {
                    candidates.add(createPosition(layerCenter, miningDirection, firstOffset, secondOffset));
                }
            }
        }

        return List.copyOf(candidates);
    }

    /**
     * 根据挖掘方向确定实际深度；垂直挖掘可由服务器全局配置强制限定深度。
     */
    private static int getEffectiveDepth(PlayerFastMineSettings settings, FastMineConfig config,
                                         Direction miningDirection) {
        int ordinaryDepth = clamp(settings.areaDepth(), 1, config.maxAreaDepth);

        if (!miningDirection.getAxis().isVertical() || !config.verticalMiningEnabled) {
            return ordinaryDepth;
        }

        return clamp(config.verticalMiningDepth, 1, config.maxAreaDepth);
    }

    /**
     * 将二维平面坐标按挖掘方向映射到世界坐标。
     */
    private static BlockPos createPosition(BlockPos layerCenter, Direction miningDirection,
                                           int firstOffset, int secondOffset) {
        return switch (miningDirection) {
            case NORTH, SOUTH -> layerCenter.offset(firstOffset, secondOffset, 0);
            case EAST, WEST -> layerCenter.offset(0, secondOffset, firstOffset);
            case UP, DOWN -> layerCenter.offset(firstOffset, 0, secondOffset);
        };
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
