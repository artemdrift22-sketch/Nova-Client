package dev.nova.mixin;

import dev.nova.modules.qol.NoOverlaysModule;
import dev.nova.modules.qol.ZoomModule;
import dev.nova.util.Hooks;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void nova$fov(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ZoomModule.applyFov(cir.getReturnValue()));
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void nova$hurtCam(CallbackInfo ci) {
        NoOverlaysModule m = Hooks.get(NoOverlaysModule.class);
        if (m != null && m.enabled() && m.hurtCam.get()) ci.cancel();
    }
}
