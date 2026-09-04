package com.timhxl.fastmine.mining;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.mining.area.AreaMiningExecutor;
import com.timhxl.fastmine.vein.mining.VeinMiningExecutor;
import com.timhxl.fastmine.vein.mining.VeinMiningActionBar;
import com.timhxl.fastmine.vein.mining.VeinMiningTrigger;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监听玩家正常完成的方块破坏，并建立 FastMine 后续处理所需的上下文。
 */
public final class MiningEventHandler {
    private static final Map<UUID, InitialBreakSnapshot> INITIAL_BREAK_SNAPSHOTS = new ConcurrentHashMap<>();
    private static final List<MiningDropSession> PENDING_DROP_SESSIONS = new ArrayList<>();

    private MiningEventHandler() {
    }

    /**
     * 注册服务端方块破坏事件。
     */
    public static void register() {
        MiningDirectionTracker.register();
        PlayerBlockBreakEvents.BEFORE.register(MiningEventHandler::beforePlayerBlockBroken);
        PlayerBlockBreakEvents.AFTER.register(MiningEventHandler::onPlayerBlockBroken);
        ServerTickEvents.END_SERVER_TICK.register(server -> finishPendingDropSessions());
    }

    /** 在原版生成锚点方块掉落物之前记录附近实体。 */
    private static boolean beforePlayerBlockBroken(Level level, Player player, BlockPos position,
                                                   BlockState state, BlockEntity blockEntity) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && !MiningOperationGuard.isActive()) {
            MiningDropSession snapshotSession = new MiningDropSession(serverPlayer, serverLevel, false, false, true, true);
            INITIAL_BREAK_SNAPSHOTS.put(serverPlayer.getUUID(), new InitialBreakSnapshot(
                    serverLevel, position.immutable(), snapshotSession.snapshot(position)));
        }
        return true;
    }

    private static void onPlayerBlockBroken(Level level, Player player, BlockPos position,
                                             BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (MiningOperationGuard.isActive()) {
            return;
        }

        InitialBreakSnapshot initialBreak = INITIAL_BREAK_SNAPSHOTS.remove(serverPlayer.getUUID());

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

            MiningDropSession dropSession = createDropSession(context, initialBreak, true);
            int destroyedCount;
            try {
                destroyedCount = VeinMiningExecutor.execute(context, plan, dropSession);
            } finally {
                finishDropSession(dropSession);
            }
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
            MiningDropSession dropSession = createDropSession(context, initialBreak, false);
            try {
                AreaMiningExecutor.execute(context, miningDirection, dropSession);
            } finally {
                finishDropSession(dropSession);
            }
        }
    }

    private static MiningDropSession createDropSession(MiningContext context, InitialBreakSnapshot initialBreak,
                                                       boolean veinMode) {
        // 管理员开关是最终权限：关闭后，玩家个人的掉落物聚合设置不得重新启用它。
        boolean aggregateDropsAtFeet = context.config().transferExtraDropsToPlayer
                && context.playerSettings().aggregateDropsAtFeet();
        boolean directExperience = context.config().directExperience
                && context.playerSettings().directExperience();
        if (!aggregateDropsAtFeet && !directExperience) {
            return null;
        }

        MiningDropSession session = new MiningDropSession(context.player(), context.level(),
                aggregateDropsAtFeet, directExperience,
                veinMode ? context.config().veinMiningConsumesDurability : context.config().areaMiningConsumesDurability,
                veinMode ? context.config().veinMiningConsumesHunger : context.config().areaMiningConsumesHunger);
        if (initialBreak != null && initialBreak.level() == context.level()
                && initialBreak.position().equals(context.origin())) {
            session.deferCollection(initialBreak.snapshot());
        }
        return session;
    }

    private static void finishDropSession(MiningDropSession session) {
        if (session != null) {
            PENDING_DROP_SESSIONS.add(session);
        }
    }

    /**
     * 必须等到当前服务器 tick 完结。锚点方块的原版战利品可能在 AFTER 回调及普通任务之后才生成；
     * 此时再按 BEFORE 快照收集，才能保证首方块和额外方块使用同一套聚合规则。
     */
    private static void finishPendingDropSessions() {
        if (PENDING_DROP_SESSIONS.isEmpty()) {
            return;
        }

        List<MiningDropSession> sessions = new ArrayList<>(PENDING_DROP_SESSIONS);
        PENDING_DROP_SESSIONS.clear();
        for (MiningDropSession session : sessions) {
            session.finish();
        }
    }

    private record InitialBreakSnapshot(ServerLevel level, BlockPos position, MiningDropSession.DropSnapshot snapshot) {
    }
}
