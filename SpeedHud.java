package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.ModeSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public final class SpeedHud extends HudModule {
    private final ModeSetting unit = mode("Единицы", "бл/с", "бл/с", "км/ч");
    private double lastX, lastZ, speed;
    public SpeedHud() { super("Speedometer", "Скорость передвижения", 0.005f, 0.82f); }

    @Override public void onTick() {
        if (!inGame()) return;
        double dx = mc.player.getX() - lastX, dz = mc.player.getZ() - lastZ;
        double now = Math.sqrt(dx * dx + dz * dz) * 20;
        speed = speed * 0.6 + (now > 100 ? 0 : now) * 0.4; // телепорты не считаем
        lastX = mc.player.getX(); lastZ = mc.player.getZ();
    }

    private String text() {
        double v = unit.is("км/ч") ? speed * 3.6 : speed;
        return String.format(Locale.ROOT, "%.2f %s", v, unit.get());
    }
    @Override public float width() { return Render.width("99.99 км/ч") + 8; }
    @Override public float height() { return Render.height() + 6; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        Render.text(ctx, text(), 4, 3, Theme.TEXT, true);
    }
}
