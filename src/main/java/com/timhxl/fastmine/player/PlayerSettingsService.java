package com.timhxl.fastmine.player;
import com.timhxl.fastmine.config.ConfigManager;
import com.timhxl.fastmine.config.FastMineConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理玩家 FastMine 个人设置，并负责按 UUID 持久化到磁盘。
 */
public final class PlayerSettingsService {
    private static final Type STORED_SETTINGS_TYPE = new TypeToken<Map<String, PlayerFastMineSettings>>() {
    }.getType();

    private final Map<UUID, PlayerFastMineSettings> settingsByPlayer = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Logger logger;
    private final Path settingsPath;
    private final ConfigManager configManager;

    public PlayerSettingsService(Path settingsPath, Logger logger, ConfigManager configManager) {
        this.settingsPath = settingsPath;
        this.logger = logger;
        this.configManager = configManager;
    }

    /**
     * 获取玩家设置；首次使用时创建默认设置并立即保存。
     */
    public synchronized PlayerFastMineSettings getOrCreate(UUID playerId) {
        PlayerFastMineSettings settings = settingsByPlayer.get(playerId);

        if (settings != null) {
            return settings;
        }

        PlayerFastMineSettings defaultSettings = PlayerFastMineSettings.createDefault(
                configManager.getConfig()
        );
        settingsByPlayer.put(playerId, defaultSettings);
        save();
        return defaultSettings;
    }

    /**
     * 更新玩家设置并立即保存。
     */
    public synchronized void update(UUID playerId, PlayerFastMineSettings settings) {
        settingsByPlayer.put(playerId, settings);
        save();
    }

    /**
     * 接收客户端提交的个人设置，并以服务器全局限制进行校验。
     *
     * <p>客户端界面仅是操作入口；该方法是玩家设置修改的服务端权威边界。</p>
     *
     * @throws IllegalArgumentException 当范围尺寸不符合服务器规则时抛出
     */
    public synchronized PlayerFastMineSettings updateFromClient(UUID playerId, boolean veinEnabled, boolean areaEnabled,
                                                                 int areaWidth, int areaHeight, int areaDepth,
                                                                 boolean aggregateDropsAtFeet, boolean directExperience) {
        FastMineConfig config = configManager.getConfig();

        if (!isValidOddSize(areaWidth, config.minAreaWidth, config.maxAreaWidth)
                || !isValidOddSize(areaHeight, config.minAreaHeight, config.maxAreaHeight)
                || !isValidDepth(areaDepth, config.minAreaDepth, config.maxAreaDepth)) {
            throw new IllegalArgumentException("Invalid FastMine area size from client.");
        }

        PlayerFastMineSettings updated = new PlayerFastMineSettings(
                veinEnabled,
                areaEnabled,
                areaWidth,
                areaHeight,
                areaDepth,
                aggregateDropsAtFeet,
                directExperience
        );
        update(playerId, updated);
        return updated;
    }

    /**
     * 判断范围尺寸是否为服务器允许的正奇数。
     */
    private static boolean isValidOddSize(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum && value % 2 != 0;
    }

    private static boolean isValidDepth(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    /**
     * 从磁盘读取已有玩家设置；读取失败时保留安全默认状态。
     */
    public synchronized void load() {
        if (!Files.exists(settingsPath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(settingsPath, StandardCharsets.UTF_8)) {
            Map<String, PlayerFastMineSettings> storedSettings = gson.fromJson(reader, STORED_SETTINGS_TYPE);

            if (storedSettings == null) {
                return;
            }

            settingsByPlayer.clear();

            for (Map.Entry<String, PlayerFastMineSettings> entry : storedSettings.entrySet()) {
                try {
                    settingsByPlayer.put(UUID.fromString(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException exception) {
                    logger.warn("Ignoring invalid FastMine player UUID: {}", entry.getKey());
                }
            }

            logger.info("Loaded {} FastMine player settings.", settingsByPlayer.size());
        } catch (Exception exception) {
            logger.error("Failed to load FastMine player settings; safe defaults will be used.", exception);
            settingsByPlayer.clear();
        }
    }

    /**
     * 将全部玩家设置写入磁盘。
     */
    private void save() {
        try {
            Files.createDirectories(settingsPath.getParent());

            Map<String, PlayerFastMineSettings> storedSettings = new TreeMap<>();

            for (Map.Entry<UUID, PlayerFastMineSettings> entry : settingsByPlayer.entrySet()) {
                storedSettings.put(entry.getKey().toString(), entry.getValue());
            }

            try (Writer writer = Files.newBufferedWriter(settingsPath, StandardCharsets.UTF_8)) {
                gson.toJson(storedSettings, STORED_SETTINGS_TYPE, writer);
            }
        } catch (Exception exception) {
            logger.error("Failed to save FastMine player settings.", exception);
        }
    }
}
