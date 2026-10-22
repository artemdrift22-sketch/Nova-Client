package dev.nova.mixin;

import dev.nova.modules.qol.ZoomModule;
import dev.nova.util.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void nova$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen != null) return;
        if (action == GLFW.GLFW_PRESS) CpsTracker.onButton(button, true);
        else if (action == GLFW.GLFW_RELEASE) CpsTracker.onButton(button, false);
    }

    /** Колесо во время зума меняет кратность, а не слот хотбара. */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void nova$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen == null && ZoomModule.onScroll(vertical)) ci.cancel();
    }
}
