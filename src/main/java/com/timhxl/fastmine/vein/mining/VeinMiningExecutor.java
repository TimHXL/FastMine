package com.timhxl.fastmine.vein.mining;

import com.timhxl.fastmine.mining.MiningContext;
import com.timhxl.fastmine.mining.MiningOperationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 使用服务器原版玩家破坏流程执行一次连锁采集计划。
 */
public final class VeinMiningExecutor {
    private VeinMiningExecutor() {
    }

    /**
     * 依次破坏计划中的额外方块，并阻止这些破坏重新触发 FastMine 根事件。
     */
    public static int execute(MiningContext context, VeinMiningPlan plan) {
        if (plan.candidates().isEmpty()) {
            return 0;
        }

        int[] destroyedCount = {0};
        MiningOperationGuard.runProtected(() -> destroyedCount[0] = destroyCandidates(context, plan));
        return destroyedCount[0];
    }

    private static int destroyCandidates(MiningContext context, VeinMiningPlan plan) {
        int destroyedCount = 0;

        for (BlockPos position : plan.candidates()) {
            if (!context.player().isAlive()) {
                break;
            }

            BlockState currentState = context.level().getBlockState(position);
            if (!plan.targetBlocks().contains(currentState.getBlock())) {
                continue;
            }

            ItemStack heldItem = context.player().getMainHandItem();
            if (!plan.allowedTools().isEmpty() && !plan.allowedTools().contains(heldItem.getItem())) {
                break;
            }
            if (plan.settings().needCorrectTool
                    && currentState.requiresCorrectToolForDrops()
                    && !heldItem.isCorrectToolForDrops(currentState)) {
                break;
            }

            if (context.player().gameMode.destroyBlock(position)) {
                destroyedCount++;
            }
        }

        return destroyedCount;
    }
}
