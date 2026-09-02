package com.timhxl.fastmine.client.mixin;

import com.timhxl.fastmine.client.FastMinePreviewRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 释放 FastMine 客户端预览渲染使用的显存缓冲区。 */
@Mixin(GameRenderer.class)
public final class FastMinePreviewRendererMixin {
    @Inject(at = @At("RETURN"), method = "close")
    private void fastmine$closePreviewRenderer(CallbackInfo info) {
        FastMinePreviewRenderer.close();
    }
}
