package com.timhxl.fastmine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 负责读取和保存 FastMine 服务器全局配置。
 */
public final class ConfigManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Logger logger;
    private final Path configPath;
    private FastMineConfig config = new FastMineConfig();

    public ConfigManager(Path configPath, Logger logger) {
        this.configPath = configPath;
        this.logger = logger;
    }

    /**
     * 获取当前有效的服务器全局配置。
     */
    public FastMineConfig getConfig() {
        return config;
    }

    /**
     * 从磁盘加载配置；首次启动时自动创建默认配置文件。
     */
    public void load() {
        if (!Files.exists(configPath)) {
            config.normalize();
            save();
            logger.info("Created default FastMine configuration: {}", configPath);
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            FastMineConfig loadedConfig = gson.fromJson(reader, FastMineConfig.class);

            if (loadedConfig == null) {
                throw new IllegalStateException("Configuration file contains no usable data.");
            }

            loadedConfig.normalize();
            config = loadedConfig;
            save();
            logger.info("Loaded FastMine configuration: {}", configPath);
        } catch (Exception exception) {
            config = new FastMineConfig();
            config.normalize();
            logger.error("Failed to load FastMine configuration; safe defaults will be used.", exception);
        }
    }

    /**
     * 将当前配置保存到磁盘。
     */
    public void save() {
        try {
            Files.createDirectories(configPath.getParent());

            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                gson.toJson(config, writer);
            }
        } catch (Exception exception) {
            logger.error("Failed to save FastMine configuration.", exception);
        }
    }
}
