package dev.nova.modules.pvp;

import dev.nova.hud.HudModule;
import dev.nova.module.Category;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.util.CombatTracker;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Только ПОКАЗЫВАЕТ дистанцию последнего удара. Реальный reach не меняет. */
public final class ReachHud extends HudModule {
    public ReachHud() { super("ReachDisplay", "Дистанция последнего удара", Category.PVP, 0.005f, 0.66f); }
    @Override public float width() { return Render.width("9.99 бл") + 8; }
    @Override public float height() { return Render.height() + 6; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        boolean fresh = System.currentTimeMillis() - CombatTracker.lastHitTime < 3000;
        String s = fresh || preview ? String.format(Locale.ROOT, "%.2f бл", CombatTracker.lastReach) : "- бл";
        Render.text(ctx, s, 4, 3, Theme.TEXT, true);
    }
}
