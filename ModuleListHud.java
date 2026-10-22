package dev.nova.modules.hud;

import dev.nova.NovaClient;
import dev.nova.hud.AnimatedList;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Render;
import dev.nova.render.Render2DUtils;
import dev.nova.render.Theme;
import dev.nova.render.ThemeManager;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;

/** ArrayList справа сверху: модули въезжают справа при включении и уезжают при выключении, плавная сортировка. */
public final class ModuleListHud extends Module {
    private static final float ROW_H = 12;
    private final BoolSetting hideHud = bool("Скрывать HUD-модули", true);
    private final BoolSetting bg = bool("Фон", true);
    private final BoolSetting rainbow = bool("Перелив", true);
    private final AnimatedList list = new AnimatedList();

    public ModuleListHud() { super("ModuleList", "Список включённых модулей", Category.HUD); }

    @Override
    public void onRender2D(DrawContext ctx, float delta) {
        list.begin();
        for (Module m : NovaClient.modules().all()) {
            if (!m.enabled() || m == this) continue;
            if (hideHud.get() && m.category() == Category.HUD) continue;
            if (m.category() == Category.CLIENT) continue;
            list.put(m.name(), m.name(), "", ThemeManager.TEXT).sort = Render.width(m.name());
        }
        list.end(Comparator.comparingLong((AnimatedList.Entry e) -> e.sort).reversed().thenComparing(e -> e.key), ROW_H);

        int sw = mc.getWindow().getScaledWidth();
        float base = Render2DUtils.alpha;
        try {
            for (AnimatedList.Entry e : list.entries()) {
                float t = e.t();
                Render2DUtils.alpha = base * t;
                float y = 3 + e.y();
                int w = Render.width(e.left);
                float x = sw - w - 9 + (w + 14) * (1 - t);
                int c = rainbow.get() ? Theme.accent((long) (y * 15)) : ThemeManager.ACCENT;
                if (bg.get()) Render2DUtils.drawRoundedRect(ctx, x - 4, y, w + 8, ROW_H - 1, 3, ThemeManager.HUD_BG);
                Render2DUtils.drawRoundedRect(ctx, x + w + 5, y + 2, 1.5f, ROW_H - 5, 0.75f, c);
                Render.text(ctx, e.left, x, y + 2, c, false);
            }
        } finally {
            Render2DUtils.alpha = base;
        }
    }
}
