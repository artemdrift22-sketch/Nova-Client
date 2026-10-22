package dev.nova.render;

import dev.nova.NovaClient;
import dev.nova.modules.client.ClickGuiModule;

/** Совместимость со старыми модулями: теперь все цвета берутся из ThemeManager (Nova Glass). */
public final class Theme {
    private Theme() {}
    public static final int BG = ThemeManager.BG;
    public static final int PANEL = ThemeManager.BG;
    public static final int ITEM = ThemeManager.CARD;
    public static final int ITEM_HOVER = ThemeManager.CARD_HOVER;
    public static final int TEXT = ThemeManager.TEXT;
    public static final int TEXT_DIM = ThemeManager.TEXT_DIM;
    public static final int HUD_BG = ThemeManager.rgba(255, 255, 255, 16);

    public static int accent() { return NovaClient.modules().get(ClickGuiModule.class).accent.color(); }
    public static int accent(long offset) { return NovaClient.modules().get(ClickGuiModule.class).accent.color(offset); }
}
