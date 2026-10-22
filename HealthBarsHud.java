package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Liquid Glass Bars: ХП (+поглощение), броня, голод и насыщение. Полоски плавно догоняют значение, урон оставляет «хвост». */
public final class HealthBarsHud extends HudModule {
    private static final int W = 120, BAR = 7;
    private final BoolSetting armor = bool("Броня", true);
    private final BoolSetting hunger = bool("Голод и насыщение", true);
    private final BoolSetting numbers = bool("Числа", true);
    private float hpShown = 20, hpTrail = 20, foodShown = 20;
    private long last = System.nanoTime();

    public HealthBarsHud() { super("HealthBars", "Стеклянные полоски ХП/брони/голода", 0.5f - 60f / 854f, 0.78f); }

    private int rows() { return 1 + (armor.get() ? 1 : 0) + (hunger.get() ? 1 : 0); }
    @Override public float width() { return W + 6; }
    @Override public float height() { return rows() * (BAR + 3) + 3; }

    private void glassBar(DrawContext c, float x, float y, float fill, float trail, int col, String label) {
        float w = W;
        Render.rounded(c, x, y, w, BAR, 2, 0x60000000);
        if (trail > fill) Render.rect(c, x + 1, y + 1, (w - 2) * Math.min(1, trail), BAR - 2, 0xA0FFFFFF);
        Render.rect(c, x + 1, y + 1, (w - 2) * Math.max(0, Math.min(1, fill)), BAR - 2, col);
        Render.rect(c, x + 1, y + 1, (w - 2) * Math.max(0, Math.min(1, fill)), 1, 0x50FFFFFF); // блик «стекла»
        if (numbers.get()) {
            c.getMatrices().push();
            c.getMatrices().translate(x + w / 2f, y + 1, 0);
            c.getMatrices().scale(0.6f, 0.6f, 1);
            Render.text(c, label, -Render.width(label) / 2f, 0, Theme.TEXT, true);
            c.getMatrices().pop();
        }
    }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        float hp = mc.player == null ? 20 : mc.player.getHealth();
        float max = mc.player == null ? 20 : mc.player.getMaxHealth();
        float abs = mc.player == null ? 0 : mc.player.getAbsorptionAmount();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        hpShown += (hp - hpShown) * Math.min(1, dt * 10);
        hpTrail = hpTrail > hpShown ? hpTrail - dt * max * 0.4f : hpShown;

        float y = 3;
        int hpCol = hp / max > 0.5f ? 0xFFEF4444 : hp / max > 0.25f ? 0xFFF97316 : 0xFFDC2626;
        glassBar(ctx, 3, y, hpShown / max, hpTrail / max, hpCol, String.format(Locale.ROOT, "%.1f / %.0f", hp, max) + (abs > 0 ? "  +" + (int) abs : ""));
        if (abs > 0) Render.rect(ctx, 4, y + 1, (W - 2) * Math.min(1, abs / max), 2, 0xFFFACC15);
        y += BAR + 3;
        if (armor.get()) {
            int a = mc.player == null ? 20 : mc.player.getArmor();
            glassBar(ctx, 3, y, a / 20f, 0, 0xFF3B82F6, a + " брони");
            y += BAR + 3;
        }
        if (hunger.get()) {
            int food = mc.player == null ? 20 : mc.player.getHungerManager().getFoodLevel();
            float sat = mc.player == null ? 5 : mc.player.getHungerManager().getSaturationLevel();
            foodShown += (food - foodShown) * Math.min(1, dt * 10);
            glassBar(ctx, 3, y, foodShown / 20f, 0, 0xFFD97706, food + " еда  •  " + String.format(Locale.ROOT, "%.1f", sat) + " насыщ.");
            Render.rect(ctx, 4, y + BAR - 3, (W - 2) * Math.min(1, sat / 20f), 2, 0xFFFDE68A);
        }
    }
}
