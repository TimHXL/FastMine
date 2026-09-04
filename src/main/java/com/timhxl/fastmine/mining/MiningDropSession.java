package com.timhxl.fastmine.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 收集一次 FastMine 操作中新生成的掉落物和经验。
 *
 * <p>每次破坏前先记录范围内已有实体，之后只处理本次新生成的实体，避免移动地面上原有物品。
 * 所有被收集的同类物品会在操作结束后合并并生成在操作者脚下。</p>
 */
public final class MiningDropSession {
    private static final double SEARCH_SIZE = 2.0D;

    private final ServerPlayer player;
    private final ServerLevel level;
    private final boolean aggregateDropsAtFeet;
    private final boolean directExperience;
    private final boolean consumeDurability;
    private final boolean consumeHunger;
    private final List<ItemStack> collectedItems = new ArrayList<>();
    private final List<DropSnapshot> deferredSnapshots = new ArrayList<>();

    public MiningDropSession(ServerPlayer player, ServerLevel level, boolean aggregateDropsAtFeet,
                              boolean directExperience, boolean consumeDurability, boolean consumeHunger) {
        this.player = player;
        this.level = level;
        this.aggregateDropsAtFeet = aggregateDropsAtFeet;
        this.directExperience = directExperience;
        this.consumeDurability = consumeDurability;
        this.consumeHunger = consumeHunger;
    }

    /** 记录指定方块附近的现有实体。 */
    public DropSnapshot snapshot(BlockPos position) {
        AABB searchBox = AABB.ofSize(Vec3.atCenterOf(position), SEARCH_SIZE, SEARCH_SIZE, SEARCH_SIZE);
        Set<Integer> itemIds = new HashSet<>();
        Set<Integer> experienceIds = new HashSet<>();
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, searchBox)) {
            itemIds.add(itemEntity.getId());
        }
        for (ExperienceOrb experienceOrb : level.getEntitiesOfClass(ExperienceOrb.class, searchBox)) {
            experienceIds.add(experienceOrb.getId());
        }
        return new DropSnapshot(searchBox, itemIds, experienceIds);
    }

    /** 破坏一个额外方块并立即收集其产生的新实体。 */
    public boolean destroyBlock(BlockPos position) {
        DropSnapshot snapshot = snapshot(position);
        boolean destroyed = MiningBlockBreaker.destroyBlock(player, position, consumeDurability, consumeHunger);
        if (destroyed) {
            collectNewEntities(snapshot);
        }
        return destroyed;
    }

    /** 收集此前快照之后由原版破坏流程新生成的实体。 */
    public void collectNewEntities(DropSnapshot snapshot) {
        if (aggregateDropsAtFeet) {
            for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, snapshot.searchBox())) {
                if (!snapshot.itemIds().contains(itemEntity.getId()) && !itemEntity.isRemoved()) {
                    collectItem(itemEntity.getItem().copy());
                    itemEntity.discard();
                }
            }
        }

        if (directExperience) {
            for (ExperienceOrb experienceOrb : level.getEntitiesOfClass(ExperienceOrb.class, snapshot.searchBox())) {
                if (!snapshot.experienceIds().contains(experienceOrb.getId()) && !experienceOrb.isRemoved()) {
                    // 直接结算经验，不依赖经验球与玩家的距离或原版拾取冷却。
                    player.giveExperiencePoints(experienceOrb.getValue());
                    experienceOrb.discard();
                }
            }
        }
    }

    /**
     * 延后收集锚点方块的掉落物。Fabric 的 AFTER 事件可能早于该方块的实体掉落生成，
     * 因此必须在当前破坏流程结束后再读取一次。
     */
    public void deferCollection(DropSnapshot snapshot) {
        deferredSnapshots.add(snapshot);
    }

    /** 生成合并后的物品。经验已在收集时通过原版拾取逻辑结算。 */
    public void finish() {
        for (DropSnapshot snapshot : deferredSnapshots) {
            collectNewEntities(snapshot);
        }
        deferredSnapshots.clear();
        if (aggregateDropsAtFeet) {
            for (ItemStack itemStack : collectedItems) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), itemStack);
                itemEntity.setDeltaMovement(Vec3.ZERO);
                level.addFreshEntity(itemEntity);
            }
        }
    }

    private void collectItem(ItemStack source) {
        while (!source.isEmpty()) {
            ItemStack merged = null;
            for (ItemStack existing : collectedItems) {
                if (ItemStack.isSameItemSameComponents(existing, source)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    merged = existing;
                    break;
                }
            }

            if (merged == null) {
                int count = Math.min(source.getCount(), source.getMaxStackSize());
                collectedItems.add(source.copyWithCount(count));
                source.shrink(count);
            } else {
                int count = Math.min(source.getCount(), merged.getMaxStackSize() - merged.getCount());
                merged.grow(count);
                source.shrink(count);
            }
        }
    }

    /** 破坏前附近实体的不可变标记。 */
    public record DropSnapshot(AABB searchBox, Set<Integer> itemIds, Set<Integer> experienceIds) {
    }
}
