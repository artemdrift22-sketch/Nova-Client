package dev.nova.render;

/** Совместимость со старыми модулями: значения синхронизируются из ThemeManager.apply(). */
public final class Theme {
    private Theme() {}
    public static int BG = ThemeManager.BG;
    public static int PANEL = ThemeManager.BG;
    public static int ITEM = ThemeManager.CARD;
    public static int ITEM_HOVER = ThemeManager.CARD_HOVER;
    public static final int TEXT = ThemeManager.TEXT;
    public static final int TEXT_DIM = ThemeManager.TEXT_DIM;
    public static int HUD_BG = ThemeManager.HUD_BG;

    public static int accent() { return ThemeManager.ACCENT; }

    /** Переливание акцента по смещению (для списков): акцент -> светлее -> акцент. */
    public static int accent(long offset) {
        double t = (Math.sin((System.currentTimeMillis() + offset * 4) / 600.0) + 1) / 2;
        return ThemeManager.lerp(ThemeManager.ACCENT, ThemeManager.lerp(ThemeManager.ACCENT, ThemeManager.TEXT, 0.55f), (float) t);
    }
}
