package dev.nova.modules.client;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.ColorSetting;
import dev.nova.setting.ModeSetting;
import dev.nova.setting.NumberSetting;

/**
 * Глобальные настройки внешнего вида Nova: тема, акцент, шрифт, стиль HUD, анимации, размытие.
 * Работает всегда (вкл/выкл модуля ни на что не влияет) — это просто страница настроек.
 */
public final class InterfaceModule extends Module {
    private static InterfaceModule instance;

    public final ModeSetting theme = mode("Тема", "Midnight", "Midnight", "Nova Space");
    public final ModeSetting accentMode = mode("Акцент", "Голубой", "Голубой", "Золото", "Свой");
    public final ColorSetting customAccent = color("Свой акцент", 0xFFB388FF).visibleIf(() -> accentMode.is("Свой"));
    public final ModeSetting font = mode("Шрифт", "Nova Sans", "Nova Sans", "Minecraft");
    public final BoolSetting textShadow = bool("Тень текста", true);
    public final ModeSetting hudStyle = mode("Стиль HUD", "Pulse", "Pulse", "Стекло", "Минимал");
    public final NumberSetting hudOpacity = number("Прозрачность HUD", 1.0, 0.2, 1.6, 0.05);
    public final NumberSetting radius = number("Скругление HUD", 6, 0, 10, 1);
    public final BoolSetting headers = bool("Заголовки панелей", true);
    public final NumberSetting blur = number("Размытие меню", 6, 0, 10, 1);
    public final BoolSetting animations = bool("Анимации", true);
    public final NumberSetting animSpeed = number("Скорость анимаций", 1.0, 0.5, 2.0, 0.1).visibleIf(animations::get);

    public InterfaceModule() {
        super("Interface", "Тема, шрифт, стиль HUD, анимации", Category.CLIENT);
        instance = this;
    }

    public static InterfaceModule get() { return instance; }
}
