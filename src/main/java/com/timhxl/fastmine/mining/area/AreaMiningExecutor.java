package com.timhxl.fastmine.mining.area;

import com.timhxl.fastmine.mining.MiningContext;
import com.timhxl.fastmine.mining.MiningDropTransfer;
import com.timhxl.fastmine.mining.MiningDropSession;
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
        execute(context, miningDirection, null);
    }

    /** 使用给定会话处理本次操作的掉落物与经验。 */
    public static void execute(MiningContext context, Direction miningDirection, MiningDropSession dropSession) {
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
        MiningOperationGuard.runProtected(() -> destroyAllowedPositions(context, allowedPositions, dropSession));
    }

    private static void destroyAllowedPositions(MiningContext context, List<BlockPos> allowedPositions,
                                                MiningDropSession dropSession) {
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

            if (dropSession != null) {
                dropSession.destroyBlock(position);
            } else {
                MiningDropTransfer.destroyBlock(player, context.level(), position,
                        context.config().transferExtraDropsToPlayer
                                && context.playerSettings().aggregateDropsAtFeet(),
                        context.config().areaMiningConsumesDurability,
                        context.config().areaMiningConsumesHunger);
            }
        }
    }

    private static boolean hasValidPickaxe(ServerPlayer player) {
        return player.getMainHandItem().is(ItemTags.PICKAXES);
    }
}
