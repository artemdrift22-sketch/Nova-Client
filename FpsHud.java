package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import net.minecraft.client.gui.DrawContext;

public final class FpsHud extends HudModule {
    public FpsHud() { super("FPS", "Кадры в секунду", 0.005f, 0.06f); }
    private String text() { return mc.getCurrentFps() + " FPS"; }
    @Override public float width() { return Render.width("9999 FPS") + 8; }
    @Override public float height() { return Render.height() + 6; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        int fps = mc.getCurrentFps();
        int color = fps >= 60 ? Theme.TEXT : fps >= 30 ? 0xFFFACC15 : 0xFFEF4444;
        Render.text(ctx, text(), (width() - Render.width(text())) / 2f, 3, color, true);
    }
}
