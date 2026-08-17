package com.timhxl.fastmine.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.timhxl.fastmine.FastMineMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * FastMine 客户端快捷键。
 */
public final class FastMineKeyBindings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(FastMineMod.MOD_ID, "keybinds")
    );

    public static KeyMapping openSettings;

    private FastMineKeyBindings() {
    }

    /**
     * 注册默认 V 键；玩家可在原版控制设置中重新绑定。
     */
    public static void register() {
        openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fastmine.open_settings",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_V,
                CATEGORY
        ));
    }
}
