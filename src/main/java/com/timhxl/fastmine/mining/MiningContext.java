package com.timhxl.fastmine.mining;

import com.timhxl.fastmine.config.FastMineConfig;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 一次由玩家手动挖掘触发的 FastMine 处理上下文。
 *
 * <p>这里保存的是原始方块已被正常破坏后的状态。后续范围挖矿和连锁采集
 * 只能使用这个上下文发起额外破坏，不能自行重新触发新的 FastMine 流程。</p>
 */
public record MiningContext(
        ServerLevel level,
        ServerPlayer player,
        BlockPos origin,
        BlockState brokenState,
        PlayerFastMineSettings playerSettings,
        FastMineConfig config
) {
    /**
     * 判断玩家是否至少启用了一个挖矿模式。
     */
    public boolean hasEnabledMiningMode() {
        return playerSettings.areaEnabled() || playerSettings.veinEnabled();
    }
}
