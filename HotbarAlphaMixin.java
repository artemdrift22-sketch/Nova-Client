package dev.nova.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nova.modules.hud.HotbarModule;
import dev.nova.util.Hooks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Делает ванильный хотбар полупрозрачным (модуль Hotbar). */
@Mixin(InGameHud.class)
public abstract class HotbarAlphaMixin {
    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void nova$hotbarAlphaStart(DrawContext ctx, RenderTickCounter tick, CallbackInfo ci) {
        HotbarModule m = Hooks.get(HotbarModule.class);
        if (m == null || !m.enabled()) return;
        ctx.draw();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, m.opacity.asFloat());
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void nova$hotbarAlphaEnd(DrawContext ctx, RenderTickCounter tick, CallbackInfo ci) {
        HotbarModule m = Hooks.get(HotbarModule.class);
        if (m == null || !m.enabled()) return;
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
