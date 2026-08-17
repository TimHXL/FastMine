package com.timhxl.fastmine.vein.config;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Veinminer 兼容的方块组与工具组定义。
 *
 * <p>blocks 和 tools 可使用方块、物品或 Tag 标识符，例如 #c:ores 与 #minecraft:pickaxes。</p>
 */
public final class VeinMiningGroup {
    public String name = "Unnamed";
    public Set<String> blocks = new LinkedHashSet<>();
    public Set<String> tools = new LinkedHashSet<>();
    public VeinMiningSettingsOverride override = new VeinMiningSettingsOverride();

    /**
     * 修正空集合与空名称，避免后续解析时出现空指针。
     */
    public void normalize() {
        if (name == null || name.isBlank()) {
            name = "Unnamed";
        }
        if (blocks == null) {
            blocks = new LinkedHashSet<>();
        }
        if (tools == null) {
            tools = new LinkedHashSet<>();
        }
        if (override == null) {
            override = new VeinMiningSettingsOverride();
        }
        blocks.removeIf(entry -> entry == null || entry.isBlank());
        tools.removeIf(entry -> entry == null || entry.isBlank());
    }
}
