package com.timhxl.fastmine.vein.config;

/**
 * 连锁采集的服务器全局设置。
 *
 * <p>字段名与 Veinminer 2.12.0 的服务端 settings.json 保持一致，便于迁移既有配置。</p>
 */
public final class VeinMiningSettings {
    public int cooldown = 20;
    public boolean mustSneak = false;
    public int delay = 0;
    public int maxChain = 100;
    public boolean needCorrectTool = true;
    public int searchRadius = 1;
    public boolean permissionRestricted = false;
    public boolean mergeItemDrops = false;
    public boolean autoUpdate = false;
    public boolean decreaseDurability = true;
    public double hungerPerBlock = 0.0;
    public double miningSpeedModifier = 0.0;
    public boolean separateGroupMining = false;
    public boolean debug = false;

    /**
     * 创建一份应用了方块组覆盖项的有效设置。
     *
     * <p>覆盖项为 {@code null} 的字段继续使用 settings.json 的全局值。
     * 原对象不会被修改，避免不同方块组在同一服务器上互相污染设置。</p>
     */
    public VeinMiningSettings withOverride(VeinMiningSettingsOverride override) {
        VeinMiningSettings result = new VeinMiningSettings();
        result.cooldown = override == null || override.cooldown == null ? cooldown : override.cooldown;
        result.mustSneak = override == null || override.mustSneak == null ? mustSneak : override.mustSneak;
        result.delay = override == null || override.delay == null ? delay : override.delay;
        result.maxChain = override == null || override.maxChain == null ? maxChain : override.maxChain;
        result.needCorrectTool = override == null || override.needCorrectTool == null
                ? needCorrectTool : override.needCorrectTool;
        result.searchRadius = override == null || override.searchRadius == null
                ? searchRadius : override.searchRadius;
        result.permissionRestricted = override == null || override.permissionRestricted == null
                ? permissionRestricted : override.permissionRestricted;
        result.mergeItemDrops = mergeItemDrops;
        result.autoUpdate = autoUpdate;
        result.decreaseDurability = override == null || override.decreaseDurability == null
                ? decreaseDurability : override.decreaseDurability;
        result.hungerPerBlock = override == null || override.hungerPerBlock == null
                ? hungerPerBlock : override.hungerPerBlock;
        result.miningSpeedModifier = override == null || override.miningSpeedModifier == null
                ? miningSpeedModifier : override.miningSpeedModifier;
        result.separateGroupMining = override == null || override.separateGroupMining == null
                ? separateGroupMining : override.separateGroupMining;
        result.debug = debug;
        result.normalize();
        return result;
    }

    /**
     * 将配置中的非法数值收敛到安全范围。
     */
    public void normalize() {
        cooldown = Math.max(0, cooldown);
        delay = Math.max(0, delay);
        maxChain = Math.max(1, maxChain);
        searchRadius = Math.max(1, searchRadius);
        hungerPerBlock = Math.max(0.0, finiteOrZero(hungerPerBlock));
        miningSpeedModifier = finiteOrZero(miningSpeedModifier);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
