package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import dev.nova.util.CpsTracker;
import net.minecraft.client.gui.DrawContext;

public final class CpsHud extends HudModule {
    private final BoolSetting right = bool("ПКМ", true);
    public CpsHud() { super("CPS", "Клики в секунду", 0.005f, 0.14f); }
    private String text() { return right.get() ? CpsTracker.left() + " | " + CpsTracker.right() + " CPS" : CpsTracker.left() + " CPS"; }
    @Override public float width() { return Render.width("20 | 20 CPS") + 8; }
    @Override public float height() { return Render.height() + 6; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        Render.text(ctx, text(), (width() - Render.width(text())) / 2f, 3, Theme.TEXT, true);
    }
}
