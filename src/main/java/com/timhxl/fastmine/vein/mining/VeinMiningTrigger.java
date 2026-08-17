package com.timhxl.fastmine.vein.mining;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.mining.MiningContext;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import com.timhxl.fastmine.vein.config.VeinMiningResolvedGroup;
import com.timhxl.fastmine.vein.config.VeinMiningSettings;
import com.timhxl.fastmine.vein.config.VeinMiningSettingsOverride;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 将玩家设置、VeinMiner 兼容配置和候选搜索连接为一次可执行计划。
 */
public final class VeinMiningTrigger {
    private VeinMiningTrigger() {
    }

    /**
     * 根据玩家刚刚手动破坏的锚点，生成连锁采集计划；不执行破坏。
     */
    public static Optional<VeinMiningPlan> plan(MiningContext context) {
        PlayerFastMineSettings playerSettings = context.playerSettings();
        if (!playerSettings.veinEnabled()) {
            return Optional.empty();
        }

        List<VeinMiningResolvedGroup> matchedGroups = FastMineMod.getVeinMiningRuleRegistry()
                .findGroups(context.brokenState());
        boolean standaloneBlock = FastMineMod.getVeinMiningRuleRegistry()
                .isStandaloneBlock(context.brokenState());
        if (matchedGroups.isEmpty() && !standaloneBlock) {
            return Optional.empty();
        }

        Set<Block> groupedBlocks = new LinkedHashSet<>();
        Set<Item> groupedTools = new LinkedHashSet<>();
        VeinMiningSettingsOverride firstOverride = null;
        for (VeinMiningResolvedGroup group : matchedGroups) {
            groupedBlocks.addAll(group.blocks());
            groupedTools.addAll(group.tools());
            if (firstOverride == null && group.override() != null) {
                firstOverride = group.override();
            }
        }

        VeinMiningSettings settings = FastMineMod.getVeinMiningConfigManager().getSettings()
                .withOverride(firstOverride);
        if (settings.mustSneak && !context.player().isCrouching()) {
            return Optional.empty();
        }
        // permissionRestricted 将在权限模块接入后由执行层统一处理。

        ItemStack heldItem = context.player().getMainHandItem();
        if (!groupedTools.isEmpty() && !groupedTools.contains(heldItem.getItem())) {
            return Optional.empty();
        }
        if (settings.needCorrectTool
                && context.brokenState().requiresCorrectToolForDrops()
                && !heldItem.isCorrectToolForDrops(context.brokenState())) {
            return Optional.empty();
        }

        Set<Block> targetBlocks = settings.separateGroupMining || matchedGroups.isEmpty()
                ? Set.of(context.brokenState().getBlock())
                : Set.copyOf(groupedBlocks);
        List<net.minecraft.core.BlockPos> candidates = VeinMiningPlanner.plan(
                context.level(),
                context.origin(),
                targetBlocks,
                settings
        );
        String groupName = matchedGroups.isEmpty() ? "standalone" : matchedGroups.getFirst().name();
        return Optional.of(new VeinMiningPlan(
                groupName,
                targetBlocks,
                Set.copyOf(groupedTools),
                settings,
                candidates
        ));
    }
}
