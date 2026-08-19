package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineAdminConfigSyncPayload;
import com.timhxl.fastmine.network.FastMineAdminGlobalConfigSnapshot;
import com.timhxl.fastmine.network.FastMineSettingsSyncPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** OP 编辑范围挖掘最小、最大及默认尺寸规则。 */
public final class FastMineAreaRuleConfigScreen extends Screen {
    private final Screen parent;
    private EditBox minWidth;
    private EditBox minHeight;
    private EditBox minDepth;
    private EditBox maxWidth;
    private EditBox maxHeight;
    private EditBox maxDepth;
    private EditBox defaultWidth;
    private EditBox defaultHeight;
    private EditBox defaultDepth;
    private Button saveButton;
    private long displayedRevision = Long.MIN_VALUE;

    public FastMineAreaRuleConfigScreen(Screen parent) {
        super(Component.literal("范围挖掘尺寸规则"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        minWidth = integerBox(left, 64); minHeight = integerBox(left + 58, 64); minDepth = integerBox(left + 116, 64);
        maxWidth = integerBox(left, 106); maxHeight = integerBox(left + 58, 106); maxDepth = integerBox(left + 116, 106);
        defaultWidth = integerBox(left, 148); defaultHeight = integerBox(left + 58, 148); defaultDepth = integerBox(left + 116, 148);
        saveButton = addRenderableWidget(Button.builder(Component.literal("保存尺寸规则"), button -> save()).bounds(left, 188, 299, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(width / 2 - 50, height - 28, 100, 20).build());
        FastMineClientNetworking.requestAdminConfig();
        refreshWidgets();
    }

    @Override
    public void tick() {
        if (displayedRevision != FastMineClientAdminConfig.getRevision()) refreshWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (FastMineListEditHistory.handleUndoKey(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 150;
        graphics.centeredText(font, title, width / 2, 18, 0xFFFFFFFF);
        graphics.text(font, Component.literal("最小范围（宽 / 高 / 深）"), left, 48, 0xFFAAAAAA);
        graphics.text(font, Component.literal("最大范围（宽 / 高 / 深）"), left, 90, 0xFFAAAAAA);
        graphics.text(font, Component.literal("默认范围（宽 / 高 / 深）"), left, 132, 0xFFAAAAAA);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }

    private EditBox integerBox(int x, int y) {
        EditBox box = addRenderableWidget(new EditBox(font, x, y, 52, 20, Component.empty()));
        box.setMaxLength(2);
        return box;
    }

    private void refreshWidgets() {
        FastMineAdminConfigSyncPayload admin = FastMineClientAdminConfig.getSnapshot();
        displayedRevision = FastMineClientAdminConfig.getRevision();
        boolean available = admin != null;
        for (EditBox box : List.of(minWidth, minHeight, minDepth, maxWidth, maxHeight, maxDepth, defaultWidth, defaultHeight, defaultDepth)) box.active = available;
        saveButton.active = available;
        if (!available) return;
        FastMineAdminGlobalConfigSnapshot global = admin.global();
        setIfUnfocused(minWidth, global.minAreaWidth()); setIfUnfocused(minHeight, global.minAreaHeight()); setIfUnfocused(minDepth, global.minAreaDepth());
        setIfUnfocused(maxWidth, global.maxAreaWidth()); setIfUnfocused(maxHeight, global.maxAreaHeight()); setIfUnfocused(maxDepth, global.maxAreaDepth());
        setIfUnfocused(defaultWidth, global.defaultAreaWidth()); setIfUnfocused(defaultHeight, global.defaultAreaHeight()); setIfUnfocused(defaultDepth, global.defaultAreaDepth());
    }

    private void save() {
        FastMineAdminConfigSyncPayload admin = FastMineClientAdminConfig.getSnapshot();
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (admin == null || settings == null) return;
        FastMineAdminGlobalConfigSnapshot global = admin.global();
        try {
            FastMineClientNetworking.updateGlobalSettings(settings.veinMustSneak(), settings.areaMustSneak(), settings.maxChain(),
                    read(minWidth), read(minHeight), read(minDepth), read(maxWidth), read(maxHeight), read(maxDepth),
                    read(defaultWidth), read(defaultHeight), read(defaultDepth), global.verticalMiningEnabled(), global.verticalMiningDepth(), global.structureProtectionEnabled());
        } catch (NumberFormatException ignored) {
        }
    }

    private static int read(EditBox box) { return Integer.parseInt(box.getValue()); }
    private static void setIfUnfocused(EditBox box, int value) { if (!box.isFocused()) box.setValue(String.valueOf(value)); }
}
