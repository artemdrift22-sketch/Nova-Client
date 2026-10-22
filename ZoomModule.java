package dev.nova.modules.qol;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Smooth Zoom (зажать C) + колесо для кратности + Custom FOV. Клавиша меняется в Управлении → Nova. */
public final class ZoomModule extends Module {
    private static ZoomModule instance;
    private static final KeyBinding KEY = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("key.nova.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.nova"));

    private final NumberSetting zoom = number("Кратность", 4, 1.5, 30, 0.5);
    private final BoolSetting smooth = bool("Плавно", true);
    private final BoolSetting scroll = bool("Колесо", true);
    private final BoolSetting cinematic = bool("Сглаживание мыши", true);
    private final NumberSetting customFov = number("Свой FOV (0 = выкл)", 0, 0, 130, 1);

    private double current = 1, extra = 1;
    private long last = System.nanoTime();
    private boolean forcedCinematic;

    public ZoomModule() {
        super("Zoom", "Плавное приближение и свой FOV", Category.QOL);
        instance = this;
    }

    @Override public void onTick() {
        boolean held = KEY.isPressed();
        if (!held) extra = 1;
        if (cinematic.get()) {
            if (held && !mc.options.smoothCameraEnabled) { mc.options.smoothCameraEnabled = true; forcedCinematic = true; }
            if (!held && forcedCinematic) { mc.options.smoothCameraEnabled = false; forcedCinematic = false; }
        }
    }

    @Override protected void onDisable() {
        if (forcedCinematic) { mc.options.smoothCameraEnabled = false; forcedCinematic = false; }
        current = 1;
    }

    public static float applyFov(float fov) {
        ZoomModule z = instance;
        if (z == null || !z.enabled()) return fov;
        if (z.customFov.get() > 0) fov = (float) (fov * z.customFov.get() / z.mc.options.getFov().getValue());
        double target = KEY.isPressed() ? z.zoom.get() * z.extra : 1;
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - z.last) / 1e9);
        z.last = now;
        z.current = z.smooth.get() ? z.current + (target - z.current) * Math.min(1, dt * 12) : target;
        return (float) (fov / z.current);
    }

    public static boolean onScroll(double v) {
        ZoomModule z = instance;
        if (z == null || !z.enabled() || !z.scroll.get() || !KEY.isPressed() || v == 0) return false;
        z.extra = Math.max(0.5, Math.min(5, z.extra * (v > 0 ? 1.25 : 0.8)));
        return true;
    }
}
