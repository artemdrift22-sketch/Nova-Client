package dev.nova.mixin;

import dev.nova.modules.qol.NoOverlaysModule;
import dev.nova.util.Hooks;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void nova$fire(CallbackInfo ci) {
        NoOverlaysModule m = Hooks.get(NoOverlaysModule.class);
        if (m != null && m.enabled() && m.fire.get()) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void nova$water(CallbackInfo ci) {
        NoOverlaysModule m = Hooks.get(NoOverlaysModule.class);
        if (m != null && m.enabled() && m.water.get()) ci.cancel();
    }
}
