package dev.nova.render;

/**
 * Nova Visuals design system (Glassmorphism).
 * Все цвета — ARGB int (0xAARRGGBB), как ожидает DrawContext/BufferBuilder.
 */
public final class ThemeManager {
    private ThemeManager() {}

    // --- Фон ---
    /** Глубокий космический синий, RGBA(10,15,36,180). */
    public static final int BG = rgba(10, 15, 36, 180);
    /** Верх градиента панели — чуть светлее, даёт объём стекла. */
    public static final int BG_TOP = rgba(22, 30, 64, 190);
    public static final int SIDEBAR = rgba(255, 255, 255, 8);

    // --- Акценты (звезда) ---
    public static final int GOLD = rgba(255, 215, 0, 255);
    public static final int CYAN = rgba(0, 229, 255, 255);

    // --- Текст ---
    public static final int TEXT = rgba(255, 255, 255, 255);
    public static final int TEXT_DIM = rgba(170, 170, 170, 255);

    // --- Элементы ---
    public static final int CARD = rgba(255, 255, 255, 14);
    public static final int CARD_HOVER = rgba(255, 255, 255, 28);
    public static final int CARD_ON = rgba(0, 229, 255, 22);
    public static final int TOGGLE_OFF = rgba(90, 96, 120, 200);
    public static final int KNOB_OFF = rgba(200, 204, 216, 255);
    public static final int SHADOW = rgba(0, 0, 0, 110);

    // --- Обводка стекла: 1px белый 20% ---
    public static final int STROKE = rgba(255, 255, 255, 51);
    public static final float STROKE_WIDTH = 1f;

    // --- Скругления ---
    public static final float RADIUS_BUTTON = 6f;
    public static final float RADIUS_PANEL = 12f;
    public static final float RADIUS_HUD = 6f;

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    public static int withAlpha(int color, int alpha) {
        return ((alpha & 255) << 24) | (color & 0xFFFFFF);
    }

    /** Умножает текущую альфу цвета на k (0..1) — для fade-анимаций. */
    public static int fade(int color, float k) {
        int a = (int) (((color >>> 24) & 255) * Math.max(0f, Math.min(1f, k)));
        return withAlpha(color, a);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
