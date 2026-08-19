package com.timhxl.fastmine.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** 从客户端已注册方块或物品中选择一个条目的界面。 */
public final class FastMineRegistryPickerScreen extends Screen {
    private static final int COLUMNS = 12;
    private static final int ROWS = 7;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int STEP = 18;

    private final Screen parent;
    private final boolean blocks;
    private final Consumer<String> selectionConsumer;
    private final List<Entry> allEntries = new ArrayList<>();
    private List<Entry> filteredEntries = List.of();
    private EditBox searchBox;
    private Button previousPageButton;
    private Button nextPageButton;
    private String previousSearch = "\u0000";
    private int page;

    public FastMineRegistryPickerScreen(Screen parent, boolean blocks, Consumer<String> selectionConsumer) {
        super(Component.translatable(blocks ? "screen.fastmine.picker.blocks" : "screen.fastmine.picker.items"));
        this.parent = parent;
        this.blocks = blocks;
        this.selectionConsumer = selectionConsumer;
    }

    @Override
    protected void init() {
        buildEntries();
        int center = width / 2;
        searchBox = addRenderableWidget(new EditBox(font, center - 108, 32, 216, 20,
                Component.translatable("screen.fastmine.picker.search")));
        searchBox.setHint(Component.translatable("screen.fastmine.picker.search"));
        previousPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(center - 110, height - 28, 20, 20).build());
        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(center + 90, height - 28, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> minecraft.gui.setScreen(parent))
                .bounds(center - 50, height - 28, 100, 20).build());
        refreshFilter();
        setInitialFocus(searchBox);
    }

    @Override
    public void tick() {
        if (!searchBox.getValue().equals(previousSearch)) {
            refreshFilter();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        int start = page * PAGE_SIZE;
        int visible = Math.min(PAGE_SIZE, Math.max(0, filteredEntries.size() - start));
        int gridX = width / 2 - (COLUMNS * STEP) / 2;
        int gridY = 60;
        for (int index = 0; index < visible; index++) {
            Entry entry = filteredEntries.get(start + index);
            int x = gridX + (index % COLUMNS) * STEP;
            int y = gridY + (index / COLUMNS) * STEP;
            boolean hovered = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
            if (hovered) {
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x80FFFFFF);
            }
            graphics.item(entry.stack(), x, y);
            if (hovered) {
                graphics.setComponentTooltipForNextFrame(font, List.of(entry.stack().getHoverName(),
                        Component.literal(entry.identifier()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), mouseX, mouseY);
            }
        }
        int pageCount = Math.max(1, (filteredEntries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        graphics.centeredText(font, Component.translatable("screen.fastmine.picker.page", page + 1, pageCount),
                width / 2, height - 22, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            Entry entry = getEntryAt(click.x(), click.y());
            if (entry != null) {
                selectionConsumer.accept(entry.identifier());
                minecraft.gui.setScreen(parent);
                return true;
            }
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

    private void buildEntries() {
        allEntries.clear();
        if (blocks) {
            for (net.minecraft.world.level.block.Block block : BuiltInRegistries.BLOCK) {
                Item item = block.asItem();
                if (!item.equals(net.minecraft.world.item.Items.AIR)) {
                    allEntries.add(new Entry(BuiltInRegistries.BLOCK.getKey(block).toString(), new ItemStack(item)));
                }
            }
        } else {
            for (Item item : BuiltInRegistries.ITEM) {
                if (!item.equals(net.minecraft.world.item.Items.AIR)) {
                    allEntries.add(new Entry(BuiltInRegistries.ITEM.getKey(item).toString(), new ItemStack(item)));
                }
            }
        }
        allEntries.sort(Comparator.comparing(Entry::identifier));
    }

    private void refreshFilter() {
        previousSearch = searchBox.getValue();
        String query = previousSearch.strip().toLowerCase(Locale.ROOT);
        filteredEntries = allEntries.stream().filter(entry -> query.isEmpty()
                || entry.identifier().contains(query)
                || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
        page = 0;
        previousPageButton.active = false;
        nextPageButton.active = filteredEntries.size() > PAGE_SIZE;
    }

    private void changePage(int change) {
        int pageCount = Math.max(1, (filteredEntries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page + change, pageCount - 1));
        previousPageButton.active = page > 0;
        nextPageButton.active = page < pageCount - 1;
    }

    private Entry getEntryAt(double mouseX, double mouseY) {
        int gridX = width / 2 - (COLUMNS * STEP) / 2;
        int gridY = 60;
        int column = (int) ((mouseX - gridX) / STEP);
        int row = (int) ((mouseY - gridY) / STEP);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) {
            return null;
        }
        int index = page * PAGE_SIZE + row * COLUMNS + column;
        return index >= 0 && index < filteredEntries.size() ? filteredEntries.get(index) : null;
    }

    private record Entry(String identifier, ItemStack stack) {
    }
}
