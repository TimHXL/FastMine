package com.timhxl.fastmine.mining;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.mining.area.AreaMiningExecutor;
import com.timhxl.fastmine.vein.mining.VeinMiningExecutor;
import com.timhxl.fastmine.vein.mining.VeinMiningActionBar;
import com.timhxl.fastmine.vein.mining.VeinMiningTrigger;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 监听玩家正常完成的方块破坏，并建立 FastMine 后续处理所需的上下文。
 */
public final class MiningEventHandler {
    private MiningEventHandler() {
    }

    /**
     * 注册服务端方块破坏事件。
     */
    public static void register() {
        MiningDirectionTracker.register();
        PlayerBlockBreakEvents.AFTER.register(MiningEventHandler::onPlayerBlockBroken);
    }

    private static void onPlayerBlockBroken(Level level, Player player, BlockPos position,
                                             BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (MiningOperationGuard.isActive()) {
            return;
        }

        Direction miningDirection = MiningDirectionTracker.consume(serverPlayer, position).orElse(null);

        PlayerFastMineSettings settings = FastMineMod.getPlayerSettingsService().getOrCreate(
                serverPlayer.getUUID()
        );
        MiningContext context = new MiningContext(
                serverLevel,
                serverPlayer,
                position.immutable(),
                state,
                settings,
                FastMineMod.getConfigManager().getConfig()
        );

        var veinPlan = VeinMiningTrigger.plan(context);
        if (veinPlan.isPresent()) {
            var plan = veinPlan.get();
            if (plan.settings().debug) {
                FastMineMod.LOGGER.info(
                        "FastMine vein plan: player={}, block={}, candidates={}, group={}",
                        serverPlayer.getUUID(),
                        state.getBlock(),
                        plan.candidates().size(),
                        plan.groupName()
                );
            }

            int destroyedCount = VeinMiningExecutor.execute(context, plan);
            if (destroyedCount > 0) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        VeinMiningActionBar.create(state.getBlock(), destroyedCount)
                ));
            }
            return;
        }

        if (!context.hasEnabledMiningMode()) {
            return;
        }

        if (context.playerSettings().areaEnabled()
                && (!context.config().areaMustSneak || serverPlayer.isCrouching())
                && miningDirection != null) {
            AreaMiningExecutor.execute(context, miningDirection);
        }
    }
}
