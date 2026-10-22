package dev.nova.modules.client;

import dev.nova.gui.ClickGuiScreen;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.ColorSetting;
import org.lwjgl.glfw.GLFW;

/** Открывает ClickGUI (Right Shift). Держит акцентный цвет всего клиента. */
public final class ClickGuiModule extends Module {
    public final ColorSetting accent = color("Акцент", 0xFF8B5CF6);

    public ClickGuiModule() {
        super("ClickGUI", "Меню модулей. ПКМ по цвету — RGB", Category.CLIENT);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    protected void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
