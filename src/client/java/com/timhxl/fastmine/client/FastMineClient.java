package com.timhxl.fastmine.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

/**
 * FastMine Java 客户端入口。
 */
public final class FastMineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FastMineKeyBindings.register();
        FastMineClientNetworking.register();
        FastMinePreviewRenderer.initialize();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> FastMineClientNetworking.requestSettings());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FastMineClientSettings.clear();
            FastMineClientAdminConfig.clear();
            FastMineListEditHistory.clear();
            FastMinePreviewRenderer.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(FastMineClient::handleKeyBindings);
    }

    /**
     * 响应可重绑定的设置界面快捷键。
     */
    private static void handleKeyBindings(Minecraft client) {
        while (FastMineKeyBindings.openSettings.consumeClick()) {
            if (client.player == null) {
                return;
            }

            FastMineClientNetworking.requestSettings();
            client.gui.setScreen(new FastMineSettingsScreen());
        }
    }
}
