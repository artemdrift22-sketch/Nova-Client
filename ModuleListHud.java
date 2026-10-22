package dev.nova.modules.hud;

import dev.nova.NovaClient;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.render.Render;
import dev.nova.render.Theme;
import dev.nova.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.List;

/** Список включённых модулей справа сверху, отсортирован по ширине, с RGB-градиентом. */
public final class ModuleListHud extends Module {
    private final BoolSetting hideHud = bool("Скрывать HUD-модули", true);
    private final BoolSetting bg = bool("Фон", true);

    public ModuleListHud() { super("ModuleList", "Список включённых модулей", Category.HUD); }

    @Override
    public void onRender2D(DrawContext ctx, float delta) {
        List<Module> list = NovaClient.modules().all().stream()
            .filter(m -> m.enabled() && m != this)
            .filter(m -> !hideHud.get() || m.category() != Category.HUD)
            .sorted(Comparator.comparingInt((Module m) -> Render.width(m.name())).reversed())
            .toList();
        int sw = mc.getWindow().getScaledWidth();
        int y = 2, h = Render.height() + 2;
        for (Module m : list) {
            int w = Render.width(m.name());
            int x = sw - w - 4;
            if (bg.get()) Render.rect(ctx, x - 2, y, w + 4, h, Theme.HUD_BG);
            Render.rect(ctx, sw - 1, y, 1, h, Theme.accent(y * 15L));
            Render.text(ctx, m.name(), x, y + 1, Theme.accent(y * 15L), true);
            y += h;
        }
    }
}
