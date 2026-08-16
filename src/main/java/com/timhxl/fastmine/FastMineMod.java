package com.timhxl.fastmine;

import com.timhxl.fastmine.command.FastMineCommands;
import com.timhxl.fastmine.player.PlayerSettingsService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * FastMine 服务端核心入口。
 */
public final class FastMineMod implements ModInitializer {
    public static final String MOD_ID = "fastmine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Path PLAYER_SETTINGS_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve(MOD_ID)
            .resolve("players.json");

    private static final PlayerSettingsService PLAYER_SETTINGS_SERVICE = new PlayerSettingsService(
            PLAYER_SETTINGS_PATH,
            LOGGER
    );

    /**
     * 获取服务器唯一的玩家设置服务。
     */
    public static PlayerSettingsService getPlayerSettingsService() {
        return PLAYER_SETTINGS_SERVICE;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("FastMine started: Fabric 26.2 server core loaded.");
        PLAYER_SETTINGS_SERVICE.load();
        FastMineCommands.register();
    }
}
