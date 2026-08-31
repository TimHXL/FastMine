package com.timhxl.fastmine.client;

import com.timhxl.fastmine.network.FastMineAdminConfigSyncPayload;
import com.timhxl.fastmine.network.FastMineAdminConfigUpdatePayload;
import com.timhxl.fastmine.network.FastMineAdminGroupSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** OP 管理服务器 groups.json 的连锁采集组界面。 */
public final class FastMineAdminScreen extends Screen {
    private static final int ICON_SIZE = 16;
    private static final int ICON_STEP = 18;
    private static final int ICON_COLUMNS = 8;
    private static final int MAX_VISIBLE_ICONS = 32;

    private final Screen parent;
    private Button previousGroupButton;
    private Button nextGroupButton;
    private Button deleteGroupButton;
    private EditBox newGroupName;
    private EditBox entryIdentifier;
    private int selectedGroupIndex;
    private boolean selectNewGroupAfterSync;
    private long displayedRevision = Long.MIN_VALUE;

    public FastMineAdminScreen(Screen parent) {
        super(Component.translatable("screen.fastmine.admin.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        previousGroupButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> selectPreviousGroup())
                .bounds(center - 150, 36, 20, 20).build());
        nextGroupButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> selectNextGroup())
                .bounds(center + 130, 36, 20, 20).build());
        newGroupName = addRenderableWidget(new EditBox(font, center - 150, 60, 112, 20,
                Component.translatable("screen.fastmine.admin.new_group_name")));
        newGroupName.setMaxLength(64);
        newGroupName.setHint(Component.translatable("screen.fastmine.admin.new_group_name"));
        addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.create_group"), button -> createGroup())
                .bounds(center - 34, 60, 84, 20).build());
        deleteGroupButton = addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.delete_group"),
                        button -> deleteGroup())
                .bounds(center + 54, 60, 96, 20).build());
        entryIdentifier = addRenderableWidget(new EditBox(font, center - 150, 86, 142, 20, Component.literal("namespace:id")));
        entryIdentifier.setMaxLength(128);
        entryIdentifier.setHint(Component.literal("namespace:id"));
        addRenderableWidget(Button.builder(Component.literal("添加方块"), button -> addTypedEntry(true))
                .bounds(center - 4, 86, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("添加工具"), button -> addTypedEntry(false))
                .bounds(center + 72, 86, 78, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.pick_block"),
                        button -> openPicker(true))
                .bounds(center - 150, 110, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.pick_item"),
                        button -> openPicker(false))
                .bounds(center + 8, 110, 142, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.add_target_block"),
                        button -> addTargetBlock())
                .bounds(center - 150, 134, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fastmine.admin.add_held_tool"),
                        button -> addHeldTool())
                .bounds(center + 8, 134, 142, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> minecraft.gui.setScreen(parent))
                .bounds(center - 50, height - 28, 100, 20).build());
        refreshWidgets();
    }

    @Override
    public void tick() {
        if (displayedRevision != FastMineClientAdminConfig.getRevision()) {
            refreshWidgets();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        FastMineAdminConfigSyncPayload snapshot = FastMineClientAdminConfig.getSnapshot();
        if (snapshot == null) {
            graphics.centeredText(font, Component.translatable("screen.fastmine.admin.loading"), width / 2, 146, 0xFFAAAAAA);
            return;
        }
        if (snapshot.groups().isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.fastmine.admin.empty"), width / 2, 146, 0xFFAAAAAA);
            return;
        }
        selectedGroupIndex = Math.min(selectedGroupIndex, snapshot.groups().size() - 1);
        FastMineAdminGroupSnapshot group = snapshot.groups().get(selectedGroupIndex);
        graphics.centeredText(font, Component.literal(group.name()), width / 2, 41, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("screen.fastmine.admin.blocks", group.blocks().size()), width / 2 - 150, 162, 0xFF55FF55);
        drawIcons(graphics, group.blocks(), width / 2 - 150, 178, true, mouseX, mouseY);
        graphics.text(font, Component.translatable("screen.fastmine.admin.tools", group.tools().size()), width / 2 + 8, 162, 0xFF55AAFF);
        drawIcons(graphics, group.tools(), width / 2 + 8, 178, false, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 1 && removeEntryAt(click.x(), click.y())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (FastMineListEditHistory.handleUndoKey(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private void refreshWidgets() {
        FastMineAdminConfigSyncPayload snapshot = FastMineClientAdminConfig.getSnapshot();
        displayedRevision = FastMineClientAdminConfig.getRevision();
        int groupCount = snapshot == null ? 0 : snapshot.groups().size();
        if (selectNewGroupAfterSync && groupCount > 0) {
            selectedGroupIndex = groupCount - 1;
            selectNewGroupAfterSync = false;
        } else {
            selectedGroupIndex = groupCount == 0 ? 0 : Math.min(selectedGroupIndex, groupCount - 1);
        }
        previousGroupButton.active = groupCount > 1;
        nextGroupButton.active = groupCount > 1;
        deleteGroupButton.active = groupCount > 1;
    }

    private void selectPreviousGroup() {
        int groupCount = getGroupCount();
        if (groupCount > 0) {
            selectedGroupIndex = (selectedGroupIndex - 1 + groupCount) % groupCount;
        }
    }

    private void selectNextGroup() {
        int groupCount = getGroupCount();
        if (groupCount > 0) {
            selectedGroupIndex = (selectedGroupIndex + 1) % groupCount;
        }
    }

    private int getGroupCount() {
        FastMineAdminConfigSyncPayload snapshot = FastMineClientAdminConfig.getSnapshot();
        return snapshot == null ? 0 : snapshot.groups().size();
    }

    private void createGroup() {
        String name = newGroupName.getValue().strip();
        if (name.isEmpty()) {
            return;
        }
        selectNewGroupAfterSync = true;
        FastMineClientNetworking.updateAdminConfig(FastMineAdminConfigUpdatePayload.Operation.CREATE_GROUP, -1, name);
        newGroupName.setValue("");
    }

    private void deleteGroup() {
        FastMineClientNetworking.updateAdminConfig(FastMineAdminConfigUpdatePayload.Operation.DELETE_GROUP, selectedGroupIndex, "");
    }

    /** 打开完整注册表选择器，选中后仍由服务器验证并写入配置。 */
    private void openPicker(boolean blocks) {
        minecraft.gui.setScreen(new FastMineRegistryPickerScreen(this, blocks, identifier -> addEntry(blocks, identifier)));
    }

    /** 将输入框内的 namespace:id 添加到当前连锁采集组，最终合法性由服务器验证。 */
    private void addTypedEntry(boolean blocks) {
        String identifier = entryIdentifier.getValue().strip();
        if (identifier.isEmpty()) return;
        addEntry(blocks, identifier);
        entryIdentifier.setValue("");
    }

    /** 请求服务端将该管理员实际准星指向的方块加入当前组。 */
    private void addTargetBlock() {
        FastMineClientNetworking.updateAdminConfig(
                FastMineAdminConfigUpdatePayload.Operation.ADD_TARGET_BLOCK, selectedGroupIndex, "");
    }

    /** 请求服务端将该管理员实际主手持有的工具加入当前组。 */
    private void addHeldTool() {
        FastMineClientNetworking.updateAdminConfig(
                FastMineAdminConfigUpdatePayload.Operation.ADD_HELD_TOOL, selectedGroupIndex, "");
    }

    /** 通过选择器新增条目，并记录可撤回的反向操作。 */
    private void addEntry(boolean blocks, String identifier) {
        FastMineClientNetworking.updateAdminConfig(blocks
                        ? FastMineAdminConfigUpdatePayload.Operation.ADD_BLOCK
                        : FastMineAdminConfigUpdatePayload.Operation.ADD_TOOL,
                selectedGroupIndex, identifier);
        FastMineListEditHistory.recordInverse(blocks
                        ? FastMineAdminConfigUpdatePayload.Operation.REMOVE_BLOCK
                        : FastMineAdminConfigUpdatePayload.Operation.REMOVE_TOOL,
                selectedGroupIndex, identifier);
    }

    /** 右键图标从当前组删除对应配置项，并记录可撤回的反向操作。 */
    private boolean removeEntryAt(double mouseX, double mouseY) {
        FastMineAdminConfigSyncPayload snapshot = FastMineClientAdminConfig.getSnapshot();
        if (snapshot == null || snapshot.groups().isEmpty()) {
            return false;
        }
        FastMineAdminGroupSnapshot group = snapshot.groups().get(selectedGroupIndex);
        int center = width / 2;
        String blockEntry = getEntryAt(group.blocks(), center - 150, 178, mouseX, mouseY);
        if (blockEntry != null) {
            FastMineClientNetworking.updateAdminConfig(FastMineAdminConfigUpdatePayload.Operation.REMOVE_BLOCK,
                    selectedGroupIndex, blockEntry);
            FastMineListEditHistory.recordInverse(FastMineAdminConfigUpdatePayload.Operation.ADD_BLOCK,
                    selectedGroupIndex, blockEntry);
            return true;
        }
        String toolEntry = getEntryAt(group.tools(), center + 8, 178, mouseX, mouseY);
        if (toolEntry != null) {
            FastMineClientNetworking.updateAdminConfig(FastMineAdminConfigUpdatePayload.Operation.REMOVE_TOOL,
                    selectedGroupIndex, toolEntry);
            FastMineListEditHistory.recordInverse(FastMineAdminConfigUpdatePayload.Operation.ADD_TOOL,
                    selectedGroupIndex, toolEntry);
            return true;
        }
        return false;
    }

    private static String getEntryAt(List<String> entries, int x, int y, double mouseX, double mouseY) {
        int visibleCount = Math.min(entries.size(), MAX_VISIBLE_ICONS);
        for (int index = 0; index < visibleCount; index++) {
            int iconX = x + (index % ICON_COLUMNS) * ICON_STEP;
            int iconY = y + (index / ICON_COLUMNS) * ICON_STEP;
            if (mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= iconY && mouseY < iconY + ICON_SIZE) {
                return entries.get(index);
            }
        }
        return null;
    }

    /** 将服务器配置项显示为原版物品图标，完整标识符仅在鼠标悬停时显示。 */
    private void drawIcons(GuiGraphicsExtractor graphics, List<String> entries, int x, int y, boolean blocks, int mouseX, int mouseY) {
        int visibleCount = Math.min(entries.size(), MAX_VISIBLE_ICONS);
        for (int index = 0; index < visibleCount; index++) {
            int iconX = x + (index % ICON_COLUMNS) * ICON_STEP;
            int iconY = y + (index / ICON_COLUMNS) * ICON_STEP;
            String entry = entries.get(index);
            ItemStack icon = resolveIcon(entry, blocks);
            boolean hovered = mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= iconY && mouseY < iconY + ICON_SIZE;
            if (hovered) {
                graphics.fill(iconX - 1, iconY - 1, iconX + ICON_SIZE + 1, iconY + ICON_SIZE + 1, 0x80FFFFFF);
            }
            graphics.item(icon, iconX, iconY);
            if (hovered) {
                graphics.setComponentTooltipForNextFrame(font, List.of(icon.getHoverName(),
                        Component.literal(entry).withStyle(ChatFormatting.DARK_GRAY),
                        Component.literal("右键点击删除；Ctrl + Z 撤回").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            }
        }
        if (entries.size() > MAX_VISIBLE_ICONS) {
            graphics.text(font, Component.literal("+" + (entries.size() - MAX_VISIBLE_ICONS)), x, y + 4 * ICON_STEP, 0xFFAAAAAA);
        }
    }

    private static ItemStack resolveIcon(String entry, boolean blocks) {
        if (entry == null || entry.isBlank() || entry.startsWith("#")) {
            return new ItemStack(Items.KNOWLEDGE_BOOK);
        }
        Identifier identifier = Identifier.tryParse(entry);
        if (identifier == null) {
            return new ItemStack(Items.BARRIER);
        }
        if (blocks && BuiltInRegistries.BLOCK.containsKey(identifier)) {
            return new ItemStack(BuiltInRegistries.BLOCK.getValue(identifier).asItem());
        }
        if (!blocks && BuiltInRegistries.ITEM.containsKey(identifier)) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(identifier));
        }
        return new ItemStack(Items.BARRIER);
    }
}
