package com.timhxl.fastmine.mining.area;

import com.timhxl.fastmine.mining.MiningContext;
import com.timhxl.fastmine.mining.MiningOperationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 执行一次经过范围限制和天然石材筛选的额外方块破坏。
 */
public final class AreaMiningExecutor {
    private AreaMiningExecutor() {
    }

    /**
     * 使用玩家原版游戏模式依次破坏允许的候选方块。
     */
    public static void execute(MiningContext context, Direction miningDirection) {
        if (!context.playerSettings().areaEnabled()) {
            return;
        }

        if (!NaturalStoneFilter.isAllowed(context.brokenState(), context.config())) {
            return;
        }

        if (!hasValidPickaxe(context.player())) {
            return;
        }

        List<BlockPos> candidates = AreaMiningPlanner.calculateCandidates(context, miningDirection);
        List<BlockPos> allowedPositions = NaturalStoneFilter.filterAllowedPositions(context, candidates);
        MiningOperationGuard.runProtected(() -> destroyAllowedPositions(context, allowedPositions));
    }

    private static void destroyAllowedPositions(MiningContext context, List<BlockPos> allowedPositions) {
        ServerPlayer player = context.player();

        for (BlockPos position : allowedPositions) {
            if (position.equals(context.origin())) {
                continue;
            }

            if (!hasValidPickaxe(player)) {
                break;
            }

            BlockState currentState = context.level().getBlockState(position);

            if (!NaturalStoneFilter.isAllowed(currentState, context.config())) {
                continue;
            }
            if (StructureProtectionFilter.isProtected(context.level(), position, context.config())) {
                continue;
            }

            player.gameMode.destroyBlock(position);
        }
    }

    private static boolean hasValidPickaxe(ServerPlayer player) {
        return player.getMainHandItem().is(ItemTags.PICKAXES);
    }
}
