package com.timhxl.fastmine.mining.area;

import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.config.FastMineConfig;
import com.timhxl.fastmine.mining.MiningContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用服务器配置指定的方块 Tag，筛选允许范围挖矿的天然石材。
 */
public final class NaturalStoneFilter {
    private static final TagKey<Block> DEFAULT_NATURAL_STONES = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "natural_stones")
    );

    private NaturalStoneFilter() {
    }

    /**
     * 从候选位置中筛选出当前实际存在且属于天然石材 Tag 的方块。
     */
    public static List<BlockPos> filterAllowedPositions(MiningContext context, List<BlockPos> candidates) {
        TagKey<Block> naturalStoneTag = resolveTag(context.config());
        List<BlockPos> allowedPositions = new ArrayList<>();

        for (BlockPos position : candidates) {
            BlockState state = context.level().getBlockState(position);

            if (isAllowed(state, context.config())) {
                allowedPositions.add(position.immutable());
            }
        }

        return List.copyOf(allowedPositions);
    }

    /**
     * 判断一个方块状态是否属于当前配置指定的天然石材 Tag。
     */
    public static boolean isAllowed(BlockState state, FastMineConfig config) {
        if (state.is(resolveTag(config))) return true;
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return identifier != null && config.naturalStoneBlocks.contains(identifier.toString());
    }

    private static TagKey<Block> resolveTag(FastMineConfig config) {
        try {
            return TagKey.create(Registries.BLOCK, Identifier.parse(config.naturalStoneTag));
        } catch (IllegalArgumentException exception) {
            return DEFAULT_NATURAL_STONES;
        }
    }
}
