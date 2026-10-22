package dev.nova.modules.hud;

import dev.nova.NovaClient;
import dev.nova.hud.AnimatedList;
import dev.nova.hud.HudModule;
import dev.nova.module.Module;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

import java.util.Comparator;
import java.util.Locale;

/** Панель «Hotkeys» как на референсе: модули с биндами, плавное появление строк. */
public final class KeybindsHud extends HudModule {
    private static final float ROW_H = 12;
    private final BoolSetting onlyEnabled = bool("Только включённые", false);
    private final AnimatedList list = new AnimatedList();
    private float contentW = 90;

    public KeybindsHud() { super("Hotkeys", "Список биндов модулей", 0.005f, 0.32f); }

    @Override protected String header() { return "Hotkeys"; }

    private static String keyName(int key) {
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    @Override protected void update(boolean preview) {
        list.begin();
        for (Module m : NovaClient.modules().all()) {
            if (m.key() < 0) continue;
            if (onlyEnabled.get() && !m.enabled()) continue;
            list.put(m.name(), m.name(), keyName(m.key()), m.enabled() ? ThemeManager.TEXT : ThemeManager.TEXT_DIM);
        }
        if (preview && !list.anyAlive()) list.put("preview", "ClickGUI", "RSHIFT", ThemeManager.TEXT);
        list.end(Comparator.comparing(e -> e.key), ROW_H);
        float w = 90;
        for (AnimatedList.Entry e : list.entries()) w = Math.max(w, PAD + Render.width(e.left) + 14 + Render.width(e.right) + 8 + PAD);
        contentW = w;
    }

    @Override protected boolean shouldShow(boolean preview) { return list.anyAlive(); }

    @Override public float width() { return contentW; }
    @Override public float height() { return Math.max(ROW_H, list.height(ROW_H)) + 6; }

    @Override protected void draw(DrawContext ctx, float delta, boolean preview) {
        float base = Render2DUtils.alpha;
        float w = fullWidth();
        for (AnimatedList.Entry e : list.entries()) {
            float t = e.t();
            Render2DUtils.alpha = base * t;
            float y = 3 + e.y();
            float dx = -8 * (1 - t);
            boolean on = e.color == ThemeManager.TEXT;
            Render2DUtils.drawCircle(ctx, PAD + 2 + dx, y + 4.5f, 2, on ? ThemeManager.ACCENT : ThemeManager.withAlpha(ThemeManager.TEXT, 60));
            Render.text(ctx, e.left, PAD + 8 + dx, y + 1, e.color, false);
            float kw = Render.width(e.right) + 8;
            Render2DUtils.drawRoundedRect(ctx, w - PAD - kw - dx, y, kw, 10, 3, ThemeManager.withAlpha(on ? ThemeManager.ACCENT : ThemeManager.TEXT, on ? 45 : 22));
            Render.text(ctx, e.right, w - PAD - kw + 4 - dx, y + 1, on ? ThemeManager.TEXT : ThemeManager.TEXT_DIM, false);
        }
        Render2DUtils.alpha = base;
    }
}
