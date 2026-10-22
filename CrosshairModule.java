package dev.nova.modules.hud;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Render;
import dev.nova.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;

/** Свой прицел: стили, цвет, обводка, динамика от движения, подсветка при наведении на сущность. */
public final class CrosshairModule extends Module {
    private final ModeSetting style = mode("Стиль", "Крест", "Крест", "T", "Точка", "Круг", "Крест + точка");
    private final NumberSetting length = number("Длина", 4, 1, 12, 1);
    private final NumberSetting gap = number("Зазор", 2, 0, 10, 1);
    private final NumberSetting thickness = number("Толщина", 1, 1, 4, 1);
    private final ColorSetting color = color("Цвет", 0xFFFFFFFF);
    private final ColorSetting targetColor = color("Цвет на цели", 0xFFEF4444);
    private final BoolSetting outline = bool("Обводка", true);
    private final BoolSetting dynamic = bool("Динамический", true);
    private final BoolSetting thirdPerson = bool("В 3-м лице", false);
    private float spread;

    public CrosshairModule() { super("Crosshair", "Свой прицел вместо ванильного", Category.HUD); }

    private void bar(DrawContext c, float x, float y, float w, float h, int col) {
        if (outline.get()) Render.rect(c, x - 1, y - 1, w + 2, h + 2, 0xFF000000);
        Render.rect(c, x, y, w, h, col);
    }

    @Override public void onRender2D(DrawContext ctx, float delta) {
        if (!inGame() || mc.currentScreen != null) return;
        if (!mc.options.getPerspective().isFirstPerson() && !thirdPerson.get()) return;
        float cx = mc.getWindow().getScaledWidth() / 2f, cy = mc.getWindow().getScaledHeight() / 2f;

        float target = 0;
        if (dynamic.get()) {
            double v = mc.player.getVelocity().horizontalLength() * 20;
            target = (float) Math.min(6, v * 0.8) + (1 - mc.player.getAttackCooldownProgress(0)) * 3;
            if (!mc.player.isOnGround()) target += 2;
        }
        spread += (target - spread) * 0.25f;

        boolean onTarget = mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY
            && ((EntityHitResult) mc.crosshairTarget).getEntity().isAlive();
        int col = onTarget ? targetColor.color() : color.color();
        float t = thickness.asFloat(), l = length.asFloat(), g = gap.asFloat() + spread;
        float h0 = t / 2f;

        switch (style.get()) {
            case "Точка" -> bar(ctx, cx - h0, cy - h0, t, t, col);
            case "Круг" -> {
                float r = g + l / 2f;
                for (int i = 0; i < 48; i++) {
                    double a = Math.PI * 2 * i / 48;
                    Render.rect(ctx, (float) (cx + Math.cos(a) * r) - h0, (float) (cy + Math.sin(a) * r) - h0, t, t, col);
                }
                bar(ctx, cx - h0, cy - h0, t, t, col);
            }
            default -> {
                if (!style.is("T")) bar(ctx, cx - h0, cy - g - l, t, l, col); // верх
                bar(ctx, cx - h0, cy + g, t, l, col);       // низ
                bar(ctx, cx - g - l, cy - h0, l, t, col);   // лево
                bar(ctx, cx + g, cy - h0, l, t, col);       // право
                if (style.is("Крест + точка")) bar(ctx, cx - h0, cy - h0, t, t, col);
            }
        }
    }
}
