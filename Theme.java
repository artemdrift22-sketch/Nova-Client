package dev.nova.render;

import dev.nova.NovaClient;
import dev.nova.modules.client.ClickGuiModule;

/** Единая палитра: всё берёт цвета отсюда, акцент настраивается в ClickGUI. */
public final class Theme {
    private Theme() {}
    public static final int BG = 0xE0101014;
    public static final int PANEL = 0xF0181820;
    public static final int ITEM = 0xFF20202A;
    public static final int ITEM_HOVER = 0xFF2A2A36;
    public static final int TEXT = 0xFFEDEDF2;
    public static final int TEXT_DIM = 0xFF8A8A99;
    public static final int HUD_BG = 0x90000000;

    public static int accent() { return NovaClient.modules().get(ClickGuiModule.class).accent.color(); }
    public static int accent(long offset) { return NovaClient.modules().get(ClickGuiModule.class).accent.color(offset); }
}
