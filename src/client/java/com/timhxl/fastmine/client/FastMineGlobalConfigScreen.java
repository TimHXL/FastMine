package com.timhxl.fastmine.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** OP 服务器全局配置的导航页面，避免将全部规则挤在同一页。 */
public final class FastMineGlobalConfigScreen extends Screen {
    private final Screen parent;

    public FastMineGlobalConfigScreen(Screen parent) {
        super(Component.literal("FastMine 服务器配置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        addRenderableWidget(Button.builder(Component.literal("范围挖掘尺寸规则"), button -> minecraft.gui.setScreen(new FastMineAreaRuleConfigScreen(this)))
                .bounds(left, 48, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("采集与保护规则"), button -> minecraft.gui.setScreen(new FastMineMiningRuleConfigScreen(this)))
                .bounds(left + 154, 48, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("管理连锁采集组"), button -> minecraft.gui.setScreen(new FastMineAdminScreen(this)))
                .bounds(left, 76, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("天然石材列表"), button -> minecraft.gui.setScreen(new FastMineListConfigScreen(this, false)))
                .bounds(left + 154, 76, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("受保护 Structure 列表"), button -> minecraft.gui.setScreen(new FastMineListConfigScreen(this, true)))
                .bounds(left, 104, 299, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        FastMineClientNetworking.requestAdminConfig();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (FastMineListEditHistory.handleUndoKey(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 18, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("列表修改可在任意 FastMine 页面按 Ctrl + Z 撤回。"), width / 2, 138, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
