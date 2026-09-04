package com.timhxl.fastmine.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** 执行一次 FastMine 自动方块破坏，并按服务器开关恢复被禁止的消耗。 */
public final class MiningBlockBreaker {
    private MiningBlockBreaker() {
    }

    /**
     * 使用原版破坏流程，并为 FastMine 自动破坏明确处理两项消耗。
     * 原始玩家手动破坏不经过此方法，因此不会被 FastMine 开关影响。
     */
    public static boolean destroyBlock(ServerPlayer player, BlockPos position,
                                       boolean consumeDurability, boolean consumeHunger) {
        ItemStack originalMainHand = player.getMainHandItem().copy();

        boolean destroyed = player.gameMode.destroyBlock(position);
        if (!destroyed) {
            return false;
        }

        if (!consumeDurability) {
            player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
        }
        if (consumeHunger) {
            // gameMode.destroyBlock() 不会替自动破坏流程增加挖掘 exhaustion，需在此补上原版数值。
            player.causeFoodExhaustion(0.005F);
        }
        return true;
    }
}
