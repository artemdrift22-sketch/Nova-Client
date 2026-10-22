package dev.nova.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Общие примитивы рисования. Всё в GUI-координатах (уже с учётом GUI scale). */
public final class Render {
    private Render() {}

    public static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    public static void rect(DrawContext c, float x, float y, float w, float h, int color) {
        c.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color);
    }

    public static void outline(DrawContext c, float x, float y, float w, float h, int color) {
        rect(c, x, y, w, 1, color);
        rect(c, x, y + h - 1, w, 1, color);
        rect(c, x, y + 1, 1, h - 2, color);
        rect(c, x + w - 1, y + 1, 1, h - 2, color);
    }

    /** Скруглённый прямоугольник ступеньками (без шейдера, дёшево). r = 0..4 */
    public static void rounded(DrawContext c, float x, float y, float w, float h, int r, int color) {
        if (r <= 0) { rect(c, x, y, w, h, color); return; }
        rect(c, x + r, y, w - 2 * r, h, color);
        for (int i = 0; i < r; i++) {
            int inset = r - (int) Math.round(Math.sqrt(r * r - (r - i - 0.5) * (r - i - 0.5)));
            rect(c, x + i, y + inset, 1, h - 2 * inset, color);
            rect(c, x + w - 1 - i, y + inset, 1, h - 2 * inset, color);
        }
    }

    public static void gradientH(DrawContext c, int x, int y, int w, int h, int left, int right) {
        for (int i = 0; i < w; i++) c.fill(x + i, y, x + i + 1, y + h, lerpColor(left, right, i / (float) Math.max(1, w - 1)));
    }

    public static int text(DrawContext c, String s, float x, float y, int color, boolean shadow) {
        return c.drawText(font(), s, Math.round(x), Math.round(y), color, shadow);
    }

    public static int width(String s) { return font().getWidth(s); }
    public static int height() { return font().fontHeight; }

    public static int alpha(int color, float a) {
        int base = (color >>> 24) & 0xFF;
        return ((int) (base * a) << 24) | (color & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static boolean hovered(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
