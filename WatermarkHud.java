package dev.nova.modules.hud;

import dev.nova.NovaClient;
import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

/** Логотип: стеклянная пилюля со звездой — ★ Nova | Visuals | 0.3.0 | 144 fps */
public final class WatermarkHud extends HudModule {
    private final BoolSetting version = bool("Версия", true);
    private final BoolSetting fps = bool("FPS", true);

    public WatermarkHud() { super("Watermark", "Логотип Nova", 0.005f, 0.005f); }

    private String rest() {
        StringBuilder sb = new StringBuilder();
        if (version.get()) sb.append("  ").append(NovaClient.VERSION);
        if (fps.get()) sb.append("  ").append(mc.getCurrentFps()).append(" fps");
        return sb.toString();
    }

    @Override public float width() { return 24 + Render.width(NovaClient.NAME) + Render.width(" Visuals") + Render.width(rest()) + 8; }
    @Override public float height() { return Render.height() + 10; }

    @Override
    protected void draw(DrawContext ctx, float delta, boolean preview) {
        Render2DUtils.drawNovaLogo(ctx, 7, 4.5f, 10);
        int x = Render.text(ctx, NovaClient.NAME, 22, 6, ThemeManager.TEXT, false);
        x = Render.text(ctx, " Visuals", x, 6, ThemeManager.GOLD, false);
        Render.text(ctx, rest(), x, 6, ThemeManager.TEXT_DIM, false);
    }
}
