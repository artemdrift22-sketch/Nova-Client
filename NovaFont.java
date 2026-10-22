package dev.nova.render;

import dev.nova.modules.client.InterfaceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Шрифт Nova Sans (TTF, гладкий, с кириллицей).
 * Загружается ресурсами мода: assets/nova/font/main.json -> nova_font.ttf (oversample 4, без мыла).
 * Если шрифт не загрузился или в настройках выбран Minecraft — используется ванильный.
 * Позиция дробная (через матрицу), поэтому текст плавно едет в анимациях, а не прыгает по пикселям.
 */
public final class NovaFont {
    private NovaFont() {}

    public static final Identifier ID = Identifier.of("nova", "main");
    private static final Style STYLE = Style.EMPTY.withFont(ID);

    public static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    public static boolean custom() {
        InterfaceModule i = InterfaceModule.get();
        return i == null || i.font.is("Nova Sans");
    }

    public static Text text(String s) {
        MutableText t = Text.literal(s);
        return custom() ? t.setStyle(STYLE) : t;
    }

    public static int width(String s) {
        if (s == null || s.isEmpty()) return 0;
        return custom() ? font().getWidth(text(s)) : font().getWidth(s);
    }

    public static int height() { return font().fontHeight; }

    /** Рисует строку. Учитывает общую прозрачность (Render2DUtils.alpha) и глобальную тень. Возвращает X конца строки. */
    public static int draw(DrawContext ctx, String s, float x, float y, int color, boolean shadow) {
        if (s == null || s.isEmpty()) return Math.round(x);
        float a = Render2DUtils.alpha;
        if (a < 1f) color = ThemeManager.fade(color, a);
        if (((color >>> 24) & 255) < 8) return Math.round(x) + width(s); // почти прозрачный текст MC рисует непрозрачным — пропускаем
        InterfaceModule i = InterfaceModule.get();
        if (i != null && i.textShadow.get()) shadow = true;
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        float fx = x - ix, fy = y - iy;
        if (fx != 0 || fy != 0) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(fx, fy, 0);
        }
        int end = custom() ? ctx.drawText(font(), text(s), ix, iy, color, shadow) : ctx.drawText(font(), s, ix, iy, color, shadow);
        if (fx != 0 || fy != 0) ctx.getMatrices().pop();
        return end;
    }

    /** Текст с масштабом (заголовки). */
    public static void draw(DrawContext ctx, String s, float x, float y, int color, boolean shadow, float scale) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        draw(ctx, s, 0, 0, color, shadow);
        ctx.getMatrices().pop();
    }

    public static String trim(String s, int maxW) {
        if (width(s) <= maxW) return s;
        String ell = "…";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width(s.substring(0, mid) + ell) <= maxW) lo = mid; else hi = mid - 1;
        }
        return s.substring(0, lo) + ell;
    }
}
