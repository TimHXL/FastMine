package com.timhxl.fastmine.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.timhxl.fastmine.FastMineMod;
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
                        .then(Commands.literal("vein")
                                .then(Commands.literal("on").executes(context -> executeSetVein(context, true)))
                                .then(Commands.literal("off").executes(context -> executeSetVein(context, false))))
                        .then(Commands.literal("area")
                                .then(Commands.literal("on").executes(context -> executeSetArea(context, true)))
                                .then(Commands.literal("off").executes(context -> executeSetArea(context, false))))
        ));
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