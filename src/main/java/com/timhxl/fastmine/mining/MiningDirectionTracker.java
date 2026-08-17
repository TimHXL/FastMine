package com.timhxl.fastmine.mining;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录玩家开始破坏方块时实际命中的方块面。
 */
public final class MiningDirectionTracker {
    private static final Map<UUID, TrackedDirection> DIRECTION_BY_PLAYER = new ConcurrentHashMap<>();

    private MiningDirectionTracker() {
    }

    /**
     * 注册带有方块面方向的攻击事件。
     */
    public static void register() {
        AttackBlockCallback.EVENT.register(MiningDirectionTracker::onBlockAttacked);
    }

    /**
     * 取得并删除与本次成功破坏相匹配的挖掘方向。
     */
    public static Optional<Direction> consume(ServerPlayer player, BlockPos position) {
        TrackedDirection trackedDirection = DIRECTION_BY_PLAYER.remove(player.getUUID());

        if (trackedDirection == null || !trackedDirection.position().equals(position)) {
            return Optional.empty();
        }

        return Optional.of(trackedDirection.miningDirection());
    }

    private static InteractionResult onBlockAttacked(Player player, Level level, InteractionHand hand,
                                                     BlockPos position, Direction hitFace) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            Direction miningDirection = hitFace.getOpposite();
            DIRECTION_BY_PLAYER.put(
                    serverPlayer.getUUID(),
                    new TrackedDirection(position.immutable(), miningDirection)
            );
        }

        return InteractionResult.PASS;
    }

    private record TrackedDirection(BlockPos position, Direction miningDirection) {
    }
}
