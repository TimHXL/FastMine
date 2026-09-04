package com.timhxl.fastmine.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.timhxl.fastmine.FastMineMod;
import com.timhxl.fastmine.config.FastMineConfig;
import com.timhxl.fastmine.player.PlayerFastMineSettings;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * FastMine 玩家命令。
 */
public final class FastMineCommands {
    private FastMineCommands() {
    }

    /**
     * 注册 FastMine 命令。
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("fastmine")
                        .executes(FastMineCommands::executeStatus)
                        .then(Commands.literal("status").executes(FastMineCommands::executeStatus))
                        .then(Commands.literal("help").executes(FastMineCommands::executeHelp))
                        .then(Commands.literal("reload")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(FastMineCommands::executeReload))
                        .then(Commands.literal("vein")
                                .then(Commands.literal("on").executes(context -> executeSetVein(context, true)))
                                .then(Commands.literal("off").executes(context -> executeSetVein(context, false)))
                                .then(Commands.literal("sneak")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .executes(FastMineCommands::executeVeinSneakStatus)
                                        .then(Commands.literal("on").executes(context -> executeSetVeinMustSneak(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetVeinMustSneak(context, false)))))
                                .then(Commands.literal("durability")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(Commands.literal("on").executes(context -> executeSetVeinDurability(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetVeinDurability(context, false))))
                                .then(Commands.literal("hunger")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(Commands.literal("on").executes(context -> executeSetVeinHunger(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetVeinHunger(context, false))))
                        .then(Commands.literal("drops")
                                .then(Commands.literal("on").executes(context -> executeSetAggregateDrops(context, true)))
                                .then(Commands.literal("off").executes(context -> executeSetAggregateDrops(context, false))))
                        .then(Commands.literal("experience")
                                .then(Commands.literal("on").executes(context -> executeSetDirectExperience(context, true)))
                                .then(Commands.literal("off").executes(context -> executeSetDirectExperience(context, false)))
                                .then(Commands.literal("global")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(Commands.literal("on").executes(context -> executeSetGlobalExperience(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetGlobalExperience(context, false)))))
                        .then(Commands.literal("area")
                                .then(Commands.literal("on").executes(context -> executeSetArea(context, true)))
                                .then(Commands.literal("off").executes(context -> executeSetArea(context, false)))
                                .then(Commands.literal("sneak")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .executes(FastMineCommands::executeAreaSneakStatus)
                                        .then(Commands.literal("on").executes(context -> executeSetAreaMustSneak(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetAreaMustSneak(context, false))))
                                .then(Commands.literal("durability")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(Commands.literal("on").executes(context -> executeSetAreaDurability(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetAreaDurability(context, false))))
                                .then(Commands.literal("hunger")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(Commands.literal("on").executes(context -> executeSetAreaHunger(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetAreaHunger(context, false))))
                                .then(Commands.literal("size")
                                        .then(Commands.argument("width", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("height", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("depth", IntegerArgumentType.integer(1))
                                                                .executes(FastMineCommands::executeSetAreaSize))))))
        ));
    }

    /**
     * 输出 FastMine 中文命令帮助。
     */
    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6=== FastMine 命令帮助 ==="), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine§r 或 §e/fastmine status§r：查看自己的当前设置。"), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine vein on|off§r：开启或关闭自己的连锁采集。"), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine area on|off§r：开启或关闭自己的范围挖掘。"), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine drops on|off§r：开启或关闭本次快速挖矿掉落物聚合到脚下。"), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine experience on|off§r：开启或关闭额外经验直接获得。"), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine area size <宽> <高> <深>"), false);
        source.sendSuccess(() -> Component.literal("  §7设置范围尺寸：宽、高必须为奇数；深度可为偶数。"), false);
        source.sendSuccess(() -> Component.literal("§c管理员命令："), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine reload§r：重新加载服务器配置与连锁采集规则。"), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine vein sneak [on|off]§r：查看或设置连锁采集是否必须蹲下。"), false);
        source.sendSuccess(() -> Component.literal("§e/fastmine area sneak [on|off]§r：查看或设置范围挖掘是否必须蹲下。"), false);
        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
            source.sendSuccess(() -> Component.literal("§e/fastmine experience global on|off§r：管理员设置是否允许额外经验直接获得。"), false);
            source.sendSuccess(() -> Component.literal("§e/fastmine vein durability on|off§r：管理员设置连锁采集是否消耗耐久。"), false);
            source.sendSuccess(() -> Component.literal("§e/fastmine vein hunger on|off§r：管理员设置连锁采集是否消耗饥饿值。"), false);
            source.sendSuccess(() -> Component.literal("§e/fastmine area durability on|off§r：管理员设置范围挖掘是否消耗耐久。"), false);
            source.sendSuccess(() -> Component.literal("§e/fastmine area hunger on|off§r：管理员设置范围挖掘是否消耗饥饿值。"), false);
        }
        return 1;
    }

    /**
     * 重载服务器全局配置和连锁采集规则。
     *
     * <p>此命令仅重读 config.json 与 veinmining 目录，玩家个人设置保持不变。</p>
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        FastMineMod.reloadServerConfiguration(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: server configuration and vein mining rules have been reloaded."
        ), true);
        return 1;
    }

    /**
     * 输出执行命令的玩家自己的 FastMine 设置。
     */
    private static int executeStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getRequiredPlayer(context);

        if (player == null) {
            return 0;
        }

        PlayerFastMineSettings settings = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal(formatStatus(settings)), false);
        return 1;
    }

    /**
     * 修改执行命令的玩家自己的连锁采集开关。
     */
    private static int executeSetVein(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = getRequiredPlayer(context);

        if (player == null) {
            return 0;
        }

        PlayerFastMineSettings currentSettings = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        PlayerFastMineSettings updatedSettings = currentSettings.withVeinEnabled(enabled);
        FastMineMod.getPlayerSettingsService().update(player.getUUID(), updatedSettings);

        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: vein mining is now %s.".formatted(enabled ? "ON" : "OFF")
        ), false);
        return 1;
    }

    /**
     * 修改执行命令的玩家自己的范围挖矿开关。
     */
    private static int executeSetArea(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = getRequiredPlayer(context);

        if (player == null) {
            return 0;
        }

        PlayerFastMineSettings currentSettings = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        PlayerFastMineSettings updatedSettings = currentSettings.withAreaEnabled(enabled);
        FastMineMod.getPlayerSettingsService().update(player.getUUID(), updatedSettings);

        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: area mining is now %s.".formatted(enabled ? "ON" : "OFF")
        ), false);
        return 1;
    }

    /** 修改执行者自己的掉落物聚合开关。 */
    private static int executeSetAggregateDrops(CommandContext<CommandSourceStack> context, boolean enabled)
            throws CommandSyntaxException {
        ServerPlayer player = getRequiredPlayer(context);
        if (player == null) return 0;

        PlayerFastMineSettings current = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        FastMineMod.getPlayerSettingsService().update(player.getUUID(), new PlayerFastMineSettings(
                current.veinEnabled(), current.areaEnabled(), current.areaWidth(), current.areaHeight(), current.areaDepth(),
                enabled, current.directExperience()
        ));
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: drop aggregation at your feet is now %s.".formatted(enabled ? "ON" : "OFF")
        ), false);
        return 1;
    }

    /** 修改执行者自己的额外经验直接获得开关。 */
    private static int executeSetDirectExperience(CommandContext<CommandSourceStack> context, boolean enabled)
            throws CommandSyntaxException {
        ServerPlayer player = getRequiredPlayer(context);
        if (player == null) return 0;

        PlayerFastMineSettings current = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        FastMineMod.getPlayerSettingsService().update(player.getUUID(), new PlayerFastMineSettings(
                current.veinEnabled(), current.areaEnabled(), current.areaWidth(), current.areaHeight(), current.areaDepth(),
                current.aggregateDropsAtFeet(), enabled
        ));
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: direct experience is now %s.".formatted(enabled ? "ON" : "OFF")
        ), false);
        return 1;
    }

    /**
     * 输出服务器全局的范围挖矿蹲下触发状态。
     */
    private static int executeAreaSneakStatus(CommandContext<CommandSourceStack> context) {
        boolean mustSneak = FastMineMod.getConfigManager().getConfig().areaMustSneak;
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: area mining %s crouching."
                        .formatted(mustSneak ? "requires" : "does not require")
        ), false);
        return 1;
    }

    /**
     * 修改服务器全局的范围挖矿蹲下触发规则，并立即持久化到 config.json。
     */
    private static int executeSetAreaMustSneak(CommandContext<CommandSourceStack> context, boolean mustSneak) {
        FastMineMod.getConfigManager().getConfig().areaMustSneak = mustSneak;
        FastMineMod.getConfigManager().save();
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: area mining %s crouching."
                        .formatted(mustSneak ? "now requires" : "no longer requires")
        ), true);
        return 1;
    }

    private static int executeSetVeinDurability(CommandContext<CommandSourceStack> context, boolean enabled) {
        return setAdminFlag(context, enabled, "veinMiningConsumesDurability", "vein durability");
    }

    private static int executeSetVeinHunger(CommandContext<CommandSourceStack> context, boolean enabled) {
        return setAdminFlag(context, enabled, "veinMiningConsumesHunger", "vein hunger");
    }

    private static int executeSetAreaDurability(CommandContext<CommandSourceStack> context, boolean enabled) {
        return setAdminFlag(context, enabled, "areaMiningConsumesDurability", "area durability");
    }

    private static int executeSetAreaHunger(CommandContext<CommandSourceStack> context, boolean enabled) {
        return setAdminFlag(context, enabled, "areaMiningConsumesHunger", "area hunger");
    }

    private static int executeSetGlobalExperience(CommandContext<CommandSourceStack> context, boolean enabled) {
        return setAdminFlag(context, enabled, "directExperience", "global direct experience");
    }

    private static int setAdminFlag(CommandContext<CommandSourceStack> context, boolean enabled, String flag, String label) {
        FastMineConfig config = FastMineMod.getConfigManager().getConfig();
        switch (flag) {
            case "veinMiningConsumesDurability" -> config.veinMiningConsumesDurability = enabled;
            case "veinMiningConsumesHunger" -> config.veinMiningConsumesHunger = enabled;
            case "areaMiningConsumesDurability" -> config.areaMiningConsumesDurability = enabled;
            case "areaMiningConsumesHunger" -> config.areaMiningConsumesHunger = enabled;
            case "directExperience" -> config.directExperience = enabled;
            default -> throw new IllegalArgumentException("Unknown FastMine admin flag: " + flag);
        }
        FastMineMod.getConfigManager().save();
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: %s is now %s.".formatted(label, enabled ? "ON" : "OFF")), true);
        return 1;
    }

    /**
     * 输出服务器全局的连锁采集蹲下触发状态。
     */
    private static int executeVeinSneakStatus(CommandContext<CommandSourceStack> context) {
        boolean mustSneak = FastMineMod.getVeinMiningConfigManager().getSettings().mustSneak;
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: vein mining %s crouching."
                        .formatted(mustSneak ? "requires" : "does not require")
        ), false);
        return 1;
    }

    /**
     * 修改服务器全局的连锁采集蹲下触发规则，并立即持久化到 settings.json。
     */
    private static int executeSetVeinMustSneak(CommandContext<CommandSourceStack> context, boolean mustSneak) {
        FastMineMod.getVeinMiningConfigManager().getSettings().mustSneak = mustSneak;
        FastMineMod.getVeinMiningConfigManager().save();
        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: vein mining %s crouching."
                        .formatted(mustSneak ? "now requires" : "no longer requires")
        ), true);
        return 1;
    }

    /**
     * 修改执行命令的玩家自己的范围挖矿尺寸。
     *
     * <p>宽度和高度必须为奇数，保证锚点位于平面中心；深度从锚点单向延伸，可为偶数。</p>
     */
    private static int executeSetAreaSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getRequiredPlayer(context);

        if (player == null) {
            return 0;
        }

        int width = IntegerArgumentType.getInteger(context, "width");
        int height = IntegerArgumentType.getInteger(context, "height");
        int depth = IntegerArgumentType.getInteger(context, "depth");
        FastMineConfig config = FastMineMod.getConfigManager().getConfig();

        if (!isValidOddAreaSize(width, config.minAreaWidth, config.maxAreaWidth)
                || !isValidOddAreaSize(height, config.minAreaHeight, config.maxAreaHeight)
                || !isValidDepth(depth, config.minAreaDepth, config.maxAreaDepth)) {
            context.getSource().sendFailure(Component.literal(
                    "FastMine: width/height must be odd and size must be within %d-%d x %d-%d x %d-%d."
                            .formatted(config.minAreaWidth, config.maxAreaWidth, config.minAreaHeight, config.maxAreaHeight,
                                    config.minAreaDepth, config.maxAreaDepth)
            ));
            return 0;
        }

        PlayerFastMineSettings currentSettings = FastMineMod.getPlayerSettingsService().getOrCreate(player.getUUID());
        PlayerFastMineSettings updatedSettings = currentSettings.withAreaSize(width, height, depth);
        FastMineMod.getPlayerSettingsService().update(player.getUUID(), updatedSettings);

        context.getSource().sendSuccess(() -> Component.literal(
                "FastMine: area size is now %dx%dx%d.".formatted(width, height, depth)
        ), false);
        return 1;
    }

    /**
     * 判断单个范围尺寸是否在服务器许可范围内。
     */
    private static boolean isValidOddAreaSize(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum && value % 2 != 0;
    }

    private static boolean isValidDepth(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    /**
     * 从命令来源获取玩家；控制台和命令方块不允许修改个人设置。
     */
    private static ServerPlayer getRequiredPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This FastMine command must be run by a player."));
            return null;
        }

        return source.getPlayerOrException();
    }

    /**
     * 将玩家设置格式化为状态文本。
     */
    private static String formatStatus(PlayerFastMineSettings settings) {
        return "FastMine status: vein=%s, area=%s, area size=%dx%dx%d, drops=%s, experience=%s."
                .formatted(
                        settings.veinEnabled() ? "ON" : "OFF",
                        settings.areaEnabled() ? "ON" : "OFF",
                        settings.areaWidth(),
                        settings.areaHeight(),
                        settings.areaDepth(),
                        settings.aggregateDropsAtFeet() ? "ON" : "OFF",
                        settings.directExperience() ? "ON" : "OFF"
                );
    }
}
