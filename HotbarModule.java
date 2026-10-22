package dev.nova.modules.hud;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;

/** Ванильный хотбар, но полупрозрачный (по умолчанию 0.8). Кастомный хотбар удалён. */
public final class HotbarModule extends Module {
    public final NumberSetting opacity = number("Непрозрачность", 0.8, 0.2, 1.0, 0.05);

    public HotbarModule() { super("Hotbar", "Полупрозрачный ванильный хотбар", Category.HUD); }
}
