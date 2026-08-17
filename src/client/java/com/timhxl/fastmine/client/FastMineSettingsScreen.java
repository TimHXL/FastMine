package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineSettingsSyncPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Java 玩家个人 FastMine 设置界面。
 */
public final class FastMineSettingsScreen extends Screen {
    private Button veinButton;
    private Button areaButton;
    private Button widthButton;
    private Button heightButton;
    private Button depthButton;
    private Button veinSneakButton;
    private Button areaSneakButton;
    private long displayedRevision = Long.MIN_VALUE;

    public FastMineSettingsScreen() {
        super(Component.translatable("screen.fastmine.title"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        int top = height / 2 - 92;

        veinButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleVein())
                .bounds(left, top, 200, 20)
                .build());
        areaButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleArea())
                .bounds(left, top + 26, 200, 20)
                .build());
        widthButton = addRenderableWidget(Button.builder(Component.empty(), button -> changeWidth())
                .bounds(left, top + 62, 64, 20)
                .build());
        heightButton = addRenderableWidget(Button.builder(Component.empty(), button -> changeHeight())
                .bounds(left + 68, top + 62, 64, 20)
                .build());
        depthButton = addRenderableWidget(Button.builder(Component.empty(), button -> changeDepth())
                .bounds(left + 136, top + 62, 64, 20)
                .build());
        veinSneakButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleVeinSneak())
                .bounds(left, top + 106, 200, 20)
                .build());
        areaSneakButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleAreaSneak())
                .bounds(left, top + 132, 200, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 50, top + 164, 100, 20)
                .build());

        refreshWidgets();
    }

    @Override
    public void tick() {
        if (displayedRevision != FastMineClientSettings.getRevision()) {
            refreshWidgets();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, height / 2 - 119, 0xFFFFFF);

        if (FastMineClientSettings.getSnapshot() == null) {
            graphics.centeredText(font, Component.translatable("screen.fastmine.loading"), width / 2, height / 2 - 69,
                    0xAAAAAA);
        } else {
            graphics.centeredText(font, Component.translatable("screen.fastmine.area_size"), width / 2, height / 2 - 42,
                    0xAAAAAA);
            if (FastMineClientSettings.getSnapshot().canManageServerSettings()) {
                graphics.centeredText(font, Component.translatable("screen.fastmine.admin_settings"), width / 2,
                        height / 2 + 2, 0xAAAAAA);
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }

    private void refreshWidgets() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        displayedRevision = FastMineClientSettings.getRevision();
        boolean available = settings != null;
        boolean canManageServerSettings = available && settings.canManageServerSettings();

        veinButton.active = available;
        areaButton.active = available;
        widthButton.active = available;
        heightButton.active = available;
        depthButton.active = available;
        veinSneakButton.visible = canManageServerSettings;
        areaSneakButton.visible = canManageServerSettings;
        veinSneakButton.active = canManageServerSettings;
        areaSneakButton.active = canManageServerSettings;

        if (!available) {
            veinButton.setMessage(Component.translatable("screen.fastmine.waiting"));
            areaButton.setMessage(Component.empty());
            widthButton.setMessage(Component.empty());
            heightButton.setMessage(Component.empty());
            depthButton.setMessage(Component.empty());
            veinSneakButton.setMessage(Component.empty());
            areaSneakButton.setMessage(Component.empty());
            return;
        }

        veinButton.setMessage(Component.translatable("screen.fastmine.vein", state(settings.veinEnabled())));
        areaButton.setMessage(Component.translatable("screen.fastmine.area", state(settings.areaEnabled())));
        widthButton.setMessage(Component.translatable("screen.fastmine.width", settings.areaWidth()));
        heightButton.setMessage(Component.translatable("screen.fastmine.height", settings.areaHeight()));
        depthButton.setMessage(Component.translatable("screen.fastmine.depth", settings.areaDepth()));
        veinSneakButton.setMessage(Component.translatable("screen.fastmine.vein_sneak", state(settings.veinMustSneak())));
        areaSneakButton.setMessage(Component.translatable("screen.fastmine.area_sneak", state(settings.areaMustSneak())));
    }

    private Component state(boolean enabled) {
        return Component.translatable(enabled ? "screen.fastmine.on" : "screen.fastmine.off");
    }

    private void toggleVein() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(!settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    settings.areaDepth());
        }
    }

    private void toggleArea() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), !settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    settings.areaDepth());
        }
    }

    private void changeWidth() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), nextOdd(settings.areaWidth(), settings.maxAreaWidth()),
                    settings.areaHeight(), settings.areaDepth());
        }
    }

    private void changeHeight() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(),
                    nextOdd(settings.areaHeight(), settings.maxAreaHeight()), settings.areaDepth());
        }
    }

    private void changeDepth() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    nextOdd(settings.areaDepth(), settings.maxAreaDepth()));
        }
    }

    private void submit(boolean veinEnabled, boolean areaEnabled, int areaWidth, int areaHeight, int areaDepth) {
        FastMineClientNetworking.updateSettings(veinEnabled, areaEnabled, areaWidth, areaHeight, areaDepth);
    }

    private void toggleVeinSneak() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null && settings.canManageServerSettings()) {
            FastMineClientNetworking.updateGlobalSettings(!settings.veinMustSneak(), settings.areaMustSneak());
        }
    }

    private void toggleAreaSneak() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null && settings.canManageServerSettings()) {
            FastMineClientNetworking.updateGlobalSettings(settings.veinMustSneak(), !settings.areaMustSneak());
        }
    }

    private static int nextOdd(int current, int maximum) {
        int next = current + 2;
        return next <= maximum ? next : 1;
    }
}
