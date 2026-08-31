package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineSettingsSyncPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Java 玩家个人 FastMine 设置界面。
 */
public final class FastMineSettingsScreen extends Screen {
    private Button veinButton;
    private Button areaButton;
    private Button aggregateDropsButton;
    private Button directExperienceButton;
    private Button widthButton;
    private Button heightButton;
    private Button depthButton;
    private Button veinSneakButton;
    private Button areaSneakButton;
    private Button adminButton;
    private Button maxChainSaveButton;
    private EditBox maxChainBox;
    private long displayedRevision = Long.MIN_VALUE;

    public FastMineSettingsScreen() {
        super(Component.translatable("screen.fastmine.title"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        int top = Math.max(24, height / 2 - 82);

        veinButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleVein())
                .bounds(left, top, 200, 20)
                .build());
        areaButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleArea())
                .bounds(left, top + 28, 200, 20)
                .build());
        aggregateDropsButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleAggregateDrops())
                .bounds(left, top + 56, 98, 20)
                .build());
        directExperienceButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleDirectExperience())
                .bounds(left + 102, top + 56, 98, 20)
                .build());
        widthButton = addRenderableWidget(Button.builder(Component.empty(), button -> changeWidth())
                .bounds(left, top + 116, 64, 20)
                .build());
        heightButton = addRenderableWidget(Button.builder(Component.empty(), button -> changeHeight())
                .bounds(left + 68, top + 116, 64, 20)
                .build());
        depthButton = addRenderableWidget(Button.builder(Component.empty(), button -> changeDepth())
                .bounds(left + 136, top + 116, 64, 20)
                .build());
        veinSneakButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleVeinSneak())
                .bounds(left, top + 76, 200, 20)
                .build());
        areaSneakButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleAreaSneak())
                .bounds(left, top + 100, 200, 20)
                .build());
        maxChainBox = addRenderableWidget(new EditBox(font, left + 94, top + 126, 48, 20,
                Component.translatable("screen.fastmine.admin.max_chain")));
        maxChainBox.setMaxLength(5);
        maxChainSaveButton = addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.save"), button -> saveMaxChain())
                .bounds(left + 146, top + 126, 54, 20).build());
        adminButton = addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.open"),
                        button -> openAdminScreen())
                .bounds(left, top + 148, 200, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 50, height - 28, 100, 20)
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
        int top = Math.max(24, height / 2 - 82);
        graphics.centeredText(font, title, width / 2, top - 22, 0xFFFFFFFF);

        if (FastMineClientSettings.getSnapshot() == null) {
            graphics.centeredText(font, Component.translatable("screen.fastmine.loading"), width / 2, top + 46,
                    0xFFAAAAAA);
        } else {
            graphics.centeredText(font, Component.translatable("screen.fastmine.area_size"), width / 2, top + 102,
                    0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        FastMineListEditHistory.clear();
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (FastMineListEditHistory.handleUndoKey(event)) return true;
        return super.keyPressed(event);
    }

    private void refreshWidgets() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        displayedRevision = FastMineClientSettings.getRevision();
        boolean available = settings != null;
        boolean canManageServerSettings = available && settings.canManageServerSettings();

        veinButton.active = available;
        areaButton.active = available;
        aggregateDropsButton.active = available;
        directExperienceButton.active = available;
        widthButton.active = available;
        heightButton.active = available;
        depthButton.active = available;
        veinSneakButton.visible = false;
        areaSneakButton.visible = false;
        veinSneakButton.active = false;
        areaSneakButton.active = false;
        adminButton.visible = canManageServerSettings;
        adminButton.active = canManageServerSettings;
        maxChainBox.visible = false;
        maxChainBox.active = false;
        maxChainSaveButton.visible = false;
        maxChainSaveButton.active = false;

        if (!available) {
            veinButton.setMessage(Component.translatable("screen.fastmine.waiting"));
            areaButton.setMessage(Component.empty());
            aggregateDropsButton.setMessage(Component.empty());
            directExperienceButton.setMessage(Component.empty());
            widthButton.setMessage(Component.empty());
            heightButton.setMessage(Component.empty());
            depthButton.setMessage(Component.empty());
            veinSneakButton.setMessage(Component.empty());
            areaSneakButton.setMessage(Component.empty());
            adminButton.setMessage(Component.empty());
            maxChainBox.setValue("");
            return;
        }

        veinButton.setMessage(Component.translatable("screen.fastmine.vein", state(settings.veinEnabled())));
        areaButton.setMessage(Component.translatable("screen.fastmine.area", state(settings.areaEnabled())));
        aggregateDropsButton.setMessage(Component.translatable("screen.fastmine.aggregate_drops",
                shortState(settings.aggregateDropsAtFeet())));
        directExperienceButton.setMessage(Component.translatable("screen.fastmine.direct_experience",
                shortState(settings.directExperience())));
        widthButton.setMessage(Component.translatable("screen.fastmine.width", settings.areaWidth()));
        heightButton.setMessage(Component.translatable("screen.fastmine.height", settings.areaHeight()));
        depthButton.setMessage(Component.translatable("screen.fastmine.depth", settings.areaDepth()));
        veinSneakButton.setMessage(Component.translatable("screen.fastmine.vein_sneak", state(settings.veinMustSneak())));
        areaSneakButton.setMessage(Component.translatable("screen.fastmine.area_sneak", state(settings.areaMustSneak())));
        if (!maxChainBox.isFocused()) maxChainBox.setValue(String.valueOf(settings.maxChain()));
    }

    private Component state(boolean enabled) {
        return Component.translatable(enabled ? "screen.fastmine.on" : "screen.fastmine.off");
    }

    private Component shortState(boolean enabled) {
        return Component.translatable(enabled ? "screen.fastmine.short_on" : "screen.fastmine.short_off");
    }

    private void toggleVein() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(!settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    settings.areaDepth(), settings.aggregateDropsAtFeet(), settings.directExperience());
        }
    }

    private void toggleArea() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), !settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    settings.areaDepth(), settings.aggregateDropsAtFeet(), settings.directExperience());
        }
    }

    private void changeWidth() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), nextOdd(settings.areaWidth(), settings.minAreaWidth(), settings.maxAreaWidth()),
                    settings.areaHeight(), settings.areaDepth(), settings.aggregateDropsAtFeet(), settings.directExperience());
        }
    }

    private void changeHeight() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(),
                    nextOdd(settings.areaHeight(), settings.minAreaHeight(), settings.maxAreaHeight()), settings.areaDepth(),
                    settings.aggregateDropsAtFeet(), settings.directExperience());
        }
    }

    private void changeDepth() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    nextValue(settings.areaDepth(), settings.minAreaDepth(), settings.maxAreaDepth()),
                    settings.aggregateDropsAtFeet(), settings.directExperience());
        }
    }

    private void toggleAggregateDrops() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    settings.areaDepth(), !settings.aggregateDropsAtFeet(), settings.directExperience());
        }
    }

    private void toggleDirectExperience() {
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (settings != null) {
            submit(settings.veinEnabled(), settings.areaEnabled(), settings.areaWidth(), settings.areaHeight(),
                    settings.areaDepth(), settings.aggregateDropsAtFeet(), !settings.directExperience());
        }
    }

    private void submit(boolean veinEnabled, boolean areaEnabled, int areaWidth, int areaHeight, int areaDepth,
                        boolean aggregateDropsAtFeet, boolean directExperience) {
        FastMineClientNetworking.updateSettings(veinEnabled, areaEnabled, areaWidth, areaHeight, areaDepth,
                aggregateDropsAtFeet, directExperience);
    }

    private void toggleVeinSneak() {
        openAdminScreen();
    }

    private void toggleAreaSneak() {
        openAdminScreen();
    }

    private void openAdminScreen() {
        FastMineClientNetworking.requestAdminConfig();
        minecraft.gui.setScreen(new FastMineGlobalConfigScreen(this));
    }

    private void saveMaxChain() {
        openAdminScreen();
    }

    private int parseMaxChain() {
        try {
            return Integer.parseInt(maxChainBox.getValue());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static int nextOdd(int current, int minimum, int maximum) {
        int next = current + 2;
        return next <= maximum ? next : minimum;
    }

    private static int nextValue(int current, int minimum, int maximum) {
        int next = current + 1;
        return next <= maximum ? next : minimum;
    }
}
