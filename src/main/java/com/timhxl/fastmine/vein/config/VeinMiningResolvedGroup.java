package com.timhxl.fastmine.vein.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * 已把方块和工具 Tag 展开后的连锁采集组。
 */
public record VeinMiningResolvedGroup(
        String name,
        Set<net.minecraft.world.level.block.Block> blocks,
        Set<net.minecraft.world.item.Item> tools,
        VeinMiningSettingsOverride override
) {
    /**
     * 判断当前方块状态是否属于此组。
     */
    public boolean contains(BlockState state) {
        return blocks.contains(state.getBlock());
    }

    /**
     * 判断主手工具是否可用于此组；空工具组表示不额外限制工具种类。
     */
    public boolean acceptsTool(ItemStack stack) {
        return tools.isEmpty() || tools.contains(stack.getItem());
    }
}
