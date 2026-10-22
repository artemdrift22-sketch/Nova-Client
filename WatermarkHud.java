package dev.nova.modules.hud;

import dev.nova.NovaClient;
import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

public final class WatermarkHud extends HudModule {
    private final BoolSetting version = bool("Версия", true);
    private final BoolSetting fps = bool("FPS", true);

    public WatermarkHud() { super("Watermark", "Логотип Nova", 0.005f, 0.005f); }

    private String rest() {
        StringBuilder sb = new StringBuilder();
        if (version.get()) sb.append(" ").append(NovaClient.VERSION);
        if (fps.get()) sb.append(" | ").append(mc.getCurrentFps()).append(" fps");
        return sb.toString();
    }

    @Override public float width() { return Render.width(NovaClient.NAME + rest()) + 8; }
    @Override public float height() { return Render.height() + 6; }

    @Override
    protected void draw(DrawContext ctx, float delta, boolean preview) {
        Render.gradientH(ctx, 0, 0, (int) width(), 1, Theme.accent(0), Theme.accent(800));
        int nx = Render.text(ctx, NovaClient.NAME, 4, 4, Theme.accent(), true);
        Render.text(ctx, rest(), nx, 4, Theme.TEXT, true);
    }
}
