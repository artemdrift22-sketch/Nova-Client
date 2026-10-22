package dev.nova.modules.qol;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;

public final class HideNametagsModule extends Module {
    public final BoolSetting players = bool("Игроки", true);
    public final BoolSetting mobs = bool("Мобы", true);
    public HideNametagsModule() { super("HideNametags", "Скрывает ники над головами (для скринов/видео)", Category.QOL); }
}
