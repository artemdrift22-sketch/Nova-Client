package dev.nova.modules.hud;

import dev.nova.hud.AnimatedList;
import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.Comparator;

/** Эффекты: цветная точка эффекта, имя + уровень, время. Плавное появление/исчезновение, мигает < 10 сек. */
public final class PotionHud extends HudModule {
    private static final float ROW_H = 12;
    private final AnimatedList list = new AnimatedList();
    private float contentW = 90;

    public PotionHud() { super("Potions", "Активные эффекты", 0.82f, 0.4f); }

    @Override protected String header() { return "Potions"; }

    @Override protected void update(boolean preview) {
        list.begin();
        if (mc.player != null) for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString();
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            String time = e.isInfinite() ? "∞" : String.format("%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
            AnimatedList.Entry en = list.put(e.getEffectType().getKey().map(k -> k.getValue().toString()).orElse(name), name, time, 0xFF000000 | e.getEffectType().value().getColor());
            en.sort = e.isInfinite() ? Long.MAX_VALUE : e.getDuration();
        }
        if (preview && !list.anyAlive()) {
            AnimatedList.Entry en = list.put("preview", "Скорость 2", "1:30", 0xFF33EBFF);
            en.sort = 1800;
        }
        list.end(Comparator.comparingLong((AnimatedList.Entry e) -> e.sort).reversed(), ROW_H);
        float w = 90;
        for (AnimatedList.Entry e : list.entries()) w = Math.max(w, PAD + 9 + Render.width(e.left) + 14 + Render.width(e.right) + PAD);
        contentW = w;
    }

    @Override protected boolean shouldShow(boolean preview) { return list.anyAlive(); }

    @Override public float width() { return contentW; }
    @Override public float height() { return Math.max(ROW_H, list.height(ROW_H)) + 6; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        float base = Render2DUtils.alpha;
        float w = fullWidth();
        boolean blink = (System.currentTimeMillis() / 300) % 2 == 0;
        for (AnimatedList.Entry e : list.entries()) {
            float t = e.t();
            Render2DUtils.alpha = base * t;
            float y = 3 + e.y();
            float dx = 8 * (1 - t);
            Render2DUtils.drawGlow(ctx, PAD + dx, y + 2, 5, 5, 2.5f, 2, ThemeManager.withAlpha(e.color, 90));
            Render2DUtils.drawCircle(ctx, PAD + 2.5f + dx, y + 4.5f, 2.5f, e.color);
            Render.text(ctx, e.left, PAD + 9 + dx, y + 1, ThemeManager.TEXT, false);
            boolean low = e.sort < 200;
            int tc = low && blink ? ThemeManager.RED : ThemeManager.TEXT_DIM;
            Render.text(ctx, e.right, w - PAD - Render.width(e.right) + dx, y + 1, tc, false);
        }
        Render2DUtils.alpha = base;
    }
}
