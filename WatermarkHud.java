package dev.nova.modules.hud;

import dev.nova.NovaClient;
import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Вотермарк-пилюля в стиле Pulse:  ★ Nova | ник | 144 fps | 32 ms | 21:30 */
public final class WatermarkHud extends HudModule {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final BoolSetting nick = bool("Ник", true);
    private final BoolSetting fps = bool("FPS", true);
    private final BoolSetting ping = bool("Пинг", true);
    private final BoolSetting time = bool("Время", true);
    private final BoolSetting version = bool("Версия", false);

    public WatermarkHud() { super("Watermark", "Логотип Nova", 0.005f, 0.005f); }

    private List<String> segments() {
        List<String> out = new ArrayList<>();
        if (version.get()) out.add(NovaClient.VERSION);
        if (nick.get()) out.add(mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername());
        if (fps.get()) out.add(mc.getCurrentFps() + " fps");
        if (ping.get()) {
            int p = 0;
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) p = e.getLatency();
            }
            out.add(p + " ms");
        }
        if (time.get()) out.add(LocalTime.now().format(TIME));
        return out;
    }

    @Override public float width() {
        float w = 22 + Render.width(NovaClient.NAME);
        for (String s : segments()) w += 13 + Render.width(s);
        return w + PAD + 1;
    }

    @Override public float height() { return Render.height() + 10; }

    @Override protected void drawBackground(DrawContext ctx, float w, float h) {
        // пилюля: скругление = половина высоты
        float r = Math.min(h / 2f, ThemeManager.RADIUS_HUD + 3);
        if (hudStyle().equals("Стекло")) Render2DUtils.drawGlassPanel(ctx, 0, 0, w, h, r);
        else if (hudStyle().equals("Минимал")) Render2DUtils.drawRoundedRect(ctx, 0, 0, w, h, r, ThemeManager.HUD_BG);
        else Render2DUtils.drawHudPanel(ctx, 0, 0, w, h, r, true);
    }

    @Override
    protected void draw(DrawContext ctx, float delta, boolean preview) {
        Render2DUtils.drawGlow(ctx, 7, 4.5f, 10, 10, 5, 3, ThemeManager.withAlpha(ThemeManager.GOLD, 60));
        Render2DUtils.drawNovaLogo(ctx, 7, 4.5f, 10);
        float x = Render.text(ctx, NovaClient.NAME, 21, 6, ThemeManager.GOLD, false);
        for (String s : segments()) {
            Render2DUtils.drawRoundedRect(ctx, x + 6, 5, 1, 9, 0.5f, ThemeManager.withAlpha(ThemeManager.TEXT, 40));
            x = Render.text(ctx, s, x + 13, 6, ThemeManager.TEXT, false);
        }
    }
}
