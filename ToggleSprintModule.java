package dev.nova.modules.qol;

import dev.nova.hud.HudModule;
import dev.nova.module.Category;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

/** Toggle Sprint / Sneak + индикатор. Аналог ванильной опции, сервером не запрещено. */
public final class ToggleSprintModule extends HudModule {
    private final BoolSetting sneak = bool("Тогл приседа", false);
    private final BoolSetting indicator = bool("Индикатор", true);
    private boolean sneakToggled, sneakWasDown;

    public ToggleSprintModule() { super("ToggleSprint", "Бег без зажатия + индикатор", Category.QOL, 0.005f, 0.95f); background.set(false); }

    @Override public void onTick() {
        if (!inGame()) return;
        if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking() && !mc.player.horizontalCollision)
            mc.options.sprintKey.setPressed(true);
        if (sneak.get()) {
            boolean down = mc.options.sneakKey.isPressed();
            if (down && !sneakWasDown) sneakToggled = !sneakToggled;
            sneakWasDown = down;
            if (sneakToggled) mc.options.sneakKey.setPressed(true);
        }
    }

    @Override protected void onDisable() { mc.options.sprintKey.setPressed(false); sneakToggled = false; }

    private String text() {
        if (sneakToggled) return "[Присед (тогл)]";
        if (mc.player != null && mc.player.isSprinting()) return "[Бег (тогл)]";
        return "[Шаг]";
    }

    @Override public float width() { return Render.width("[Присед (тогл)]") + 8; }
    @Override public float height() { return Render.height() + 6; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        if (indicator.get() || preview) Render.text(ctx, text(), 4, 3, Theme.TEXT, true);
    }
}
