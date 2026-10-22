package dev.nova.modules.hud;

import dev.nova.hud.HudModule;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public final class PingHud extends HudModule {
    public PingHud() { super("Ping", "Задержка до сервера", 0.005f, 0.1f); }

    private int ping() {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    @Override public float width() { return Render.width("999 ms") + 8; }
    @Override public float height() { return Render.height() + 6; }
    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        int p = ping();
        String s = (mc.isInSingleplayer() ? "SP" : p + " ms");
        int color = p < 80 ? 0xFF22C55E : p < 150 ? 0xFFFACC15 : 0xFFEF4444;
        Render.text(ctx, s, (width() - Render.width(s)) / 2f, 3, mc.isInSingleplayer() ? Theme.TEXT_DIM : color, true);
    }
}
