package com.timhxl.fastmine.vein.mining;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 生成 Java 客户端可识别的原版颜色连锁提示。
 */
public final class VeinMiningActionBar {
    private VeinMiningActionBar() {
    }

    /**
     * 仅为成功连锁数量着色，其他文字保持白色。
     */
    public static Component create(Block originBlock, int destroyedCount) {
        MutableComponent message = Component.literal("本次连锁了").withStyle(ChatFormatting.WHITE);
        message.append(Component.literal(Integer.toString(destroyedCount)).withStyle(resolveOreColor(originBlock)));
        message.append(Component.literal("个方块").withStyle(ChatFormatting.WHITE));
        return message;
    }

    private static ChatFormatting resolveOreColor(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return ChatFormatting.AQUA;
        }
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return ChatFormatting.RED;
        }
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return ChatFormatting.GREEN;
        }
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return ChatFormatting.BLUE;
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) {
            return ChatFormatting.GOLD;
        }
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return ChatFormatting.GRAY;
        }
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return ChatFormatting.GOLD;
        }
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return ChatFormatting.DARK_GRAY;
        }
        if (block == Blocks.ANCIENT_DEBRIS) {
            return ChatFormatting.DARK_RED;
        }
        return ChatFormatting.WHITE;
    }
}
