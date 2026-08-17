package com.timhxl.fastmine.vein.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 读取和保存 Veinminer 兼容的连锁采集配置。
 *
 * <p>配置文件位于 config/fastmine/veinmining/，文件结构与 Veinminer 2.12.0 一致：
 * settings.json、blocks.json 和 groups.json。</p>
 */
public final class VeinMiningConfigManager {
    private static final Type STRING_SET_TYPE = new TypeToken<LinkedHashSet<String>>() {
    }.getType();
    private static final Type GROUP_LIST_TYPE = new TypeToken<ArrayList<VeinMiningGroup>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path directory;
    private final Path settingsPath;
    private final Path blocksPath;
    private final Path groupsPath;
    private final Logger logger;

    private VeinMiningSettings settings = new VeinMiningSettings();
    private Set<String> veinBlocks = new LinkedHashSet<>();
    private List<VeinMiningGroup> groups = createFallbackGroups();

    public VeinMiningConfigManager(Path directory, Logger logger) {
        this.directory = directory;
        this.settingsPath = directory.resolve("settings.json");
        this.blocksPath = directory.resolve("blocks.json");
        this.groupsPath = directory.resolve("groups.json");
        this.logger = logger;
    }

    public VeinMiningSettings getSettings() {
        return settings;
    }

    public Set<String> getVeinBlocks() {
        return Set.copyOf(veinBlocks);
    }

    public List<VeinMiningGroup> getGroups() {
        return List.copyOf(groups);
    }

    /**
     * 从磁盘读取配置；首次启动时创建 Veinminer 兼容的默认文件。
     */
    public void load() {
        try {
            Files.createDirectories(directory);
            settings = loadValue(settingsPath, VeinMiningSettings.class, new VeinMiningSettings());
            veinBlocks = loadValue(blocksPath, STRING_SET_TYPE, new LinkedHashSet<>());
            groups = loadValue(groupsPath, GROUP_LIST_TYPE, loadDefaultGroups());
            normalize();
            save();
            logger.info("Loaded FastMine vein mining configuration: {}", directory);
        } catch (Exception exception) {
            settings = new VeinMiningSettings();
            veinBlocks = new LinkedHashSet<>();
            groups = createFallbackGroups();
            normalize();
            logger.error("Failed to load FastMine vein mining configuration; safe defaults will be used.", exception);
        }
    }

    /**
     * 保存当前配置。后续管理员命令与重载命令将复用此入口。
     */
    public void save() {
        try {
            Files.createDirectories(directory);
            writeValue(settingsPath, settings);
            writeValue(blocksPath, veinBlocks);
            writeValue(groupsPath, groups);
        } catch (Exception exception) {
            logger.error("Failed to save FastMine vein mining configuration.", exception);
        }
    }

    private void normalize() {
        if (settings == null) {
            settings = new VeinMiningSettings();
        }
        settings.normalize();
        if (veinBlocks == null) {
            veinBlocks = new LinkedHashSet<>();
        }
        veinBlocks.removeIf(entry -> entry == null || entry.isBlank());
        if (groups == null || groups.isEmpty()) {
            groups = loadDefaultGroups();
        }
        groups.removeIf(group -> group == null);
        groups.forEach(VeinMiningGroup::normalize);
    }

    private <T> T loadValue(Path path, Type type, T fallback) throws Exception {
        if (!Files.exists(path)) {
            return fallback;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T value = gson.fromJson(reader, type);
            return value == null ? fallback : value;
        }
    }

    private void writeValue(Path path, Object value) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(value, writer);
        }
    }

    private List<VeinMiningGroup> loadDefaultGroups() {
        try (InputStream input = VeinMiningConfigManager.class.getResourceAsStream(
                "/assets/fastmine/veinmining/default_groups.json"
        )) {
            if (input == null) {
                return createFallbackGroups();
            }
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            List<VeinMiningGroup> loadedGroups = gson.fromJson(json, GROUP_LIST_TYPE);
            return loadedGroups == null || loadedGroups.isEmpty() ? createFallbackGroups() : loadedGroups;
        } catch (Exception exception) {
            logger.warn("Failed to read bundled Veinminer default groups; fallback values will be used.", exception);
            return createFallbackGroups();
        }
    }

    private static List<VeinMiningGroup> createFallbackGroups() {
        VeinMiningGroup ores = new VeinMiningGroup();
        ores.name = "Ores";
        ores.blocks.add("#c:ores");
        ores.tools.add("#minecraft:pickaxes");
        return new ArrayList<>(List.of(ores));
    }
}
