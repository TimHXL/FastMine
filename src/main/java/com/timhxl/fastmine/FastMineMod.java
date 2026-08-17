package com.timhxl.fastmine;

import com.timhxl.fastmine.command.FastMineCommands;
import com.timhxl.fastmine.config.ConfigManager;
import com.timhxl.fastmine.mining.MiningEventHandler;
import com.timhxl.fastmine.player.PlayerSettingsService;
import com.timhxl.fastmine.vein.config.VeinMiningConfigManager;
import com.timhxl.fastmine.vein.config.VeinMiningRuleRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * FastMine 服务端核心入口。
 */
public final class FastMineMod implements ModInitializer {
    public static final String MOD_ID = "fastmine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Path CONFIG_DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

    private static final Path CONFIG_PATH = CONFIG_DIRECTORY.resolve("config.json");

    private static final ConfigManager CONFIG_MANAGER = new ConfigManager(
            CONFIG_PATH,
            LOGGER
    );

    private static final Path PLAYER_SETTINGS_PATH = CONFIG_DIRECTORY.resolve("players.json");

    private static final PlayerSettingsService PLAYER_SETTINGS_SERVICE = new PlayerSettingsService(
            PLAYER_SETTINGS_PATH,
            LOGGER,
            CONFIG_MANAGER
    );

    private static final VeinMiningConfigManager VEIN_MINING_CONFIG_MANAGER = new VeinMiningConfigManager(
            CONFIG_DIRECTORY.resolve("veinmining"),
            LOGGER
    );

    private static final VeinMiningRuleRegistry VEIN_MINING_RULE_REGISTRY = new VeinMiningRuleRegistry(
            VEIN_MINING_CONFIG_MANAGER,
            LOGGER
    );

    public static ConfigManager getConfigManager() {
        return CONFIG_MANAGER;
    }

    /**
     * 获取服务器唯一的玩家设置服务。
     */
    public static PlayerSettingsService getPlayerSettingsService() {
        return PLAYER_SETTINGS_SERVICE;
    }

    /**
     * 获取 VeinMiner 兼容连锁采集配置。
     */
    public static VeinMiningConfigManager getVeinMiningConfigManager() {
        return VEIN_MINING_CONFIG_MANAGER;
    }

    /**
     * 获取已经解析为服务器注册表对象的连锁采集规则。
     */
    public static VeinMiningRuleRegistry getVeinMiningRuleRegistry() {
        return VEIN_MINING_RULE_REGISTRY;
    }

    /**
     * 重读服务器全局配置，并重新解析连锁采集规则。
     *
     * <p>玩家个人开关和个人范围尺寸不在此处重载。</p>
     */
    public static void reloadServerConfiguration(MinecraftServer server) {
        CONFIG_MANAGER.load();
        VEIN_MINING_CONFIG_MANAGER.load();
        VEIN_MINING_RULE_REGISTRY.reload(server);
        LOGGER.info("FastMine server configuration reloaded.");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("FastMine started: Fabric 26.2 server core loaded.");
        CONFIG_MANAGER.load();
        PLAYER_SETTINGS_SERVICE.load();
        VEIN_MINING_CONFIG_MANAGER.load();
        ServerLifecycleEvents.SERVER_STARTED.register(VEIN_MINING_RULE_REGISTRY::reload);
        FastMineCommands.register();
        MiningEventHandler.register();
    }
}
