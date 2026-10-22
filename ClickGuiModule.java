package dev.nova.modules.client;

import dev.nova.gui.ClickGuiScreen;
import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.ColorSetting;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Открывает ClickGUI. Три способа: клавиша из Управление -> Nova (по умолчанию правый Shift),
 * запасная клавиша Insert и команда /nova в чате.
 */
public final class ClickGuiModule extends Module {
    public final ColorSetting accent = color("Акцент", 0xFF8B5CF6);

    private static final KeyBinding OPEN = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("key.nova.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nova"));
    private static final KeyBinding OPEN_ALT = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("key.nova.gui_alt", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, "category.nova"));
    private static boolean pending;

    public ClickGuiModule() {
        super("ClickGUI", "Меню модулей. ПКМ по цвету — RGB", Category.CLIENT);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
            dispatcher.register(ClientCommandManager.literal("nova").executes(c -> { pending = true; return 1; })));
    }

    /** Вызывается каждый тик из NovaClient, даже когда модуль выключен. */
    public static void tickOpen() {
        boolean pressed = false;
        while (OPEN.wasPressed()) pressed = true;
        while (OPEN_ALT.wasPressed()) pressed = true;
        if (mc.player == null) { pending = false; return; }
        if ((pressed || pending) && mc.currentScreen == null) {
            pending = false;
            mc.setScreen(new ClickGuiScreen());
        }
    }

    @Override
    protected void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
