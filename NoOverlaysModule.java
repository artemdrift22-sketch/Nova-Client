package dev.nova.modules.qol;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;

/** No Fire Overlay + No Vignette + No Hurt Cam + скрытие боссбара/портала/воды. Всё чисто визуально. */
public final class NoOverlaysModule extends Module {
    public final BoolSetting fire = bool("Огонь", true);
    public final BoolSetting vignette = bool("Виньетка", true);
    public final BoolSetting hurtCam = bool("Тряска от урона", false);
    public final BoolSetting portal = bool("Портал", true);
    public final BoolSetting water = bool("Под водой", false);
    public final BoolSetting bossbar = bool("Боссбар", false);

    public NoOverlaysModule() { super("NoOverlays", "Убирает мешающие эффекты экрана", Category.QOL); }
}
