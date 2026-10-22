package dev.nova.mixin;

import dev.nova.modules.hud.CrosshairModule;
import dev.nova.modules.qol.NoOverlaysModule;
import dev.nova.util.Hooks;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void nova$crosshair(CallbackInfo ci) {
        if (Hooks.on(CrosshairModule.class)) ci.cancel();
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void nova$vignette(CallbackInfo ci) {
        NoOverlaysModule m = Hooks.get(NoOverlaysModule.class);
        if (m != null && m.enabled() && m.vignette.get()) ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void nova$portal(CallbackInfo ci) {
        NoOverlaysModule m = Hooks.get(NoOverlaysModule.class);
        if (m != null && m.enabled() && m.portal.get()) ci.cancel();
    }
}
