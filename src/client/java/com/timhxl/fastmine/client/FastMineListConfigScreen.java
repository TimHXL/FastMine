package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineAdminConfigSyncPayload;
import com.timhxl.fastmine.network.FastMineAdminConfigUpdatePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** OP 编辑天然石材或受保护 Structure 列表的独立页面。 */
public final class FastMineListConfigScreen extends Screen {
    private final Screen parent;
    private final boolean structures;
    private EditBox entryBox;

    public FastMineListConfigScreen(Screen parent, boolean structures) {
        super(Component.literal(structures ? "受保护 Structure 列表" : "天然石材列表"));
        this.parent = parent;
        this.structures = structures;
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        entryBox = addRenderableWidget(new EditBox(font, left, 46, 190, 20,
                Component.literal(structures ? "minecraft:stronghold" : "minecraft:stone")));
        entryBox.setMaxLength(128);
        addRenderableWidget(Button.builder(Component.literal("添加"), button -> addEntry()).bounds(left + 198, 46, 101, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> minecraft.gui.setScreen(parent))
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        FastMineClientNetworking.requestAdminConfig();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 150;
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.text(font, Component.literal("输入 namespace:id；右键已有条目删除，Ctrl + Z 可撤回。"), left, 30, 0xFFAAAAAA);
        FastMineAdminConfigSyncPayload snapshot = FastMineClientAdminConfig.getSnapshot();
        if (snapshot == null) return;
        List<String> entries = structures ? snapshot.global().protectedStructures() : snapshot.global().naturalStoneBlocks();
        for (int index = 0; index < visibleEntryCount(entries.size()); index++) {
            int y = 82 + index * 16;
            boolean hovered = mouseX >= left && mouseX < left + 300 && mouseY >= y && mouseY < y + 12;
            graphics.text(font, Component.literal(entries.get(index)), left, y, hovered ? 0xFFFFFFFF : (structures ? 0xFFFFAA55 : 0xFF55FF55));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 1 && removeEntry(click.x(), click.y())) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (FastMineListEditHistory.handleUndoKey(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }

    private void addEntry() {
        String value = entryBox.getValue().strip();
        if (value.isEmpty()) return;
        FastMineClientNetworking.updateAdminConfig(structures
                ? FastMineAdminConfigUpdatePayload.Operation.ADD_PROTECTED_STRUCTURE
                : FastMineAdminConfigUpdatePayload.Operation.ADD_NATURAL_STONE, -1, value);
        FastMineListEditHistory.recordInverse(structures
                ? FastMineAdminConfigUpdatePayload.Operation.REMOVE_PROTECTED_STRUCTURE
                : FastMineAdminConfigUpdatePayload.Operation.REMOVE_NATURAL_STONE, -1, value);
        entryBox.setValue("");
    }

    private boolean removeEntry(double mouseX, double mouseY) {
        FastMineAdminConfigSyncPayload snapshot = FastMineClientAdminConfig.getSnapshot();
        if (snapshot == null || mouseX < width / 2 - 150 || mouseX >= width / 2 + 150) return false;
        List<String> entries = structures ? snapshot.global().protectedStructures() : snapshot.global().naturalStoneBlocks();
        int index = (int) ((mouseY - 82) / 16);
        if (index < 0 || index >= visibleEntryCount(entries.size())) return false;
        String value = entries.get(index);
        FastMineClientNetworking.updateAdminConfig(structures
                ? FastMineAdminConfigUpdatePayload.Operation.REMOVE_PROTECTED_STRUCTURE
                : FastMineAdminConfigUpdatePayload.Operation.REMOVE_NATURAL_STONE, -1, value);
        FastMineListEditHistory.recordInverse(structures
                ? FastMineAdminConfigUpdatePayload.Operation.ADD_PROTECTED_STRUCTURE
                : FastMineAdminConfigUpdatePayload.Operation.ADD_NATURAL_STONE, -1, value);
        return true;
    }

    /** 保留底部返回按钮的空间，避免低 GUI 分辨率时条目与按钮重叠。 */
    private int visibleEntryCount(int entryCount) {
        return Math.min(entryCount, Math.max(0, (height - 116) / 16));
    }
}
