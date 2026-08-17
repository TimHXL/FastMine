package com.timhxl.fastmine.vein.config;

/**
 * 单个连锁采集组覆盖的服务器设置。
 *
 * <p>null 表示使用 settings.json 中的全局值。</p>
 */
public final class VeinMiningSettingsOverride {
    public Integer cooldown;
    public Boolean mustSneak;
    public Integer delay;
    public Integer maxChain;
    public Boolean needCorrectTool;
    public Integer searchRadius;
    public Boolean permissionRestricted;
    public Boolean decreaseDurability;
    public Double hungerPerBlock;
    public Double miningSpeedModifier;
    public Boolean separateGroupMining;
}
