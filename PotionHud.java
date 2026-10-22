package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** Активные эффекты: название, уровень, время. Мигает, когда осталось < 10 сек. */
public final class PotionHud extends HudModule {
    public PotionHud() { super("Potions", "Активные эффекты", 0.82f, 0.4f); }

    private List<String[]> lines(boolean preview) {
        List<String[]> out = new ArrayList<>();
        if (mc.player != null) for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString();
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            String time = e.isInfinite() ? "∞" : String.format("%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
            out.add(new String[]{name, time, String.valueOf(e.getDuration())});
        }
        if (out.isEmpty() && preview) out.add(new String[]{"Скорость 2", "1:30", "1800"});
        return out;
    }

    @Override public float width() {
        int w = 60;
        for (String[] l : lines(true)) w = Math.max(w, Render.width(l[0] + "  " + l[1]) + 8);
        return w;
    }
    @Override public float height() { return Math.max(1, lines(true).size()) * (Render.height() + 2) + 4; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        int y = 3;
        boolean blink = (System.currentTimeMillis() / 300) % 2 == 0;
        for (String[] l : lines(preview)) {
            Render.text(ctx, l[0], 4, y, Theme.TEXT, true);
            boolean low = Integer.parseInt(l[2]) < 200;
            int tc = low ? (blink ? 0xFFEF4444 : Theme.TEXT_DIM) : Theme.TEXT_DIM;
            Render.text(ctx, l[1], width() - 4 - Render.width(l[1]), y, tc, true);
            y += Render.height() + 2;
        }
    }
}
