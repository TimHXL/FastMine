package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineAdminConfigUpdatePayload;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/** 保存一次 FastMine 设置会话中的列表修改，并负责全局 Ctrl + Z 撤回。 */
public final class FastMineListEditHistory {
    private static final int MAX_HISTORY_SIZE = 64;
    private static final Deque<UndoAction> UNDO_ACTIONS = new ArrayDeque<>();

    private FastMineListEditHistory() {
    }

    /** 记录一个已发出的修改请求对应的反向操作。 */
    public static void recordInverse(FastMineAdminConfigUpdatePayload.Operation inverseOperation, int groupIndex, String value) {
        push(new UndoAction(inverseOperation, groupIndex, value));
    }

    /** 在任意 FastMine 设置页面处理 Ctrl + Z。 */
    public static boolean handleUndoKey(KeyEvent event) {
        if (event.key() != GLFW.GLFW_KEY_Z || (event.modifiers() & GLFW.GLFW_MOD_CONTROL) == 0) return false;
        UndoAction action = UNDO_ACTIONS.pollFirst();
        if (action == null) return false;
        FastMineClientNetworking.updateAdminConfig(action.inverseOperation(), action.groupIndex(), action.value());
        return true;
    }

    /** 关闭设置界面或断开服务器时清除本次会话的撤销记录。 */
    public static void clear() { UNDO_ACTIONS.clear(); }

    private static void push(UndoAction action) {
        UNDO_ACTIONS.addFirst(action);
        while (UNDO_ACTIONS.size() > MAX_HISTORY_SIZE) UNDO_ACTIONS.removeLast();
    }

    private record UndoAction(FastMineAdminConfigUpdatePayload.Operation inverseOperation, int groupIndex, String value) {
    }
}
