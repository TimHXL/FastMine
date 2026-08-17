package com.timhxl.fastmine.vein.config;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 Veinminer 兼容配置中的标识符和 #Tag 展开成服务端可直接匹配的规则。
 *
 * <p>规则仅在服务器注册表可用后解析，因此支持原版和其他 MOD 注册的方块、物品与 Tag。</p>
 */
public final class VeinMiningRuleRegistry {
    private final VeinMiningConfigManager configManager;
    private final Logger logger;
    private volatile List<VeinMiningResolvedGroup> groups = List.of();
    private volatile Set<Block> standaloneBlocks = Set.of();

    public VeinMiningRuleRegistry(VeinMiningConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    /**
     * 在服务器启动完成后加载并解析所有方块组、工具组和独立方块白名单。
     */
    public void reload(MinecraftServer server) {
        List<VeinMiningResolvedGroup> resolvedGroups = new ArrayList<>();

        for (VeinMiningGroup group : configManager.getGroups()) {
            Set<Block> blocks = resolveBlocks(server, group.blocks);
            Set<Item> tools = resolveItems(server, group.tools);

            if (blocks.isEmpty()) {
                logger.warn("FastMine vein mining group '{}' contains no valid blocks and will be ignored.", group.name);
                continue;
            }

            resolvedGroups.add(new VeinMiningResolvedGroup(
                    group.name,
                    Set.copyOf(blocks),
                    Set.copyOf(tools),
                    group.override
            ));
        }

        groups = List.copyOf(resolvedGroups);
        standaloneBlocks = Set.copyOf(resolveBlocks(server, configManager.getVeinBlocks()));
        logger.info(
                "Resolved FastMine vein mining rules: {} groups, {} standalone blocks.",
                groups.size(),
                standaloneBlocks.size()
        );
    }

    /**
     * 返回包含指定方块的全部组。后续执行器将据此决定连锁候选集合和组覆盖设置。
     */
    public List<VeinMiningResolvedGroup> findGroups(BlockState state) {
        return groups.stream().filter(group -> group.contains(state)).toList();
    }

    /**
     * 判断方块是否在 blocks.json 的独立连锁采集白名单中。
     */
    public boolean isStandaloneBlock(BlockState state) {
        return standaloneBlocks.contains(state.getBlock());
    }

    private Set<Block> resolveBlocks(MinecraftServer server, Collection<String> entries) {
        Set<Block> result = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            boolean tagEntry = entry.startsWith("#");
            Identifier identifier = Identifier.tryParse(tagEntry ? entry.substring(1) : entry);
            if (identifier == null) {
                logger.warn("Invalid FastMine vein mining block entry: {}", entry);
                continue;
            }

            if (!tagEntry) {
                if (BuiltInRegistries.BLOCK.containsKey(identifier)) {
                    result.add(BuiltInRegistries.BLOCK.getValue(identifier));
                } else {
                    logger.warn("Unknown FastMine vein mining block: {}", entry);
                }
                continue;
            }

            TagKey<Block> tag = TagKey.create(Registries.BLOCK, identifier);
            for (Holder<Block> holder : server.registryAccess().lookupOrThrow(Registries.BLOCK).getTagOrEmpty(tag)) {
                result.add(holder.value());
            }
        }
        return result;
    }

    private Set<Item> resolveItems(MinecraftServer server, Collection<String> entries) {
        Set<Item> result = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            boolean tagEntry = entry.startsWith("#");
            Identifier identifier = Identifier.tryParse(tagEntry ? entry.substring(1) : entry);
            if (identifier == null) {
                logger.warn("Invalid FastMine vein mining tool entry: {}", entry);
                continue;
            }

            if (!tagEntry) {
                if (BuiltInRegistries.ITEM.containsKey(identifier)) {
                    result.add(BuiltInRegistries.ITEM.getValue(identifier));
                } else {
                    logger.warn("Unknown FastMine vein mining tool: {}", entry);
                }
                continue;
            }

            TagKey<Item> tag = TagKey.create(Registries.ITEM, identifier);
            for (Holder<Item> holder : server.registryAccess().lookupOrThrow(Registries.ITEM).getTagOrEmpty(tag)) {
                result.add(holder.value());
            }
        }
        return result;
    }
}
