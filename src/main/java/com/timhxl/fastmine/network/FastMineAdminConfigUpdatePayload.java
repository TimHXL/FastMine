package com.timhxl.fastmine.network;

import com.timhxl.fastmine.FastMineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** OP 客户端提交的单次连锁采集组或全局列表编辑操作。 */
public record FastMineAdminConfigUpdatePayload(Operation operation, int groupIndex, String value) implements CustomPacketPayload {
    private static final int MAX_TEXT_LENGTH = 256;

    public enum Operation {
        CREATE_GROUP,
        DELETE_GROUP,
        ADD_BLOCK,
        ADD_TOOL,
        REMOVE_BLOCK,
        REMOVE_TOOL,
        ADD_NATURAL_STONE,
        REMOVE_NATURAL_STONE,
        ADD_PROTECTED_STRUCTURE,
        REMOVE_PROTECTED_STRUCTURE,
        ADD_TARGET_BLOCK,
        ADD_HELD_TOOL
    }

    public static final Type<FastMineAdminConfigUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "admin_config_update")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FastMineAdminConfigUpdatePayload> CODEC = new StreamCodec<>() {
        @Override
        public FastMineAdminConfigUpdatePayload decode(RegistryFriendlyByteBuf buffer) {
            int operationIndex = buffer.readInt();
            if (operationIndex < 0 || operationIndex >= Operation.values().length) {
                throw new IllegalArgumentException("Invalid FastMine admin configuration operation.");
            }
            return new FastMineAdminConfigUpdatePayload(Operation.values()[operationIndex], buffer.readInt(),
                    buffer.readUtf(MAX_TEXT_LENGTH));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FastMineAdminConfigUpdatePayload payload) {
            buffer.writeInt(payload.operation().ordinal());
            buffer.writeInt(payload.groupIndex());
            buffer.writeUtf(payload.value(), MAX_TEXT_LENGTH);
        }
    };

    @Override
    public Type<FastMineAdminConfigUpdatePayload> type() {
        return TYPE;
    }
}
