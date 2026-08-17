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
                        .then(Commands.literal("area")
                                .then(Commands.literal("on").executes(context -> executeSetArea(context, true)))
                                .then(Commands.literal("off").executes(context -> executeSetArea(context, false)))
                                .then(Commands.literal("sneak")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .executes(FastMineCommands::executeAreaSneakStatus)
                                        .then(Commands.literal("on").executes(context -> executeSetAreaMustSneak(context, true)))
                                        .then(Commands.literal("off").executes(context -> executeSetAreaMustSneak(context, false))))
                                .then(Commands.literal("size")
                                        .then(Commands.argument("width", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("height", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("depth", IntegerArgumentType.integer(1))
                                                                .executes(FastMineCommands::executeSetAreaSize))))))
        ));
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
     * <p>尺寸必须为正奇数，才能保证原始被破坏的方块始终位于范围中心。</p>
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

        if (!isValidAreaSize(width, config.maxAreaWidth)
                || !isValidAreaSize(height, config.maxAreaHeight)
                || !isValidAreaSize(depth, config.maxAreaDepth)) {
            context.getSource().sendFailure(Component.literal(
                    "FastMine: area dimensions must be positive odd numbers and cannot exceed %dx%dx%d."
                            .formatted(config.maxAreaWidth, config.maxAreaHeight, config.maxAreaDepth)
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
    private static boolean isValidAreaSize(int value, int maximum) {
        return value > 0 && value <= maximum && value % 2 != 0;
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
        return "FastMine status: vein=%s, area=%s, area size=%dx%dx%d."
                .formatted(
                        settings.veinEnabled() ? "ON" : "OFF",
                        settings.areaEnabled() ? "ON" : "OFF",
                        settings.areaWidth(),
                        settings.areaHeight(),
                        settings.areaDepth()
                );
    }
}
