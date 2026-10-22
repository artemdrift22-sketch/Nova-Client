package dev.nova.render;

import dev.nova.modules.client.InterfaceModule;

/**
 * Nova Visuals design system.
 * Цвета — ARGB int (0xAARRGGBB). Часть цветов зависит от темы (Interface -> Тема) и
 * пересчитывается в apply() каждый кадр (это дёшево).
 *
 * Темы:
 *  - Midnight  : графит RGBA(15,15,20,220), обводка белым 30 — «премиальный» тёмный стиль.
 *  - Nova Space: космический синий RGBA(10,15,36,180), обводка белым 20%.
 */
public final class ThemeManager {
    private ThemeManager() {}

    // --- Фиксированные цвета бренда ---
    public static final int GOLD = rgba(255, 215, 0, 255);
    public static final int GOLD_LIGHT = rgba(255, 233, 120, 255);
    public static final int GOLD_DEEP = rgba(255, 176, 0, 255);
    public static final int CYAN = rgba(0, 229, 255, 255);
    public static final int TEXT = rgba(255, 255, 255, 255);
    public static final int TEXT_DIM = rgba(170, 170, 170, 255);
    public static final int RED = rgba(255, 84, 96, 255);
    public static final int GREEN = rgba(80, 230, 140, 255);

    // --- Цвета темы (меняются в apply) ---
    public static int BG = rgba(15, 15, 20, 220);
    public static int BG_TOP = rgba(30, 30, 40, 225);
    public static int SIDEBAR = rgba(255, 255, 255, 6);
    public static int CARD = rgba(255, 255, 255, 10);
    public static int CARD_HOVER = rgba(255, 255, 255, 20);
    public static int CARD_ON = rgba(0, 229, 255, 18);
    public static int TOGGLE_OFF = rgba(48, 48, 56, 235);
    public static int KNOB_OFF = rgba(125, 125, 135, 255);
    public static int SHADOW = rgba(0, 0, 0, 130);
    public static int STROKE = rgba(255, 255, 255, 30);
    /** Фон HUD-плашек: RGBA(10,10,15,150), прозрачность умножается на настройку. */
    public static int HUD_BG = rgba(10, 10, 15, 150);
    public static int HUD_STROKE = rgba(255, 255, 255, 30);
    /** Основной акцент (Interface -> Акцент). */
    public static int ACCENT = CYAN;

    public static final float STROKE_WIDTH = 1f;
    public static final float RADIUS_BUTTON = 6f;
    public static final float RADIUS_PANEL = 12f;
    public static float RADIUS_HUD = 6f;

    public static void apply() {
        InterfaceModule i = InterfaceModule.get();
        boolean space = i != null && i.theme.is("Nova Space");
        if (space) {
            BG = rgba(10, 15, 36, 180);
            BG_TOP = rgba(22, 30, 64, 190);
            STROKE = rgba(255, 255, 255, 51);
            HUD_BG = rgba(10, 15, 36, 160);
            TOGGLE_OFF = rgba(40, 46, 70, 235);
        } else {
            BG = rgba(15, 15, 20, 220);
            BG_TOP = rgba(30, 30, 40, 225);
            STROKE = rgba(255, 255, 255, 30);
            HUD_BG = rgba(10, 10, 15, 150);
            TOGGLE_OFF = rgba(48, 48, 56, 235);
        }
        HUD_STROKE = STROKE;
        if (i != null) {
            float k = i.hudOpacity.asFloat();
            HUD_BG = withAlpha(HUD_BG, Math.min(255, Math.round(((HUD_BG >>> 24) & 255) * k / 0.6f * 0.6f)));
            RADIUS_HUD = i.radius.asFloat();
            if (i.accentMode.is("Золото")) ACCENT = GOLD;
            else if (i.accentMode.is("Свой")) ACCENT = i.customAccent.color() | 0xFF000000;
            else ACCENT = CYAN;
        } else ACCENT = CYAN;
        CARD_ON = withAlpha(ACCENT, 20);

        Theme.BG = BG; Theme.PANEL = BG; Theme.ITEM = CARD; Theme.ITEM_HOVER = CARD_HOVER;
        Theme.HUD_BG = HUD_BG;
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    public static int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    /** Умножает альфу цвета на k (0..1) — для fade-анимаций. */
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

    // --- HSV для пипетки цвета ---
    public static int hsv(float h, float s, float v, int alpha) {
        h = ((h % 1f) + 1f) % 1f;
        float r, g, b;
        int i = (int) (h * 6);
        float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return rgba(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255), alpha);
    }

    /** -> {h, s, v} в 0..1 */
    public static float[] toHsv(int argb) {
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6f;
            if (h < 0) h += 1;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
