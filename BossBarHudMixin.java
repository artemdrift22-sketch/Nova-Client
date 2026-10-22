package dev.nova.mixin;

import dev.nova.modules.qol.NoOverlaysModule;
import dev.nova.util.Hooks;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void nova$render(CallbackInfo ci) {
        NoOverlaysModule m = Hooks.get(NoOverlaysModule.class);
        if (m != null && m.enabled() && m.bossbar.get()) ci.cancel();
    }
}
