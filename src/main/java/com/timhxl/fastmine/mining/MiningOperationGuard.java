package com.timhxl.fastmine.mining;

/**
 * 标记 FastMine 发起的额外方块破坏，防止其再次触发 FastMine。
 */
public final class MiningOperationGuard {
    private static final ThreadLocal<Integer> OPERATION_DEPTH = ThreadLocal.withInitial(() -> 0);

    private MiningOperationGuard() {
    }

    /**
     * 当前线程是否正在执行 FastMine 发起的额外破坏。
     */
    public static boolean isActive() {
        return OPERATION_DEPTH.get() > 0;
    }

    /**
     * 在防递归保护范围内执行一次额外破坏操作。
     */
    public static void runProtected(Runnable action) {
        int previousDepth = OPERATION_DEPTH.get();
        OPERATION_DEPTH.set(previousDepth + 1);

        try {
            action.run();
        } finally {
            if (previousDepth == 0) {
                OPERATION_DEPTH.remove();
            } else {
                OPERATION_DEPTH.set(previousDepth);
            }
        }
    }
}
