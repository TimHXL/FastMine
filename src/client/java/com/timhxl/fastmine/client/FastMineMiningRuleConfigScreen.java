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

/** OP 编辑连锁、垂直挖掘与结构保护规则。 */
public final class FastMineMiningRuleConfigScreen extends Screen {
    private final Screen parent;
    private EditBox verticalDepth;
    private EditBox maxChain;
    private Button verticalButton;
    private Button protectionButton;
    private Button dropTransferButton;
    private Button directExperienceButton;
    private Button veinDurabilityButton;
    private Button veinHungerButton;
    private Button areaDurabilityButton;
    private Button areaHungerButton;
    private Button veinSneakButton;
    private Button areaSneakButton;
    private long displayedRevision = Long.MIN_VALUE;
    private boolean refreshingWidgets;

    public FastMineMiningRuleConfigScreen(Screen parent) {
        super(Component.literal("采集与保护规则"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        // 这一页控件较多，使用紧凑且连续的纵向布局，避免底部开关与返回按钮重叠。
        verticalDepth = integerBox(left, 38);
        maxChain = integerBox(left + 174, 38);
        verticalButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(true, false, false, false, false, false, false, false, false, false)).bounds(left, 68, 145, 20).build());
        protectionButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, true, false, false, false, false, false, false, false, false)).bounds(left + 154, 68, 145, 20).build());
        veinSneakButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, true, false, false, false, false, false, false, false)).bounds(left, 94, 145, 20).build());
        areaSneakButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, false, true, false, false, false, false, false, false)).bounds(left + 154, 94, 145, 20).build());
        dropTransferButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, false, false, true, false, false, false, false, false)).bounds(left, 120, 145, 20).build());
        directExperienceButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, false, false, false, false, false, false, false, true)).bounds(left + 154, 120, 145, 20).build());
        veinDurabilityButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, false, false, false, true, false, false, false, false)).bounds(left, 146, 145, 20).build());
        veinHungerButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, false, false, false, false, true, false, false, false)).bounds(left + 154, 146, 145, 20).build());
        areaDurabilityButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, false, false, false, false, false, true, false, false)).bounds(left, 172, 145, 20).build());
        areaHungerButton = addRenderableWidget(Button.builder(Component.empty(), button -> save(false, false, false, false, false, false, false, false, true, false)).bounds(left + 154, 172, 145, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(width / 2 - 50, height - 28, 100, 20).build());
        FastMineClientNetworking.requestAdminConfig();
        refreshWidgets();
        verticalDepth.setResponder(value -> {
            if (!refreshingWidgets) save(false, false, false, false, false, false, false, false, false, false);
        });
        maxChain.setResponder(value -> {
            if (!refreshingWidgets) save(false, false, false, false, false, false, false, false, false, false);
        });
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
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.text(font, Component.literal("垂直深度"), left, 22, 0xFFAAAAAA);
        graphics.text(font, Component.literal("连锁最大数量"), left + 174, 22, 0xFFAAAAAA);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }

    private EditBox integerBox(int x, int y) {
        EditBox box = addRenderableWidget(new EditBox(font, x, y, 126, 20, Component.empty()));
        box.setMaxLength(5);
        return box;
    }

    private void refreshWidgets() {
        refreshingWidgets = true;
        try {
            FastMineAdminConfigSyncPayload admin = FastMineClientAdminConfig.getSnapshot();
            displayedRevision = FastMineClientAdminConfig.getRevision();
            boolean available = admin != null;
            verticalDepth.active = available; maxChain.active = available;
            verticalButton.active = available; protectionButton.active = available; veinSneakButton.active = available; areaSneakButton.active = available; dropTransferButton.active = available; directExperienceButton.active = available;
            veinDurabilityButton.active = available; veinHungerButton.active = available;
            areaDurabilityButton.active = available; areaHungerButton.active = available;
            if (!available) return;
            FastMineAdminGlobalConfigSnapshot global = admin.global();
            FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
            setIfUnfocused(verticalDepth, global.verticalMiningDepth());
            if (settings != null) setIfUnfocused(maxChain, settings.maxChain());
            verticalButton.setMessage(Component.literal("垂直深度规则：" + state(global.verticalMiningEnabled())));
            protectionButton.setMessage(Component.literal("结构保护：" + state(global.structureProtectionEnabled())));
            veinSneakButton.setMessage(Component.literal("连锁需要蹲下：" + state(settings != null && settings.veinMustSneak())));
            areaSneakButton.setMessage(Component.literal("范围需要蹲下：" + state(settings != null && settings.areaMustSneak())));
            dropTransferButton.setMessage(Component.literal("掉落物传入背包：" + state(global.transferExtraDropsToPlayer())));
            directExperienceButton.setMessage(Component.literal("直接获取经验：" + state(global.directExperience())));
            veinDurabilityButton.setMessage(Component.literal("连锁消耗耐久：" + state(global.veinMiningConsumesDurability())));
            veinHungerButton.setMessage(Component.literal("连锁消耗饥饿：" + state(global.veinMiningConsumesHunger())));
            areaDurabilityButton.setMessage(Component.literal("范围消耗耐久：" + state(global.areaMiningConsumesDurability())));
            areaHungerButton.setMessage(Component.literal("范围消耗饥饿：" + state(global.areaMiningConsumesHunger())));
        } finally {
            refreshingWidgets = false;
        }
    }

    private void save(boolean toggleVertical, boolean toggleProtection, boolean toggleVeinSneak, boolean toggleAreaSneak,
                      boolean toggleDropTransfer, boolean toggleVeinDurability, boolean toggleVeinHunger,
                      boolean toggleAreaDurability, boolean toggleAreaHunger, boolean toggleDirectExperience) {
        FastMineAdminConfigSyncPayload admin = FastMineClientAdminConfig.getSnapshot();
        FastMineSettingsSyncPayload settings = FastMineClientSettings.getSnapshot();
        if (admin == null || settings == null) return;
        FastMineAdminGlobalConfigSnapshot global = admin.global();
        try {
            FastMineClientNetworking.updateGlobalSettings(toggleVeinSneak ? !settings.veinMustSneak() : settings.veinMustSneak(),
                    toggleAreaSneak ? !settings.areaMustSneak() : settings.areaMustSneak(), read(maxChain),
                    global.minAreaWidth(), global.minAreaHeight(), global.minAreaDepth(), global.maxAreaWidth(), global.maxAreaHeight(), global.maxAreaDepth(),
                    global.defaultAreaWidth(), global.defaultAreaHeight(), global.defaultAreaDepth(),
                    toggleVertical ? !global.verticalMiningEnabled() : global.verticalMiningEnabled(), read(verticalDepth),
                    toggleProtection ? !global.structureProtectionEnabled() : global.structureProtectionEnabled(),
                    toggleDropTransfer ? !global.transferExtraDropsToPlayer() : global.transferExtraDropsToPlayer(),
                    toggleDirectExperience ? !global.directExperience() : global.directExperience(),
                    toggleVeinDurability ? !global.veinMiningConsumesDurability() : global.veinMiningConsumesDurability(),
                    toggleVeinHunger ? !global.veinMiningConsumesHunger() : global.veinMiningConsumesHunger(),
                    toggleAreaDurability ? !global.areaMiningConsumesDurability() : global.areaMiningConsumesDurability(),
                    toggleAreaHunger ? !global.areaMiningConsumesHunger() : global.areaMiningConsumesHunger());
        } catch (NumberFormatException ignored) {
        }
    }

    private static int read(EditBox box) { return Integer.parseInt(box.getValue()); }
    private static void setIfUnfocused(EditBox box, int value) { if (!box.isFocused()) box.setValue(String.valueOf(value)); }
    private static String state(boolean enabled) { return enabled ? "开启" : "关闭"; }
}
