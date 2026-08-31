package com.timhxl.fastmine.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 FastMine 额外破坏操作新生成的掉落物转入执行挖掘的玩家背包。
 */
public final class MiningDropTransfer {
    private MiningDropTransfer() {
    }

    /**
     * 在执行原版破坏流程前记录附近已有掉落物，随后仅收取这次破坏新生成的掉落物。
     */
    public static boolean destroyBlock(ServerPlayer player, ServerLevel level, BlockPos position, boolean transferDrops) {
        AABB searchBox = AABB.ofSize(Vec3.atCenterOf(position), 2.0D, 2.0D, 2.0D);
        Set<Integer> existingDropIds = new HashSet<>();
        if (transferDrops) {
            for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, searchBox)) {
                existingDropIds.add(itemEntity.getId());
            }
        }

        boolean destroyed = player.gameMode.destroyBlock(position);
        if (destroyed && transferDrops) {
            transferNewDrops(player, level.getEntitiesOfClass(ItemEntity.class, searchBox), existingDropIds);
        }
        return destroyed;
    }

    private static void transferNewDrops(ServerPlayer player, List<ItemEntity> itemEntities, Set<Integer> existingDropIds) {
        for (ItemEntity itemEntity : itemEntities) {
            if (existingDropIds.contains(itemEntity.getId()) || itemEntity.isRemoved()) {
                continue;
            }

            player.getInventory().add(itemEntity.getItem());
            if (itemEntity.getItem().isEmpty()) {
                itemEntity.discard();
            }
        }
    }
}
