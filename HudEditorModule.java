package dev.nova.modules.client;

import dev.nova.hud.HudEditorScreen;
import dev.nova.module.Category;
import dev.nova.module.Module;

public final class HudEditorModule extends Module {
    public HudEditorModule() { super("HUD Editor", "Перетаскивание и масштаб HUD-элементов", Category.CLIENT); }

    @Override
    protected void onEnable() {
        mc.setScreen(new HudEditorScreen());
        setEnabled(false);
    }
}
