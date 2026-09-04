package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** 服务端发送给 OP 客户端的 groups.json 只读快照。 */
public record FastMineAdminConfigSyncPayload(List<FastMineAdminGroupSnapshot> groups,
                                             FastMineAdminGlobalConfigSnapshot global) implements CustomPacketPayload {
    private static final int MAX_GROUPS = 256;
    private static final int MAX_ENTRIES_PER_GROUP = 512;
    private static final int MAX_TEXT_LENGTH = 256;

    public static final Type<FastMineAdminConfigSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "admin_config_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineAdminConfigSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public FastMineAdminConfigSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            int groupCount = readCount(buffer, MAX_GROUPS);
            List<FastMineAdminGroupSnapshot> groups = new ArrayList<>(groupCount);
            for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                String name = buffer.readUtf(MAX_TEXT_LENGTH);
                List<String> blocks = readEntries(buffer);
                List<String> tools = readEntries(buffer);
                groups.add(new FastMineAdminGroupSnapshot(name, blocks, tools));
            }
            FastMineAdminGlobalConfigSnapshot global = new FastMineAdminGlobalConfigSnapshot(
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readInt(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), readEntries(buffer), readEntries(buffer)
            );
            return new FastMineAdminConfigSyncPayload(List.copyOf(groups), global);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FastMineAdminConfigSyncPayload payload) {
            buffer.writeInt(payload.groups().size());
            for (FastMineAdminGroupSnapshot group : payload.groups()) {
                buffer.writeUtf(group.name(), MAX_TEXT_LENGTH);
                writeEntries(buffer, group.blocks());
                writeEntries(buffer, group.tools());
            }
            FastMineAdminGlobalConfigSnapshot global = payload.global();
            buffer.writeInt(global.minAreaWidth());
            buffer.writeInt(global.minAreaHeight());
            buffer.writeInt(global.minAreaDepth());
            buffer.writeInt(global.maxAreaWidth());
            buffer.writeInt(global.maxAreaHeight());
            buffer.writeInt(global.maxAreaDepth());
            buffer.writeInt(global.defaultAreaWidth());
            buffer.writeInt(global.defaultAreaHeight());
            buffer.writeInt(global.defaultAreaDepth());
            buffer.writeBoolean(global.verticalMiningEnabled());
            buffer.writeInt(global.verticalMiningDepth());
            buffer.writeBoolean(global.structureProtectionEnabled());
            buffer.writeBoolean(global.transferExtraDropsToPlayer());
            buffer.writeBoolean(global.directExperience());
            buffer.writeBoolean(global.veinMiningConsumesDurability());
            buffer.writeBoolean(global.veinMiningConsumesHunger());
            buffer.writeBoolean(global.areaMiningConsumesDurability());
            buffer.writeBoolean(global.areaMiningConsumesHunger());
            writeEntries(buffer, global.naturalStoneBlocks());
            writeEntries(buffer, global.protectedStructures());
        }
    };

    private static List<String> readEntries(RegistryFriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_ENTRIES_PER_GROUP);
        List<String> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(buffer.readUtf(MAX_TEXT_LENGTH));
        }
        return List.copyOf(entries);
    }

    private static void writeEntries(RegistryFriendlyByteBuf buffer, List<String> entries) {
        buffer.writeInt(entries.size());
        for (String entry : entries) {
            buffer.writeUtf(entry, MAX_TEXT_LENGTH);
        }
    }

    private static int readCount(RegistryFriendlyByteBuf buffer, int maximum) {
        int count = buffer.readInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid FastMine admin configuration payload count.");
        }
        return count;
    }

    @Override
    public Type<FastMineAdminConfigSyncPayload> type() {
        return TYPE;
    }
}
