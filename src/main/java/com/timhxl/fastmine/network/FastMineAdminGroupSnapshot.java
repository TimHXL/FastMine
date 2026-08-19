package com.timhxl.fastmine.network;

import com.timhxl.fastmine.vein.config.VeinMiningGroup;

import java.util.List;

/** 发送给 OP 客户端的单个连锁采集组只读快照。 */
public record FastMineAdminGroupSnapshot(String name, List<String> blocks, List<String> tools) {
    public static FastMineAdminGroupSnapshot from(VeinMiningGroup group) {
        return new FastMineAdminGroupSnapshot(group.name, List.copyOf(group.blocks), List.copyOf(group.tools));
    }
}
